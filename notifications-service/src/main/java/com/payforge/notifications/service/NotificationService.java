package com.payforge.notifications.service;

import com.payforge.notifications.domain.Notification;
import com.payforge.notifications.messaging.TransferEvent;
import com.payforge.notifications.repo.NotificationRepository;
import com.payforge.notifications.web.dto.NotificationView;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository repository;
    private final Counter recorded;

    public NotificationService(NotificationRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.recorded = Counter.builder("payforge_notifications_recorded_total").register(meterRegistry);
    }

    /**
     * Projects one event into the read model. Returns {@code false} when the event was already
     * seen: Kafka delivers at least once, so duplicates are normal rather than exceptional.
     */
    @Transactional
    public boolean record(TransferEvent event) {
        String eventKey = event.transferId() + ":" + event.status();
        if (repository.existsByEventKey(eventKey)) {
            return false;
        }

        Notification.Kind kind = switch (event.status()) {
            case "COMPLETED" -> Notification.Kind.TRANSFER_COMPLETED;
            case "FAILED" -> Notification.Kind.TRANSFER_FAILED;
            default -> Notification.Kind.TRANSFER_PENDING;
        };

        String amount = event.amount() == null
                ? "an amount"
                : event.amount().setScale(2, RoundingMode.HALF_UP).toPlainString() + " " + event.currency();

        String message = switch (kind) {
            case TRANSFER_COMPLETED -> "Transfer of " + amount + " from "
                    + event.source() + " to " + event.target() + " completed.";
            case TRANSFER_FAILED -> "Transfer of " + amount + " from "
                    + event.source() + " to " + event.target() + " failed.";
            case TRANSFER_PENDING -> "Transfer of " + amount + " from "
                    + event.source() + " to " + event.target() + " is awaiting settlement.";
        };

        try {
            repository.save(new Notification(eventKey, event.transferId(), kind,
                    event.source(), event.target(), event.amount(), event.currency(), message));
            recorded.increment();
            return true;
        } catch (DataIntegrityViolationException concurrentDuplicate) {
            // Another consumer instance stored the same event first; that is a no-op for us.
            return false;
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationView> feed(String accountNumber) {
        List<Notification> rows = (accountNumber == null || accountNumber.isBlank())
                ? repository.findTop50ByOrderByCreatedAtDesc()
                : repository.findTop50BySourceAccountOrTargetAccountOrderByCreatedAtDesc(
                        accountNumber, accountNumber);
        return rows.stream().map(NotificationView::from).toList();
    }
}
