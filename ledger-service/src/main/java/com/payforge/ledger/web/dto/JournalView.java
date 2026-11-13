package com.payforge.ledger.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JournalView(
        UUID id,
        String reference,
        String description,
        String status,
        Instant createdAt,
        List<PostingView> postings
) {
}
