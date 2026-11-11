package com.mphasis.assessment.txservice.ordering;

import com.mphasis.assessment.txservice.model.TransactionRequest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * US-05 / L2-FR-07 / NFR-02: Option A, sequence-based buffering - a single
 * ordering cursor across the whole batch (not per account). A request is
 * ready only once every earlier sequence number in the batch has been
 * resolved, successfully or not; otherwise it is parked until its
 * predecessor's slot is resolved and {@link #pollReady} releases it.
 *
 * Corrected from an earlier per-account design: with a per-account cursor,
 * an account that only ever appears once at a high sequence number (e.g.
 * ACC-203 at sequence 5 in §9.8) could never become "ready", so it would
 * never reach the retry logic - contradicting §9.8's expected outcome for
 * that row. A single batch-wide cursor, advanced on every resolved
 * sequence number (including validation failures, so one permanently
 * failed record never blocks everything after it), reproduces every
 * §9.8 expected outcome exactly.
 */
public class SequenceBuffer {

    private volatile long lastResolvedSequence = 0;
    private final Map<Long, TransactionRequest> buffered = new ConcurrentHashMap<>();

    public synchronized boolean isReadyToProcess(TransactionRequest request) {
        return request.getSequenceNumber() == lastResolvedSequence + 1;
    }

    /** Its slot was already resolved by an earlier request - this one is a replay, not new work. */
    public synchronized boolean isAlreadyResolved(TransactionRequest request) {
        return request.getSequenceNumber() <= lastResolvedSequence;
    }

    public void buffer(TransactionRequest request) {
        buffered.put(request.getSequenceNumber(), request);
    }

    /** Call once a request reaches ANY terminal outcome (PROCESSED or FAILED) to advance the cursor. */
    public synchronized void markResolved(TransactionRequest request) {
        lastResolvedSequence = Math.max(lastResolvedSequence, request.getSequenceNumber());
    }

    /** Returns and removes the next buffered request, if its turn has now arrived. */
    public synchronized TransactionRequest pollReady() {
        return buffered.remove(lastResolvedSequence + 1);
    }

    /** Requests still parked at the end of a run - their predecessor sequence never arrived (AC-05.3). */
    public Map<Long, TransactionRequest> stillBuffered() {
        return buffered;
    }
}
