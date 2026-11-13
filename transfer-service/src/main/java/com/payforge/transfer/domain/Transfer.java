package com.payforge.transfer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer")
public class Transfer {

    public enum Status {
        PENDING, COMPLETED, FAILED
    }

    @Id
    private UUID id;

    /** Supplied by the caller; the unique constraint is the real idempotency guard. */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "source_account_number", nullable = false, length = 34)
    private String sourceAccountNumber;

    @Column(name = "target_account_number", nullable = false, length = 34)
    private String targetAccountNumber;

    @Column(name = "source_ledger_code", length = 64)
    private String sourceLedgerCode;

    @Column(name = "target_ledger_code", length = 64)
    private String targetLedgerCode;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private Status status;

    @Column(name = "note", length = 255)
    private String note;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Transfer() {
    }

    public Transfer(String idempotencyKey, String sourceAccountNumber, String targetAccountNumber,
                    BigDecimal amount, String currency, String note) {
        this.id = UUID.randomUUID();
        this.idempotencyKey = idempotencyKey;
        this.sourceAccountNumber = sourceAccountNumber;
        this.targetAccountNumber = targetAccountNumber;
        this.amount = amount;
        this.currency = currency;
        this.note = note;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void resolveLedgerCodes(String source, String target) {
        this.sourceLedgerCode = source;
        this.targetLedgerCode = target;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        this.status = Status.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public void fail(String reason) {
        this.status = Status.FAILED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public boolean matchesRequest(String source, String target, BigDecimal amount, String currency) {
        return sourceAccountNumber.equals(source)
                && targetAccountNumber.equals(target)
                && this.amount.compareTo(amount) == 0
                && this.currency.equalsIgnoreCase(currency);
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public String getTargetAccountNumber() {
        return targetAccountNumber;
    }

    public String getSourceLedgerCode() {
        return sourceLedgerCode;
    }

    public String getTargetLedgerCode() {
        return targetLedgerCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Status getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
