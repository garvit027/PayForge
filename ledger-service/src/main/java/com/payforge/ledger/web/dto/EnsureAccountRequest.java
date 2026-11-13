package com.payforge.ledger.web.dto;

import com.payforge.ledger.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EnsureAccountRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 128) String ownerRef,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull AccountType accountType,
        boolean allowNegative
) {
}
