package com.payforge.ledger.web.dto;

import com.payforge.ledger.domain.Direction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PostingLine(
        @NotBlank String accountCode,
        @NotNull Direction direction,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal amount,
        @NotBlank String currency
) {
}
