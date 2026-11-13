package com.payforge.transfer.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AccountDirectoryClient {

    private final RestClient client;

    public AccountDirectoryClient(@Qualifier("accountRestClient") RestClient client) {
        this.client = client;
    }

    public AccountRef lookup(String accountNumber) {
        return client.get()
                .uri("/internal/v1/accounts/{accountNumber}", accountNumber)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new DownstreamRejectedException("Account " + accountNumber + " could not be resolved");
                })
                .body(AccountRef.class);
    }

    public record AccountRef(String accountNumber, String ledgerCode, String currency, String status) {
    }
}
