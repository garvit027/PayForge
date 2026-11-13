package com.payforge.account.web.dto;

import com.payforge.account.domain.Account;

import java.time.Instant;
import java.util.UUID;

public record AccountView(
        UUID id,
        String accountNumber,
        String ledgerCode,
        UUID customerId,
        String currency,
        String status,
        Instant createdAt) {

    public static AccountView from(Account account) {
        return new AccountView(account.getId(), account.getAccountNumber(), account.getLedgerCode(),
                account.getCustomerId(), account.getCurrency(), account.getStatus().name(), account.getCreatedAt());
    }
}
