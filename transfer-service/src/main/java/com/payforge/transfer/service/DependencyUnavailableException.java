package com.payforge.transfer.service;

/**
 * A downstream dependency could not be reached. The transfer stays PENDING and the caller
 * should retry with the same idempotency key once the dependency recovers.
 */
public class DependencyUnavailableException extends RuntimeException {

    public DependencyUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
