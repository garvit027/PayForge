package com.payforge.notifications.web.dto;

import com.payforge.notifications.domain.Notification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record NotificationView(
        UUID id,
        String transferId,
        String kind,
        String sourceAccount,
        String targetAccount,
        BigDecimal amount,
        String currency,
        String message,
        Instant createdAt
) {
    public static NotificationView from(Notification notification) {
        return new NotificationView(
                notification.getId(),
                notification.getTransferId(),
                notification.getKind().name(),
                notification.getSourceAccount(),
                notification.getTargetAccount(),
                notification.getAmount(),
                notification.getCurrency(),
                notification.getMessage(),
                notification.getCreatedAt());
    }
}
