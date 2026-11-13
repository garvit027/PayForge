package com.payforge.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    /**
     * Buckets are per authenticated subject, so one noisy caller cannot exhaust another
     * caller's budget. Anything that cannot be attributed to a subject falls back to a
     * shared bucket rather than failing the request.
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(principal -> principal.getName() == null || principal.getName().isBlank()
                        ? "anonymous"
                        : principal.getName())
                .defaultIfEmpty("anonymous")
                .onErrorResume(failure -> Mono.just("anonymous"));
    }
}
