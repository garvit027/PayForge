package com.payforge.account.web.dto;

import com.payforge.account.domain.Customer;

import java.time.Instant;
import java.util.UUID;

public record CustomerView(UUID id, String fullName, String email, String country, Instant createdAt) {

    public static CustomerView from(Customer customer) {
        return new CustomerView(customer.getId(), customer.getFullName(), customer.getEmail(),
                customer.getCountry(), customer.getCreatedAt());
    }
}
