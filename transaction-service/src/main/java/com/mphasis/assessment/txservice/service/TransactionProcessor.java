package com.mphasis.assessment.txservice.service;

import com.mphasis.assessment.txservice.model.TransactionRequest;
import com.mphasis.assessment.txservice.model.TransactionStatus;
import com.mphasis.assessment.txservice.ordering.SequenceBuffer;
import com.mphasis.assessment.txservice.retry.RetryPolicy;
import com.mphasis.assessment.txservice.retry.TransientProcessingException;
import com.mphasis.assessment.txservice.store.AccountLedger;
import com.mphasis.assessment.txservice.store.IdempotencyStore;
import com.mphasis.assessment.txservice.util.AccountIdMasking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Orchestrates one request through: validate (US-02) -> duplicate check
 * (US-04) -> sequence/ordering gate (US-05) -> apply to the ledger with
 * retry (US-06/US-07) -> release anything that was waiting on this
 * account's sequence. Every transition is logged (US-08) without ever
 * printing the full accountId (US-09).
 */
public class TransactionProcessor {

    private static final Logger log = LoggerFactory.getLogger(TransactionProcessor.class);

    private final Validator validator;
    private final IdempotencyStore idempotencyStore;
    private final SequenceBuffer sequenceBuffer;
    private final RetryPolicy retryPolicy;
    private final AccountLedger ledger;

    public TransactionProcessor(Validator validator, IdempotencyStore idempotencyStore,
                                 SequenceBuffer sequenceBuffer, RetryPolicy retryPolicy, AccountLedger ledger) {
        this.validator = validator;
        this.idempotencyStore = idempotencyStore;
        this.sequenceBuffer = sequenceBuffer;
        this.retryPolicy = retryPolicy;
        this.ledger = ledger;
    }

    public void process(TransactionRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        ValidationResult validation = validator.validate(request);
        if (!validation.isValid()) {
            setStatus(request, TransactionStatus.FAILED);
            request.setFailureReason(validation.getReason());
            logEvent(request, "rejected: " + validation.getReason());
            // Only advance the cursor if this slot was actually due now - otherwise an
            // invalid record arriving ahead of its turn would wrongly fast-forward past
            // legitimately pending earlier sequence numbers.
            if (sequenceBuffer.isReadyToProcess(request)) {
                sequenceBuffer.markResolved(request);
                releaseNowReady();
            }
            return;
        }

        if (idempotencyStore.isProcessed(request.getRequestId())) {
            setStatus(request, TransactionStatus.DUPLICATE);
            logEvent(request, "requestId already processed");
            return;
        }

        if (idempotencyStore.isTransactionProcessed(request.getTransactionId())) {
            setStatus(request, TransactionStatus.DUPLICATE);
            logEvent(request, "transactionId already processed under a different requestId");
            return;
        }

        if (sequenceBuffer.isAlreadyResolved(request)) {
            setStatus(request, TransactionStatus.DUPLICATE);
            logEvent(request, "sequence already resolved for this batch - treating as a stale replay");
            return;
        }

        setStatus(request, TransactionStatus.RECEIVED);
        logEvent(request, "received");

        if (!sequenceBuffer.isReadyToProcess(request)) {
            setStatus(request, TransactionStatus.PENDING_ORDER);
            sequenceBuffer.buffer(request);
            logEvent(request, "waiting for an earlier sequence in the batch");
            return;
        }

        attemptProcess(request);
        releaseNowReady();
    }

    private void releaseNowReady() {
        TransactionRequest next = sequenceBuffer.pollReady();
        while (next != null) {
            attemptProcess(next);
            next = sequenceBuffer.pollReady();
        }
    }

    private void attemptProcess(TransactionRequest request) {
        setStatus(request, TransactionStatus.PROCESSING);
        logEvent(request, "processing, attempt " + (request.getRetryCount() + 1));

        try {
            if (request.getRetryCount() < request.getSimulatedFailures()) {
                throw new TransientProcessingException("simulated transient failure");
            }
            ledger.apply(request.getAccountId(), request.getTransactionType(), request.getAmount());
            sequenceBuffer.markResolved(request);
            idempotencyStore.markProcessed(request.getRequestId());
            idempotencyStore.markTransactionProcessed(request.getTransactionId());
            setStatus(request, TransactionStatus.PROCESSED);
            logEvent(request, "processed");
        } catch (TransientProcessingException e) {
            if (retryPolicy.shouldRetry(request)) {
                request.incrementRetryCount();
                setStatus(request, TransactionStatus.RETRY_PENDING);
                logEvent(request, "transient failure, will retry: " + e.getMessage());
                attemptProcess(request);
            } else {
                sequenceBuffer.markResolved(request);
                setStatus(request, TransactionStatus.FAILED);
                request.setFailureReason("retries exhausted after " + request.getRetryCount() + " attempt(s): " + e.getMessage());
                logEvent(request, request.getFailureReason());
            }
        }
    }

    private void setStatus(TransactionRequest request, TransactionStatus status) {
        request.setStatus(status);
    }

    private void logEvent(TransactionRequest request, String note) {
        log.info("{} transactionId={} requestId={} accountId={} seq={} -> {} ({})",
                request.getStatus(), request.getTransactionId(), request.getRequestId(),
                AccountIdMasking.mask(request.getAccountId()), request.getSequenceNumber(), request.getStatus(), note);
    }
}
