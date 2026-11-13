package com.payforge.account.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record OpenAccountRequest(
        @NotNull UUID customerId,
        @NotBlank @Size(min = 3, max = 3) String currency
) {
}
