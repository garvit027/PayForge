package com.payforge.ledger.web.dto;

import java.math.BigDecimal;

public record BalanceView(
        String accountCode,
        String accountType,
        String currency,
        BigDecimal balance
) {
}
