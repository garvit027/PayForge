package com.payforge.ledger.service;

import com.payforge.commons.outbox.OutboxRecorder;
import com.payforge.ledger.domain.AccountType;
import com.payforge.ledger.domain.Direction;
import com.payforge.ledger.domain.JournalEntry;
import com.payforge.ledger.domain.LedgerAccount;
import com.payforge.ledger.repo.JournalEntryRepository;
import com.payforge.ledger.repo.LedgerAccountRepository;
import com.payforge.ledger.repo.PostingRepository;
import com.payforge.ledger.web.dto.JournalView;
import com.payforge.ledger.web.dto.PostJournalRequest;
import com.payforge.ledger.web.dto.PostingLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerAccountRepository ledgerAccounts;
    @Mock
    private JournalEntryRepository journals;
    @Mock
    private PostingRepository postings;
    @Mock
    private OutboxRecorder outbox;

    private LedgerService service;

    @BeforeEach
    void setUp() {
        service = new LedgerService(ledgerAccounts, journals, postings, outbox);
    }

    @Test
    @DisplayName("rejects a journal whose debits and credits do not net to zero")
    void rejectsUnbalancedJournal() {
        PostJournalRequest request = new PostJournalRequest("ref-unbalanced", "bad", List.of(
                new PostingLine("CUST-A", Direction.DEBIT, new BigDecimal("10.00"), "INR"),
                new PostingLine("CUST-B", Direction.CREDIT, new BigDecimal("9.00"), "INR")));

        when(journals.findByReference("ref-unbalanced")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.post(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not balanced");

        verify(journals, never()).save(any());
        verify(outbox, never()).record(any(), any(), any(), any());
    }

    @Test
    @DisplayName("rejects a debit that would take a non-negative account below zero")
    void rejectsInsufficientFunds() {
        LedgerAccount source = new LedgerAccount("CUST-A", "PF1", "INR", AccountType.LIABILITY, false);

        when(ledgerAccounts.findByCode("CUST-A")).thenReturn(Optional.of(source));
        when(postings.totalCredits(source.getId())).thenReturn(new BigDecimal("10.0000"));
        when(postings.totalDebits(source.getId())).thenReturn(new BigDecimal("0.0000"));

        PostJournalRequest request = new PostJournalRequest("ref-1", "overdraft", List.of(
                new PostingLine("CUST-A", Direction.DEBIT, new BigDecimal("50.00"), "INR"),
                new PostingLine("CUST-B", Direction.CREDIT, new BigDecimal("50.00"), "INR")));

        assertThatThrownBy(() -> service.post(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    @DisplayName("returns the original entry when the same reference is posted again")
    void postingIsIdempotentOnReference() {
        LedgerAccount account = new LedgerAccount("CUST-A", "PF1", "INR", AccountType.LIABILITY, false);
        JournalEntry existing = new JournalEntry("ref-dup", "already posted");
        existing.addPosting(new com.payforge.ledger.domain.Posting(
                account, Direction.CREDIT, new BigDecimal("25.00"), "INR"));

        when(journals.findByReference("ref-dup")).thenReturn(Optional.of(existing));

        PostJournalRequest replay = new PostJournalRequest("ref-dup", "retry", List.of(
                new PostingLine("CUST-A", Direction.DEBIT, new BigDecimal("25.00"), "INR"),
                new PostingLine("CUST-B", Direction.CREDIT, new BigDecimal("25.00"), "INR")));

        JournalView view = service.post(replay);

        assertThat(view.reference()).isEqualTo("ref-dup");
        assertThat(view.postings()).hasSize(1);
        verify(ledgerAccounts, never()).findByCode(any());
        verify(journals, never()).save(any());
    }
}
