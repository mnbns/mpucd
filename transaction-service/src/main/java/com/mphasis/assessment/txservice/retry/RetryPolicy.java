package com.mphasis.assessment.txservice.retry;

import com.mphasis.assessment.txservice.model.TransactionRequest;

/** US-06 / DD-04: bounded retry - 3 attempts, externalized here rather than hardcoded in the processor. */
public class RetryPolicy {

    private final int maxRetries;

    public RetryPolicy(int maxRetries) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be >= 0, got: " + maxRetries);
        }
        this.maxRetries = maxRetries;
    }

    public boolean shouldRetry(TransactionRequest request) {
        return request.getRetryCount() < maxRetries;
    }
}
