package com.payforge.ledger.web.dto;

import java.math.BigDecimal;

public record PostingView(
        String accountCode,
        String direction,
        BigDecimal amount,
        String currency
) {
}
