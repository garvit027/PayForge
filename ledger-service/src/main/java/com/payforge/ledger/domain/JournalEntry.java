package com.payforge.ledger.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * An immutable set of postings that balances to zero. Once written, a journal entry is
 * never edited; corrections are new entries that reverse the original.
 */
@Entity
@Table(name = "journal_entry")
public class JournalEntry {

    public enum Status {
        POSTED, REVERSED
    }

    @Id
    private UUID id;

    /** Client-supplied reference. Unique, which is what makes posting idempotent. */
    @Column(name = "reference", nullable = false, unique = true, length = 128)
    private String reference;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Posting> postings = new ArrayList<>();

    protected JournalEntry() {
    }

    public JournalEntry(String reference, String description) {
        this.id = UUID.randomUUID();
        this.reference = reference;
        this.description = description;
        this.status = Status.POSTED;
        this.createdAt = Instant.now();
    }

    public void addPosting(Posting posting) {
        posting.attachTo(this);
        this.postings.add(posting);
    }

    public UUID getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public String getDescription() {
        return description;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<Posting> getPostings() {
        return postings;
    }
}
