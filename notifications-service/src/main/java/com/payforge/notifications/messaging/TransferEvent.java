package com.payforge.notifications.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** Mirrors the payload published by transfer-service. Unknown fields are ignored on purpose. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TransferEvent(
        String transferId,
        String status,
        String source,
        String target,
        BigDecimal amount,
        String currency
) {
}
