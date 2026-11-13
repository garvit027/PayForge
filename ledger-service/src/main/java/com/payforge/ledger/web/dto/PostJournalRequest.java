package com.payforge.ledger.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostJournalRequest(
        @NotBlank @Size(max = 128) String reference,
        @Size(max = 255) String description,
        @NotEmpty @Valid List<PostingLine> postings
) {
}
