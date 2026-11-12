package com.mphasis.assessment.txservice.unit.service;

import com.mphasis.assessment.txservice.model.TransactionRequest;
import com.mphasis.assessment.txservice.model.TransactionStatus;
import com.mphasis.assessment.txservice.model.TransactionType;
import com.mphasis.assessment.txservice.ordering.SequenceBuffer;
import com.mphasis.assessment.txservice.retry.RetryPolicy;
import com.mphasis.assessment.txservice.store.AccountLedger;
import com.mphasis.assessment.txservice.store.IdempotencyStore;
import com.mphasis.assessment.txservice.store.InMemoryIdempotencyStore;
import com.mphasis.assessment.txservice.service.TransactionProcessor;
import com.mphasis.assessment.txservice.service.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link TransactionProcessor}, the pipeline orchestrator.
 * Collaborators are the real lightweight in-memory implementations (same
 * pattern {@code ScenarioContext} uses for the Cucumber suite) rather than
 * mocks: {@link Validator}, {@link SequenceBuffer}, {@link RetryPolicy} and
 * {@link AccountLedger} are small deterministic POJOs with no I/O, so a
 * hand-rolled fake would add indirection without adding confidence. Each
 * test gets a fresh instance of every collaborator - none is shared across
 * tests - so sequencing/idempotency state never leaks between cases.
 */
class TransactionProcessorTest {

    private Validator validator;
    private IdempotencyStore idempotencyStore;
    private SequenceBuffer sequenceBuffer;
    private RetryPolicy retryPolicy;
    private AccountLedger ledger;
    private TransactionProcessor processor;

    @BeforeEach
    void setUp() {
        validator = new Validator();
        idempotencyStore = new InMemoryIdempotencyStore();
        sequenceBuffer = new SequenceBuffer();
        retryPolicy = new RetryPolicy(3);
        ledger = new AccountLedger();
        processor = new TransactionProcessor(validator, idempotencyStore, sequenceBuffer, retryPolicy, ledger);
    }

    private TransactionRequest request(String txnId, String reqId, String accountId, long seq) {
        return new TransactionRequest(txnId, reqId, accountId, TransactionType.CREDIT, new BigDecimal("10"), "USD", seq);
    }

    private TransactionRequest requestWithFailures(String txnId, String reqId, String accountId, long seq, int simulatedFailures) {
        return new TransactionRequest(txnId, reqId, accountId, TransactionType.CREDIT, new BigDecimal("10"), "USD", seq, simulatedFailures);
    }

    @Test
    void nullRequestIsRejectedByRequireNonNull() {
        assertThrows(NullPointerException.class, () -> processor.process(null));
    }

    @Test
    void happyPathSingleRequestBecomesProcessedAndUpdatesTheLedger() {
        TransactionRequest r1 = request("TXN-1", "REQ-1", "ACC-1", 1);
        processor.process(r1);

        assertEquals(TransactionStatus.PROCESSED, r1.getStatus());
        assertEquals(new BigDecimal("10"), ledger.getBalance("ACC-1"));
        assertTrue(idempotencyStore.isProcessed("REQ-1"));
        assertTrue(idempotencyStore.isTransactionProcessed("TXN-1"));
    }

    // --- validation failure path ---

    @Test
    void invalidRequestBecomesFailedWithTheValidationReason() {
        TransactionRequest invalid = request("TXN-1", "REQ-1", "", 1); // blank accountId
        processor.process(invalid);

        assertEquals(TransactionStatus.FAILED, invalid.getStatus());
        assertEquals("accountId is required", invalid.getFailureReason());
    }

    @Test
    void invalidRequestThatIsReadyNowAdvancesTheSequenceCursor() {
        TransactionRequest invalid = request("TXN-1", "REQ-1", "", 1); // ready (seq 1 == cursor+1)
        processor.process(invalid);

        // cursor advanced past 1, so a later valid request at sequence 2 is now immediately ready
        TransactionRequest next = request("TXN-2", "REQ-2", "ACC-2", 2);
        processor.process(next);
        assertEquals(TransactionStatus.PROCESSED, next.getStatus());
    }

