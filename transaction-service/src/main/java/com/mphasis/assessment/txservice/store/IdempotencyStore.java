package com.mphasis.assessment.txservice.store;

/**
 * US-04 / NFR-01: dual-key idempotency.
 * requestId catches a literal retry of the exact same physical attempt.
 * transactionId catches a *different* requestId for the same business
 * transaction (e.g. REQ-2 vs REQ-2-DUP both referencing TXN-2002) - the
 * case the original requestId-only design missed, since a genuinely new
 * requestId with a fresh sequence number would have slipped through.
 */
public interface IdempotencyStore {
    boolean isProcessed(String requestId);
    void markProcessed(String requestId);

    boolean isTransactionProcessed(String transactionId);
    void markTransactionProcessed(String transactionId);
}
