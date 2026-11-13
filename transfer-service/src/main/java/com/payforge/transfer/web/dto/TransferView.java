package com.payforge.transfer.web.dto;

import com.payforge.transfer.domain.Transfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferView(
        UUID id,
        String status,
        String sourceAccountNumber,
        String targetAccountNumber,
        BigDecimal amount,
        String currency,
        String note,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static TransferView from(Transfer transfer) {
        return new TransferView(
                transfer.getId(),
                transfer.getStatus().name(),
                transfer.getSourceAccountNumber(),
                transfer.getTargetAccountNumber(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getNote(),
                transfer.getFailureReason(),
                transfer.getCreatedAt(),
                transfer.getUpdatedAt());
    }
}
