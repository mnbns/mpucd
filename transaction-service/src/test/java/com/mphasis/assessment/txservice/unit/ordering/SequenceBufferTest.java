package com.mphasis.assessment.txservice.unit.ordering;

import com.mphasis.assessment.txservice.model.TransactionRequest;
import com.mphasis.assessment.txservice.model.TransactionType;
import com.mphasis.assessment.txservice.ordering.SequenceBuffer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for {@link SequenceBuffer}: the single batch-wide ordering cursor. */
class SequenceBufferTest {

    private final SequenceBuffer buffer = new SequenceBuffer();

    private TransactionRequest requestAt(long sequenceNumber, String requestId) {
        return new TransactionRequest("TXN-" + requestId, requestId, "ACC-1",
                TransactionType.CREDIT, new BigDecimal("1"), "USD", sequenceNumber);
    }

    @Test
    void freshBufferIsReadyOnlyForSequenceOne() {
        assertTrue(buffer.isReadyToProcess(requestAt(1, "R1")));
        assertFalse(buffer.isReadyToProcess(requestAt(2, "R2")));
    }

    @Test
    void isAlreadyResolvedIsFalseBeforeAnythingResolves() {
        assertFalse(buffer.isAlreadyResolved(requestAt(1, "R1")));
    }

    @Test
    void markResolvedAdvancesTheCursorAndUnlocksTheNextSequence() {
        buffer.markResolved(requestAt(1, "R1"));
        assertTrue(buffer.isReadyToProcess(requestAt(2, "R2")));
        assertFalse(buffer.isReadyToProcess(requestAt(3, "R3")));
    }

    @Test
    void markResolvedNeverMovesTheCursorBackward() {
        buffer.markResolved(requestAt(5, "R5"));
        buffer.markResolved(requestAt(2, "R2")); // a lower/stale sequence resolved afterward
        assertTrue(buffer.isReadyToProcess(requestAt(6, "R6")));
    }

    @Test
    void isAlreadyResolvedIsTrueForAnySequenceAtOrBelowTheCursor() {
        buffer.markResolved(requestAt(3, "R3"));
        assertTrue(buffer.isAlreadyResolved(requestAt(1, "R1")));
        assertTrue(buffer.isAlreadyResolved(requestAt(3, "R3")));
        assertFalse(buffer.isAlreadyResolved(requestAt(4, "R4")));
    }

    @Test
    void pollReadyReturnsNullWhenNothingIsBufferedForTheNextSlot() {
        assertNull(buffer.pollReady());
    }

    @Test
    void bufferThenPollReadyReleasesOnceItsTurnArrives() {
        TransactionRequest parked = requestAt(2, "R2");
        buffer.buffer(parked);

        assertNull(buffer.pollReady()); // sequence 2 isn't ready yet (cursor is at 0)

        buffer.markResolved(requestAt(1, "R1"));
        TransactionRequest released = buffer.pollReady();
        assertSame(parked, released);

        // it was removed, not just read
        assertNull(buffer.pollReady());
        assertFalse(buffer.stillBuffered().containsKey(2L));
    }

    @Test
    void stillBufferedReflectsEverythingNotYetReleased() {
        buffer.buffer(requestAt(2, "R2"));
        buffer.buffer(requestAt(3, "R3"));
        assertTrue(buffer.stillBuffered().containsKey(2L));
        assertTrue(buffer.stillBuffered().containsKey(3L));
    }
}