    @Test
    void invalidRequestThatIsNotYetReadyDoesNotAdvanceTheCursorOrGetBuffered() {
        TransactionRequest invalidOutOfOrder = request("TXN-5", "REQ-5", "", 5); // invalid AND not ready
        processor.process(invalidOutOfOrder);

        assertEquals(TransactionStatus.FAILED, invalidOutOfOrder.getStatus());
        // the cursor must still be waiting on sequence 1, not advanced by the invalid record
        assertTrue(sequenceBuffer.isReadyToProcess(request("TXN-1", "REQ-1", "ACC-1", 1)));
        // and it is not left parked in the buffer either (process() returns before buffering on this path)
        assertFalse(sequenceBuffer.stillBuffered().containsKey(5L));
    }

    // --- duplicate detection: three distinct cases ---

    @Test
    void duplicateByRequestIdIsMarkedDuplicateAndHasNoLedgerEffect() {
        idempotencyStore.markProcessed("REQ-1");
        TransactionRequest replay = request("TXN-1", "REQ-1", "ACC-1", 1);
        processor.process(replay);

        assertEquals(TransactionStatus.DUPLICATE, replay.getStatus());
        assertEquals(BigDecimal.ZERO, ledger.getBalance("ACC-1"));
    }

    @Test
    void duplicateByTransactionIdUnderADifferentRequestIdIsMarkedDuplicate() {
        idempotencyStore.markTransactionProcessed("TXN-1");
        TransactionRequest sameTxnDifferentRequestId = request("TXN-1", "REQ-OTHER", "ACC-1", 1);
        processor.process(sameTxnDifferentRequestId);

        assertEquals(TransactionStatus.DUPLICATE, sameTxnDifferentRequestId.getStatus());
    }

    @Test
    void staleReplayOfAnAlreadyResolvedSequenceIsMarkedDuplicate() {
        // resolve sequence 1 for real first
        TransactionRequest original = request("TXN-1", "REQ-1", "ACC-1", 1);
        processor.process(original);
        assertEquals(TransactionStatus.PROCESSED, original.getStatus());

        // a completely different request (unique requestId/transactionId, so it is NOT an
        // idempotency duplicate) replaying sequence 1 after that slot is already resolved
        TransactionRequest staleReplay = request("TXN-99", "REQ-99", "ACC-99", 1);
        processor.process(staleReplay);

        assertEquals(TransactionStatus.DUPLICATE, staleReplay.getStatus());
        assertEquals(BigDecimal.ZERO, ledger.getBalance("ACC-99"));
    }

    // --- ordering / pending order ---

    @Test
    void outOfOrderRequestIsParkedAsPendingOrder() {
        TransactionRequest early = request("TXN-2", "REQ-2", "ACC-2", 2); // arrives before sequence 1
        processor.process(early);

        assertEquals(TransactionStatus.PENDING_ORDER, early.getStatus());
        assertTrue(sequenceBuffer.stillBuffered().containsKey(2L));
    }

    @Test
    void bufferedRequestIsReleasedAndProcessedOnceItsPredecessorArrives() {
        TransactionRequest seq2 = request("TXN-2", "REQ-2", "ACC-2", 2);
        processor.process(seq2);
        assertEquals(TransactionStatus.PENDING_ORDER, seq2.getStatus());

        TransactionRequest seq1 = request("TXN-1", "REQ-1", "ACC-1", 1);
        processor.process(seq1);

        assertEquals(TransactionStatus.PROCESSED, seq1.getStatus());
        assertEquals(TransactionStatus.PROCESSED, seq2.getStatus());
        assertFalse(sequenceBuffer.stillBuffered().containsKey(2L));
    }

