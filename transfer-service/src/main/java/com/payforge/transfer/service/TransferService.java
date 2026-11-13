package com.payforge.transfer.service;

import com.payforge.commons.outbox.OutboxRecorder;
import com.payforge.transfer.client.AccountDirectoryClient;
import com.payforge.transfer.client.DownstreamRejectedException;
import com.payforge.transfer.client.LedgerClient;
import com.payforge.transfer.domain.Transfer;
import com.payforge.transfer.repo.TransferRepository;
import com.payforge.transfer.web.dto.CreateTransferRequest;
import com.payforge.transfer.web.dto.TransferView;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Orchestrates a transfer as a short saga:
 *
 * <ol>
 *   <li>persist the intent (PENDING) under a unique idempotency key</li>
 *   <li>resolve both accounts</li>
 *   <li>post one balanced journal entry in the ledger</li>
 *   <li>record the outcome and emit a domain event through the outbox</li>
 * </ol>
 *
 * Deliberately no database transaction spans the remote calls. Holding row locks while a
 * network call is in flight turns a slow dependency into an outage, so the transfer row is
 * committed first and its state machine carries the rest.
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final TransferRepository transfers;
    private final AccountDirectoryClient accountDirectory;
    private final LedgerClient ledger;
    private final OutboxRecorder outbox;
    private final Counter completedCounter;
    private final Counter failedCounter;

    public TransferService(TransferRepository transfers,
                           AccountDirectoryClient accountDirectory,
                           LedgerClient ledger,
                           OutboxRecorder outbox,
                           MeterRegistry meterRegistry) {
        this.transfers = transfers;
        this.accountDirectory = accountDirectory;
        this.ledger = ledger;
        this.outbox = outbox;
        this.completedCounter = Counter.builder("payforge_transfers_total")
                .tag("outcome", "completed").register(meterRegistry);
        this.failedCounter = Counter.builder("payforge_transfers_total")
                .tag("outcome", "failed").register(meterRegistry);
    }

    public TransferView create(String idempotencyKey, CreateTransferRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("An Idempotency-Key header is required");
        }
        if (request.sourceAccountNumber().equals(request.targetAccountNumber())) {
            throw new IllegalArgumentException("Source and target accounts must differ");
        }

        BigDecimal amount = request.amount().setScale(4, RoundingMode.UNNECESSARY);
        String currency = request.currency().toUpperCase();

        Transfer replay = transfers.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (replay != null) {
            return replay(idempotencyKey, request, replay);
        }

        Transfer transfer = new Transfer(idempotencyKey, request.sourceAccountNumber(),
                request.targetAccountNumber(), amount, currency, request.note());

        try {
            transfer = transfers.save(transfer);
        } catch (DataIntegrityViolationException concurrentDuplicate) {
            // Two identical requests landed together; the loser returns the winner's result.
            Transfer winner = transfers.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> concurrentDuplicate);
            return replay(idempotencyKey, request, winner);
        }

        return execute(transfer);
    }

    public TransferView get(UUID id) {
        return transfers.findById(id)
                .map(TransferView::from)
                .orElseThrow(() -> new NoSuchElementException("Transfer " + id + " not found"));
    }

    private TransferView replay(String idempotencyKey, CreateTransferRequest request, Transfer existing) {
        boolean sameRequest = existing.matchesRequest(
                request.sourceAccountNumber(), request.targetAccountNumber(), request.amount(), request.currency());
        if (!sameRequest) {
            throw new IllegalArgumentException(
                    "Idempotency-Key " + idempotencyKey + " was already used for a different transfer");
        }
        return TransferView.from(existing);
    }

    private TransferView execute(Transfer transfer) {
        try {
            AccountDirectoryClient.AccountRef source = accountDirectory.lookup(transfer.getSourceAccountNumber());
            AccountDirectoryClient.AccountRef target = accountDirectory.lookup(transfer.getTargetAccountNumber());

            requireActive(source);
            requireActive(target);

            if (!source.currency().equalsIgnoreCase(transfer.getCurrency())
                    || !target.currency().equalsIgnoreCase(transfer.getCurrency())) {
                throw new DownstreamRejectedException("Currency mismatch between the transfer and its accounts");
            }

            ledger.ensureCustomerAccount(source.ledgerCode(), source.accountNumber(), transfer.getCurrency());
            ledger.ensureCustomerAccount(target.ledgerCode(), target.accountNumber(), transfer.getCurrency());
            transfer.resolveLedgerCodes(source.ledgerCode(), target.ledgerCode());

            ledger.postTransfer("transfer:" + transfer.getId(), transfer.getNote(),
                    source.ledgerCode(), target.ledgerCode(), transfer.getAmount(), transfer.getCurrency());

            transfer.complete();
            transfers.save(transfer);
            completedCounter.increment();
            publish(transfer, "transfer.completed");
            return TransferView.from(transfer);

        } catch (DownstreamRejectedException rejected) {
            transfer.fail(rejected.getMessage());
            transfers.save(transfer);
            failedCounter.increment();
            publish(transfer, "transfer.failed");
            log.info("Transfer {} rejected: {}", transfer.getId(), rejected.getMessage());
            return TransferView.from(transfer);

        } catch (Exception transportFailure) {
            // Leave the transfer PENDING: the intent is durable and a retry with the same
            // idempotency key will not double-charge, because the ledger is keyed on transfer id.
            publish(transfer, "transfer.pending");
            log.warn("Transfer {} could not reach the ledger; left PENDING", transfer.getId(), transportFailure);
            throw new DependencyUnavailableException(
                    "The ledger is unavailable. Retry with the same Idempotency-Key.", transportFailure);
        }
    }

    private void requireActive(AccountDirectoryClient.AccountRef account) {
        if (!"ACTIVE".equals(account.status())) {
            throw new DownstreamRejectedException("Account " + account.accountNumber() + " is " + account.status());
        }
    }

    private void publish(Transfer transfer, String eventType) {
        outbox.record("transfer", transfer.getId().toString(), eventType, new TransferEvent(
                transfer.getId().toString(),
                transfer.getStatus().name(),
                transfer.getSourceAccountNumber(),
                transfer.getTargetAccountNumber(),
                transfer.getAmount(),
                transfer.getCurrency()));
    }

    public record TransferEvent(String transferId, String status, String source, String target,
                                BigDecimal amount, String currency) {
    }
}
