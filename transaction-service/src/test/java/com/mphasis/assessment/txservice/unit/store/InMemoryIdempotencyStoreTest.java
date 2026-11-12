package com.mphasis.assessment.txservice.unit.store;

import com.mphasis.assessment.txservice.store.InMemoryIdempotencyStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for {@link InMemoryIdempotencyStore}, the only {@code IdempotencyStore} implementation. */
class InMemoryIdempotencyStoreTest {

    private final InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();

    @Test
    void requestIdIsNotProcessedUntilMarked() {
        assertFalse(store.isProcessed("REQ-1"));
        store.markProcessed("REQ-1");
        assertTrue(store.isProcessed("REQ-1"));
    }

    @Test
    void transactionIdIsNotProcessedUntilMarked() {
        assertFalse(store.isTransactionProcessed("TXN-1"));
        store.markTransactionProcessed("TXN-1");
        assertTrue(store.isTransactionProcessed("TXN-1"));
    }

    @Test
    void requestIdAndTransactionIdKeySpacesAreIndependent() {
        store.markProcessed("SAME-ID");
        assertTrue(store.isProcessed("SAME-ID"));
        assertFalse(store.isTransactionProcessed("SAME-ID"));
    }

    @Test
    void unrelatedIdsStayUnaffected() {
        store.markProcessed("REQ-1");
        assertFalse(store.isProcessed("REQ-2"));
    }
}
