package com.payforge.transfer.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

@Component
public class LedgerClient {

    private final RestClient client;

    public LedgerClient(@Qualifier("ledgerRestClient") RestClient client) {
        this.client = client;
    }

    /** Idempotent on {@code code}; safe to call before every transfer. */
    public void ensureCustomerAccount(String code, String ownerRef, String currency) {
        client.post()
                .uri("/internal/v1/ledger-accounts")
                .body(new EnsureAccountBody(code, ownerRef, currency, "LIABILITY", false))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new DownstreamRejectedException("Ledger rejected account " + code);
                })
                .toBodilessEntity();
    }

    /**
     * Posts a balanced two-line journal entry. Idempotent on {@code reference}: replaying the
     * same transfer returns the original entry instead of double-posting.
     */
    public void postTransfer(String reference, String description, String debitCode,
                             String creditCode, BigDecimal amount, String currency) {
        PostJournalBody body = new PostJournalBody(reference, description, List.of(
                new Line(debitCode, "DEBIT", amount, currency),
                new Line(creditCode, "CREDIT", amount, currency)));

        client.post()
                .uri("/internal/v1/journals")
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    String detail = new String(response.getBody().readAllBytes());
                    throw new DownstreamRejectedException(extractMessage(detail));
                })
                .toBodilessEntity();
    }

    private String extractMessage(String body) {
        int start = body.indexOf("\"message\":\"");
        if (start < 0) {
            return "The ledger rejected the transfer";
        }
        int from = start + "\"message\":\"".length();
        int end = body.indexOf('"', from);
        return end > from ? body.substring(from, end) : "The ledger rejected the transfer";
    }

    private record EnsureAccountBody(String code, String ownerRef, String currency,
                                     String accountType, boolean allowNegative) {
    }

    private record Line(String accountCode, String direction, BigDecimal amount, String currency) {
    }

    private record PostJournalBody(String reference, String description, List<Line> postings) {
    }
}
