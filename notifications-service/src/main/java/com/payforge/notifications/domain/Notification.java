package com.payforge.notifications.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A projected view of something that happened to a transfer. This is a read model, not a
 * source of truth: it can be rebuilt from the event log because every row is keyed by the
 * event that produced it.
 */
@Entity
@Table(name = "notification")
public class Notification {

    public enum Kind {
        TRANSFER_COMPLETED, TRANSFER_FAILED, TRANSFER_PENDING
    }

    @Id
    private UUID id;

    /** {@code transferId:status}. The unique constraint makes redelivery harmless. */
    @Column(name = "event_key", nullable = false, unique = true, length = 160)
    private String eventKey;

    @Column(name = "transfer_id", nullable = false, length = 64)
    private String transferId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 32)
    private Kind kind;

    @Column(name = "source_account", length = 34)
    private String sourceAccount;

    @Column(name = "target_account", length = 34)
    private String targetAccount;

    @Column(name = "amount", precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(String eventKey, String transferId, Kind kind, String sourceAccount,
                        String targetAccount, BigDecimal amount, String currency, String message) {
        this.id = UUID.randomUUID();
        this.eventKey = eventKey;
        this.transferId = transferId;
        this.kind = kind;
        this.sourceAccount = sourceAccount;
        this.targetAccount = targetAccount;
        this.amount = amount;
        this.currency = currency;
        this.message = message;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getEventKey() {
        return eventKey;
    }

    public String getTransferId() {
        return transferId;
    }

    public Kind getKind() {
        return kind;
    }

    public String getSourceAccount() {
        return sourceAccount;
    }

    public String getTargetAccount() {
        return targetAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
