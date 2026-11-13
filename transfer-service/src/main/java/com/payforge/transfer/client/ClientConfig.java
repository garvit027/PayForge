package com.payforge.transfer.client;

import com.payforge.commons.web.InternalTokenFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientConfig {

    @Bean
    public RestClient accountRestClient(RestClient.Builder builder,
                                        @Value("${payforge.clients.account-url}") String baseUrl,
                                        @Value("${payforge.internal.token}") String internalToken) {
        return builder.clone()
                .baseUrl(baseUrl)
                .defaultHeader(InternalTokenFilter.HEADER, internalToken)
                .build();
    }

    @Bean
    public RestClient ledgerRestClient(RestClient.Builder builder,
                                       @Value("${payforge.clients.ledger-url}") String baseUrl,
                                       @Value("${payforge.internal.token}") String internalToken) {
        return builder.clone()
                .baseUrl(baseUrl)
                .defaultHeader(InternalTokenFilter.HEADER, internalToken)
                .build();
    }
}