    @Test
    void aChainOfOutOfOrderRequestsCascadesReleaseInSequenceOrder() {
        TransactionRequest seq3 = request("TXN-3", "REQ-3", "ACC-3", 3);
        TransactionRequest seq2 = request("TXN-2", "REQ-2", "ACC-2", 2);
        processor.process(seq3);
        processor.process(seq2);
        assertEquals(TransactionStatus.PENDING_ORDER, seq3.getStatus());
        assertEquals(TransactionStatus.PENDING_ORDER, seq2.getStatus());

        TransactionRequest seq1 = request("TXN-1", "REQ-1", "ACC-1", 1);
        processor.process(seq1);

        assertEquals(TransactionStatus.PROCESSED, seq1.getStatus());
        assertEquals(TransactionStatus.PROCESSED, seq2.getStatus());
        assertEquals(TransactionStatus.PROCESSED, seq3.getStatus());
    }

    // --- retry behaviour ---

    @Test
    void transientFailureBelowMaxRetriesEventuallySucceeds() {
        TransactionRequest flaky = requestWithFailures("TXN-1", "REQ-1", "ACC-1", 1, 1);
        processor.process(flaky);

        assertEquals(TransactionStatus.PROCESSED, flaky.getStatus());
        assertEquals(1, flaky.getRetryCount());
        assertEquals(new BigDecimal("10"), ledger.getBalance("ACC-1"));
    }

    @Test
    void retriesExhaustedResultsInFailedWithNoLedgerEffect() {
        TransactionRequest neverSucceeds = requestWithFailures("TXN-1", "REQ-1", "ACC-1", 1, 99);
        processor.process(neverSucceeds);

        assertEquals(TransactionStatus.FAILED, neverSucceeds.getStatus());
        assertEquals(3, neverSucceeds.getRetryCount()); // bounded by RetryPolicy(3)
        assertNotNull(neverSucceeds.getFailureReason());
        assertTrue(neverSucceeds.getFailureReason().contains("retries exhausted"));
        assertEquals(BigDecimal.ZERO, ledger.getBalance("ACC-1"));
        assertFalse(idempotencyStore.isProcessed("REQ-1"));
    }

    @Test
    void successOnTheExactFinalAllowedAttemptStillSucceeds() {
        // maxRetries=3: simulatedFailures=3 fails attempts 1-3 (retryCount reaches 3, the
        // same count as the exhaustion case) and only succeeds on attempt 4 - the boundary
        // where RetryPolicy's "<" would become "<=" in an off-by-one regression.
        TransactionRequest boundary = requestWithFailures("TXN-1", "REQ-1", "ACC-1", 1, 3);
        processor.process(boundary);

        assertEquals(TransactionStatus.PROCESSED, boundary.getStatus());
        assertEquals(3, boundary.getRetryCount());
        assertEquals(new BigDecimal("10"), ledger.getBalance("ACC-1"));
    }

    @Test
    void idempotencyPreventsAReplayOfAnAlreadySucceededRetriedRequestFromDoublingTheBalance() {
        TransactionRequest flaky = requestWithFailures("TXN-1", "REQ-1", "ACC-1", 1, 2);
        processor.process(flaky);
        assertEquals(TransactionStatus.PROCESSED, flaky.getStatus());

        TransactionRequest replay = requestWithFailures("TXN-1", "REQ-1", "ACC-1", 1, 2);
        processor.process(replay);

        assertEquals(TransactionStatus.DUPLICATE, replay.getStatus());
        assertEquals(new BigDecimal("10"), ledger.getBalance("ACC-1"));
    }

    @Test
    void failureReasonAndRetryCountAreNullOrZeroOnAFreshlyProcessedSuccess() {
        TransactionRequest r1 = request("TXN-1", "REQ-1", "ACC-1", 1);
        processor.process(r1);
        assertNull(r1.getFailureReason());
        assertEquals(0, r1.getRetryCount());
    }
}
