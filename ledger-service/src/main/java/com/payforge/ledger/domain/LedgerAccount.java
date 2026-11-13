package com.payforge.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_account")
public class LedgerAccount {

    @Id
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "owner_ref", nullable = false, length = 128)
    private String ownerRef;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 16)
    private AccountType accountType;

    @Column(name = "allow_negative", nullable = false)
    private boolean allowNegative;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LedgerAccount() {
    }

    public LedgerAccount(String code, String ownerRef, String currency, AccountType accountType, boolean allowNegative) {
        this.id = UUID.randomUUID();
        this.code = code;
        this.ownerRef = ownerRef;
        this.currency = currency;
        this.accountType = accountType;
        this.allowNegative = allowNegative;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getOwnerRef() {
        return ownerRef;
    }

    public String getCurrency() {
        return currency;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public boolean isAllowNegative() {
        return allowNegative;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
