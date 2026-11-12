package com.mphasis.assessment.txservice.unit.model;

import com.mphasis.assessment.txservice.model.TransactionRequest;
import com.mphasis.assessment.txservice.model.TransactionStatus;
import com.mphasis.assessment.txservice.model.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link TransactionRequest}: constructors, equals/hashCode
 * (requestId-only identity), and the ALLOWED_TRANSITIONS-gated setStatus.
 */
class TransactionRequestTest {

    private TransactionRequest newRequest() {
        return new TransactionRequest("TXN-1", "REQ-1", "ACC-1",
                TransactionType.CREDIT, new BigDecimal("10"), "USD", 1, 2);
    }

    @Test
    void sevenArgConstructorSetsEveryField() {
        TransactionRequest request = newRequest();

        assertEquals("TXN-1", request.getTransactionId());
        assertEquals("REQ-1", request.getRequestId());
        assertEquals("ACC-1", request.getAccountId());
        assertEquals(TransactionType.CREDIT, request.getTransactionType());
        assertEquals(new BigDecimal("10"), request.getAmount());
        assertEquals("USD", request.getCurrency());
        assertEquals(1L, request.getSequenceNumber());
        assertEquals(2, request.getSimulatedFailures());
        assertEquals(TransactionStatus.RECEIVED, request.getStatus());
        assertEquals(0, request.getRetryCount());
        assertNull(request.getFailureReason());
    }

    @Test
    void sixArgConvenienceConstructorDefaultsSimulatedFailuresToZero() {
        TransactionRequest request = new TransactionRequest("TXN-2", "REQ-2", "ACC-2",
                TransactionType.DEBIT, new BigDecimal("5"), "USD", 3);

        assertEquals(0, request.getSimulatedFailures());
    }

    @Test
    void incrementRetryCountIncreasesByOneEachCall() {
        TransactionRequest request = newRequest();
        request.incrementRetryCount();
        request.incrementRetryCount();
        assertEquals(2, request.getRetryCount());
    }

    @Test
    void failureReasonIsSettableAndReadable() {
        TransactionRequest request = newRequest();
        request.setFailureReason("boom");
        assertEquals("boom", request.getFailureReason());
    }

