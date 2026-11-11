package com.mphasis.assessment.txservice.store;

/**
 * US-04 / NFR-01: dual-key idempotency.
 * requestId catches a literal retry of the exact same physical attempt.
 * transactionId catches a *different* requestId for the same business
 * transaction (e.g. two different requestIds both referencing the same
 * transactionId, as in section 9.8 rows 1 and 3) - the
 * case the original requestId-only design missed, since a genuinely new
 * requestId with a fresh sequence number would have slipped through.
 */
public interface IdempotencyStore {
    boolean isProcessed(String requestId);
    void markProcessed(String requestId);

    boolean isTransactionProcessed(String transactionId);
    void markTransactionProcessed(String transactionId);
}
