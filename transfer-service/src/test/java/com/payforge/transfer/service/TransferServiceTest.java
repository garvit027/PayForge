package com.payforge.transfer.service;

import com.payforge.commons.outbox.OutboxRecorder;
import com.payforge.transfer.client.AccountDirectoryClient;
import com.payforge.transfer.client.LedgerClient;
import com.payforge.transfer.domain.Transfer;
import com.payforge.transfer.repo.TransferRepository;
import com.payforge.transfer.web.dto.CreateTransferRequest;
import com.payforge.transfer.web.dto.TransferView;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private TransferRepository transfers;
    @Mock
    private AccountDirectoryClient accountDirectory;
    @Mock
    private LedgerClient ledger;
    @Mock
    private OutboxRecorder outbox;

    private TransferService service;

    @BeforeEach
    void setUp() {
        service = new TransferService(transfers, accountDirectory, ledger, outbox, new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("replaying a request with the same key returns the original transfer")
    void replaysExistingTransfer() {
        Transfer existing = new Transfer("key-1", "PF1", "PF2", new BigDecimal("10.0000"), "INR", "rent");
        when(transfers.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        TransferView view = service.create("key-1",
                new CreateTransferRequest("PF1", "PF2", new BigDecimal("10.00"), "INR", "rent"));

        assertThat(view.id()).isEqualTo(existing.getId());
        verifyNoInteractions(accountDirectory, ledger);
    }

    @Test
    @DisplayName("reusing a key for a different transfer is rejected")
    void rejectsKeyReuseWithDifferentPayload() {
        Transfer existing = new Transfer("key-1", "PF1", "PF2", new BigDecimal("10.0000"), "INR", "rent");
        when(transfers.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create("key-1",
                new CreateTransferRequest("PF1", "PF2", new BigDecimal("99.00"), "INR", "rent")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already used");
    }

    @Test
    @DisplayName("a transfer to the same account is rejected before anything is persisted")
    void rejectsSelfTransfer() {
        assertThatThrownBy(() -> service.create("key-2",
                new CreateTransferRequest("PF1", "PF1", new BigDecimal("5.00"), "INR", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must differ");

        verifyNoInteractions(accountDirectory, ledger);
    }
}
