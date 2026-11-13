package com.payforge.ledger.service;

import com.payforge.commons.outbox.OutboxRecorder;
import com.payforge.ledger.domain.AccountType;
import com.payforge.ledger.domain.Direction;
import com.payforge.ledger.domain.JournalEntry;
import com.payforge.ledger.domain.LedgerAccount;
import com.payforge.ledger.domain.Posting;
import com.payforge.ledger.repo.JournalEntryRepository;
import com.payforge.ledger.repo.LedgerAccountRepository;
import com.payforge.ledger.repo.PostingRepository;
import com.payforge.ledger.web.dto.BalanceView;
import com.payforge.ledger.web.dto.EnsureAccountRequest;
import com.payforge.ledger.web.dto.JournalView;
import com.payforge.ledger.web.dto.PostJournalRequest;
import com.payforge.ledger.web.dto.PostingLine;
import com.payforge.ledger.web.dto.PostingView;
import com.payforge.ledger.web.dto.SettlementRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class LedgerService {

    public static final String SETTLEMENT_ACCOUNT = "PLATFORM-SETTLEMENT";

    private final LedgerAccountRepository ledgerAccounts;
    private final JournalEntryRepository journals;
    private final PostingRepository postings;
    private final OutboxRecorder outbox;

    public LedgerService(LedgerAccountRepository ledgerAccounts,
                         JournalEntryRepository journals,
                         PostingRepository postings,
                         OutboxRecorder outbox) {
        this.ledgerAccounts = ledgerAccounts;
        this.journals = journals;
        this.postings = postings;
        this.outbox = outbox;
    }

    /** Idempotent on {@code code}; calling twice is a no-op the second time. */
    @Transactional
    public BalanceView ensureAccount(EnsureAccountRequest request) {
        LedgerAccount account = ledgerAccounts.findByCode(request.code())
                .orElseGet(() -> ledgerAccounts.save(new LedgerAccount(
                        request.code(), request.ownerRef(), request.currency().toUpperCase(),
                        request.accountType(), request.allowNegative())));

        if (account.getAccountType() != request.accountType()) {
            throw new IllegalArgumentException("Account " + request.code() + " already exists with type "
                    + account.getAccountType() + ", cannot change it to " + request.accountType());
        }
        return toBalanceView(account);
    }

    @Transactional
    public JournalView post(PostJournalRequest request) {
        return journals.findByReference(request.reference())
                .map(this::toJournalView)
                .orElseGet(() -> doPost(request));
    }

    private JournalView doPost(PostJournalRequest request) {
        if (request.postings().size() < 2) {
            throw new IllegalArgumentException("A journal entry needs at least two postings");
        }

        Map<String, BigDecimal> netByCurrency = new HashMap<>();
        for (PostingLine line : request.postings()) {
            BigDecimal signed = line.direction() == Direction.DEBIT ? line.amount() : line.amount().negate();
            netByCurrency.merge(line.currency().toUpperCase(), signed, BigDecimal::add);
        }
        netByCurrency.forEach((currency, net) -> {
            if (net.signum() != 0) {
                throw new IllegalArgumentException(
                        "Journal is not balanced for " + currency + " (net " + net + ")");
            }
        });

        JournalEntry journal = new JournalEntry(request.reference(), request.description());
        Map<String, BigDecimal> runningBalances = new HashMap<>();

        for (PostingLine line : request.postings()) {
            LedgerAccount account = ledgerAccounts.findByCode(line.accountCode())
                    .orElseThrow(() -> new NoSuchElementException("Ledger account " + line.accountCode() + " not found"));

            if (!account.getCurrency().equalsIgnoreCase(line.currency())) {
                throw new IllegalArgumentException("Currency mismatch on account " + account.getCode());
            }

            BigDecimal delta = signedDelta(account, line.direction(), line.amount());
            BigDecimal current = runningBalances.computeIfAbsent(
                    account.getCode(), code -> currentBalance(account));
            BigDecimal projected = current.add(delta);

            if (projected.signum() < 0 && !account.isAllowNegative()) {
                throw new IllegalArgumentException(
                        "Insufficient funds on account " + account.getCode() + ": balance " + current
                                + " but posting would take it to " + projected);
            }
            runningBalances.put(account.getCode(), projected);

            journal.addPosting(new Posting(account, line.direction(), line.amount(), line.currency().toUpperCase()));
        }

        JournalEntry saved;
        try {
            saved = journals.save(journal);
        } catch (DataIntegrityViolationException concurrentDuplicate) {
            // Another request posted the same reference at the same moment; return their result.
            return journals.findByReference(request.reference())
                    .map(this::toJournalView)
                    .orElseThrow(() -> concurrentDuplicate);
        }

        outbox.record("journal", saved.getId().toString(), "journal.posted",
                new JournalPostedEvent(saved.getReference(), saved.getId().toString()));
        return toJournalView(saved);
    }

    @Transactional
    public JournalView settle(SettlementRequest request) {
        // Funding a customer account is the only place a ledger account is created implicitly:
        // the bank rail knows the account code but not necessarily the owning customer record.
        LedgerAccount target = ledgerAccounts.findByCode(request.accountCode())
                .orElseGet(() -> ledgerAccounts.save(new LedgerAccount(
                        request.accountCode(), request.accountCode(), request.currency().toUpperCase(),
                        AccountType.LIABILITY, false)));

        ledgerAccounts.findByCode(SETTLEMENT_ACCOUNT).orElseGet(() -> ledgerAccounts.save(
                new LedgerAccount(SETTLEMENT_ACCOUNT, "platform", target.getCurrency(),
                        AccountType.ASSET, true)));

        return post(new PostJournalRequest(
                request.reference(),
                "Settlement funding for " + request.accountCode(),
                List.of(
                        new PostingLine(SETTLEMENT_ACCOUNT, Direction.DEBIT, request.amount(), request.currency()),
                        new PostingLine(request.accountCode(), Direction.CREDIT, request.amount(), request.currency()))));
    }

    @Transactional(readOnly = true)
    public BalanceView balance(String accountCode) {
        LedgerAccount account = ledgerAccounts.findByCode(accountCode)
                .orElseThrow(() -> new NoSuchElementException("Ledger account " + accountCode + " not found"));
        return toBalanceView(account);
    }

    @Transactional(readOnly = true)
    public List<JournalView> statement(String accountCode) {
        LedgerAccount account = ledgerAccounts.findByCode(accountCode)
                .orElseThrow(() -> new NoSuchElementException("Ledger account " + accountCode + " not found"));
        return journals.findAll().stream()
                .filter(journal -> journal.getPostings().stream()
                        .anyMatch(posting -> posting.getLedgerAccount().getCode().equals(account.getCode())))
                .map(this::toJournalView)
                .toList();
    }

    private BigDecimal currentBalance(LedgerAccount account) {
        BigDecimal credits = postings.totalCredits(account.getId());
        BigDecimal debits = postings.totalDebits(account.getId());
        boolean normalCredit = account.getAccountType() != AccountType.ASSET;
        return normalCredit ? credits.subtract(debits) : debits.subtract(credits);
    }

    /** How this posting moves the account's natural balance: up or down. */
    private BigDecimal signedDelta(LedgerAccount account, Direction direction, BigDecimal amount) {
        boolean normalCredit = account.getAccountType() != AccountType.ASSET;
        boolean increases = normalCredit == (direction == Direction.CREDIT);
        return increases ? amount : amount.negate();
    }

    private BalanceView toBalanceView(LedgerAccount account) {
        return new BalanceView(account.getCode(), account.getAccountType().name(),
                account.getCurrency(), currentBalance(account));
    }

    private JournalView toJournalView(JournalEntry journal) {
        List<PostingView> lines = journal.getPostings().stream()
                .map(posting -> new PostingView(
                        posting.getLedgerAccount().getCode(),
                        posting.getDirection().name(),
                        posting.getAmount(),
                        posting.getCurrency()))
                .toList();
        return new JournalView(journal.getId(), journal.getReference(), journal.getDescription(),
                journal.getStatus().name(), journal.getCreatedAt(), lines);
    }

    public record JournalPostedEvent(String reference, String journalId) {
    }
}
