package com.payforge.transfer.client;

/**
 * The downstream service refused the operation for a business reason (insufficient funds,
 * frozen account, unbalanced journal). Distinct from a transport failure, which is retryable.
 */
public class DownstreamRejectedException extends RuntimeException {

    public DownstreamRejectedException(String message) {
        super(message);
    }
}
