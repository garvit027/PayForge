package com.payforge.notifications.service;

import com.payforge.notifications.domain.Notification;
import com.payforge.notifications.messaging.TransferEvent;
import com.payforge.notifications.repo.NotificationRepository;
import com.payforge.notifications.web.dto.NotificationView;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository, new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("projects a completed transfer into a readable notification")
    void projectsCompletedTransfer() {
        when(repository.existsByEventKey("transfer-1:COMPLETED")).thenReturn(false);

        boolean stored = service.record(new TransferEvent("transfer-1", "COMPLETED",
                "PF-0001", "PF-0002", new BigDecimal("125.5000"), "INR"));

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(saved.capture());

        assertThat(stored).isTrue();
        Notification notification = saved.getValue();
        assertThat(notification.getTransferId()).isEqualTo("transfer-1");
        assertThat(notification.getKind()).isEqualTo(Notification.Kind.TRANSFER_COMPLETED);
        assertThat(notification.getMessage()).isEqualTo(
                "Transfer of 125.50 INR from PF-0001 to PF-0002 completed.");
    }

    @Test
    @DisplayName("ignores an event that was already projected")
    void ignoresDuplicateEvent() {
        when(repository.existsByEventKey("transfer-1:COMPLETED")).thenReturn(true);

        boolean stored = service.record(new TransferEvent("transfer-1", "COMPLETED",
                "PF-0001", "PF-0002", new BigDecimal("10.00"), "INR"));

        assertThat(stored).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("treats an unexpected status as still pending rather than dropping it")
    void mapsUnknownStatusToPending() {
        when(repository.existsByEventKey("transfer-1:REVERSED")).thenReturn(false);

        boolean stored = service.record(new TransferEvent("transfer-1", "REVERSED",
                "PF-0001", "PF-0002", new BigDecimal("10.00"), "INR"));

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(saved.capture());
        assertThat(stored).isTrue();
        assertThat(saved.getValue().getKind()).isEqualTo(Notification.Kind.TRANSFER_PENDING);
    }

    @Test
    @DisplayName("returns the feed for one account without a platform-wide query")
    void filtersFeedByAccount() {
        Notification row = new Notification("transfer-1:COMPLETED", "transfer-1",
                Notification.Kind.TRANSFER_COMPLETED, "PF-0001", "PF-0002",
                new BigDecimal("10.0000"), "INR", "done");
        when(repository.findTop50BySourceAccountOrTargetAccountOrderByCreatedAtDesc("PF-0001", "PF-0001"))
                .thenReturn(List.of(row));

        List<NotificationView> feed = service.feed("PF-0001");

        assertThat(feed).singleElement().satisfies(view -> {
            assertThat(view.transferId()).isEqualTo("transfer-1");
            assertThat(view.kind()).isEqualTo("TRANSFER_COMPLETED");
        });
    }
}
