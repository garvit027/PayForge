package com.payforge.ledger.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Moves funds into a customer ledger account from the platform settlement account.
 * In production this is the boundary where a bank rail or card acquirer would post.
 */
public record SettlementRequest(
        @NotBlank String accountCode,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal amount,
        @NotBlank String currency,
        @NotBlank String reference
) {
}