    @Test
    void equalsAndHashCodeAreBasedOnRequestIdOnly() {
        TransactionRequest a = new TransactionRequest("TXN-A", "REQ-SAME", "ACC-A",
                TransactionType.CREDIT, new BigDecimal("1"), "USD", 1);
        TransactionRequest b = new TransactionRequest("TXN-B", "REQ-SAME", "ACC-B",
                TransactionType.DEBIT, new BigDecimal("999"), "EUR", 99);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equalsReturnsFalseForDifferentRequestIdOrDifferentType() {
        TransactionRequest a = newRequest();
        TransactionRequest differentRequestId = new TransactionRequest("TXN-1", "REQ-OTHER", "ACC-1",
                TransactionType.CREDIT, new BigDecimal("10"), "USD", 1);

        assertFalse(a.equals(differentRequestId));
        assertFalse(a.equals("not a request"));
        assertFalse(a.equals(null));
        assertTrue(a.equals(a));
    }

    // --- setStatus: legal forward transitions ---

    @Test
    void receivedMayMoveToProcessingPendingOrderDuplicateOrFailed() {
        assertTransitionSucceeds(TransactionStatus.RECEIVED, TransactionStatus.PROCESSING);
        assertTransitionSucceeds(TransactionStatus.RECEIVED, TransactionStatus.PENDING_ORDER);
        assertTransitionSucceeds(TransactionStatus.RECEIVED, TransactionStatus.DUPLICATE);
        assertTransitionSucceeds(TransactionStatus.RECEIVED, TransactionStatus.FAILED);
        assertTransitionSucceeds(TransactionStatus.RECEIVED, TransactionStatus.RECEIVED);
    }

    @Test
    void processingMayMoveToProcessedRetryPendingOrFailed() {
        assertTransitionSucceeds(TransactionStatus.PROCESSING, TransactionStatus.PROCESSED);
        assertTransitionSucceeds(TransactionStatus.PROCESSING, TransactionStatus.RETRY_PENDING);
        assertTransitionSucceeds(TransactionStatus.PROCESSING, TransactionStatus.FAILED);
        assertTransitionSucceeds(TransactionStatus.PROCESSING, TransactionStatus.PROCESSING);
    }

    @Test
    void retryPendingMayOnlyReturnToProcessing() {
        assertTransitionSucceeds(TransactionStatus.RETRY_PENDING, TransactionStatus.PROCESSING);
        assertTransitionSucceeds(TransactionStatus.RETRY_PENDING, TransactionStatus.RETRY_PENDING);
    }

    @Test
    void pendingOrderMayOnlyMoveToProcessing() {
        assertTransitionSucceeds(TransactionStatus.PENDING_ORDER, TransactionStatus.PROCESSING);
        assertTransitionSucceeds(TransactionStatus.PENDING_ORDER, TransactionStatus.PENDING_ORDER);
    }

    // --- setStatus: illegal transitions silently no-op (logged, not thrown) ---

    @Test
    void illegalTransitionFromReceivedToProcessedIsSilentlyRejected() {
        assertTransitionRejected(TransactionStatus.RECEIVED, TransactionStatus.PROCESSED);
    }

    @Test
    void terminalStatusesRejectAnyTransitionAwayFromThemselves() {
        assertTransitionRejected(TransactionStatus.PROCESSED, TransactionStatus.RECEIVED);
        assertTransitionRejected(TransactionStatus.PROCESSED, TransactionStatus.FAILED);
        assertTransitionRejected(TransactionStatus.DUPLICATE, TransactionStatus.PROCESSING);
        assertTransitionRejected(TransactionStatus.FAILED, TransactionStatus.PROCESSED);
        // but self-transition on a terminal state is explicitly allowed
        assertTransitionSucceeds(TransactionStatus.PROCESSED, TransactionStatus.PROCESSED);
        assertTransitionSucceeds(TransactionStatus.DUPLICATE, TransactionStatus.DUPLICATE);
        assertTransitionSucceeds(TransactionStatus.FAILED, TransactionStatus.FAILED);
    }

    @Test
    void retryPendingRejectsDirectJumpToFailedOrProcessed() {
        assertTransitionRejected(TransactionStatus.RETRY_PENDING, TransactionStatus.FAILED);
        assertTransitionRejected(TransactionStatus.RETRY_PENDING, TransactionStatus.PROCESSED);
    }

    @Test
    void pendingOrderRejectsDirectJumpToFailedOrProcessed() {
        assertTransitionRejected(TransactionStatus.PENDING_ORDER, TransactionStatus.FAILED);
        assertTransitionRejected(TransactionStatus.PENDING_ORDER, TransactionStatus.PROCESSED);
    }

    /** Drives a request into {@code from} via the legal RECEIVED path, then asserts the move to {@code to} sticks. */
    private void assertTransitionSucceeds(TransactionStatus from, TransactionStatus to) {
        TransactionRequest request = driveTo(from);
        request.setStatus(to);
        assertEquals(to, request.getStatus());
    }

    /** Drives a request into {@code from}, then asserts an illegal move to {@code to} is silently ignored. */
    private void assertTransitionRejected(TransactionStatus from, TransactionStatus to) {
        TransactionRequest request = driveTo(from);
        request.setStatus(to);
        assertEquals(from, request.getStatus(), "illegal transition " + from + " -> " + to + " must be rejected (no-op)");
    }

    /** Walks a fresh (RECEIVED) request forward to the given status via only legal hops. */
    private TransactionRequest driveTo(TransactionStatus target) {
        TransactionRequest request = newRequest();
        if (target == TransactionStatus.RECEIVED) {
            return request;
        }
        if (target == TransactionStatus.PROCESSING || target == TransactionStatus.PROCESSED
                || target == TransactionStatus.RETRY_PENDING || target == TransactionStatus.FAILED
                || target == TransactionStatus.PENDING_ORDER) {
            if (target == TransactionStatus.PENDING_ORDER) {
                request.setStatus(TransactionStatus.PENDING_ORDER);
                return request;
            }
            request.setStatus(TransactionStatus.PROCESSING);
            if (target == TransactionStatus.PROCESSING) {
                return request;
            }
            request.setStatus(target);
            return request;
        }
        if (target == TransactionStatus.DUPLICATE) {
            request.setStatus(TransactionStatus.DUPLICATE);
            return request;
        }
        throw new IllegalArgumentException("unreachable in test: " + target);
    }
}
