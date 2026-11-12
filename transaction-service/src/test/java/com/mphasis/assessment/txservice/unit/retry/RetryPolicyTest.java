package com.mphasis.assessment.txservice.unit.retry;

import com.mphasis.assessment.txservice.model.TransactionRequest;
import com.mphasis.assessment.txservice.model.TransactionType;
import com.mphasis.assessment.txservice.retry.RetryPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for {@link RetryPolicy}: construction guard and the retryCount < maxRetries boundary. */
class RetryPolicyTest {

    private TransactionRequest requestWithRetryCount(int retryCount) {
        TransactionRequest request = new TransactionRequest("TXN-1", "REQ-1", "ACC-1",
                TransactionType.CREDIT, new BigDecimal("1"), "USD", 1);
        for (int i = 0; i < retryCount; i++) {
            request.incrementRetryCount();
        }
        return request;
    }

    @Test
    void negativeMaxRetriesIsRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new RetryPolicy(-1));
    }

    @Test
    void zeroMaxRetriesIsAllowedButNeverRetries() {
        RetryPolicy policy = new RetryPolicy(0);
        assertFalse(policy.shouldRetry(requestWithRetryCount(0)));
    }

    @Test
    void shouldRetryTrueWhileRetryCountIsBelowMax() {
        RetryPolicy policy = new RetryPolicy(3);
        assertTrue(policy.shouldRetry(requestWithRetryCount(0)));
        assertTrue(policy.shouldRetry(requestWithRetryCount(2)));
    }

    @Test
    void shouldRetryFalseOnceRetryCountReachesMax() {
        RetryPolicy policy = new RetryPolicy(3);
        assertFalse(policy.shouldRetry(requestWithRetryCount(3)));
        assertFalse(policy.shouldRetry(requestWithRetryCount(4)));
    }
}
