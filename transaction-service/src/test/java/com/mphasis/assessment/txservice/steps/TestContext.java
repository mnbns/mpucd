package com.mphasis.assessment.txservice.steps;

import com.mphasis.assessment.txservice.model.TransactionRequest;
import com.mphasis.assessment.txservice.model.TransactionType;
import com.mphasis.assessment.txservice.ordering.SequenceBuffer;
import com.mphasis.assessment.txservice.retry.RetryPolicy;
import com.mphasis.assessment.txservice.service.ProcessingSummary;
import com.mphasis.assessment.txservice.service.TransactionProcessor;
import com.mphasis.assessment.txservice.service.ValidationResult;
import com.mphasis.assessment.txservice.service.Validator;
import com.mphasis.assessment.txservice.store.AccountLedger;
import com.mphasis.assessment.txservice.store.IdempotencyStore;
import com.mphasis.assessment.txservice.store.InMemoryIdempotencyStore;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One fresh instance per Cucumber scenario (created by PicoContainer), shared
 * across every step-def class so a scenario can span several "themed" step
 * files without losing state.
 */
public class TestContext {

    public final Validator validator = new Validator();
    public final IdempotencyStore idempotencyStore = new InMemoryIdempotencyStore();
    public final SequenceBuffer sequenceBuffer = new SequenceBuffer();
    public final RetryPolicy retryPolicy = new RetryPolicy(3);
    public final AccountLedger ledger = new AccountLedger();
    public final TransactionProcessor processor =
            new TransactionProcessor(validator, idempotencyStore, sequenceBuffer, retryPolicy, ledger);

    /** Every request built or submitted in this scenario, keyed by requestId, in submission order. */
    public final Map<String, TransactionRequest> requestsByRequestId = new LinkedHashMap<>();
    public final List<TransactionRequest> batch = new ArrayList<>();

    public TransactionRequest currentRequest;
    public ValidationResult lastValidationResult;
    public ProcessingSummary lastSummary;

    private int counter = 0;

    public TransactionRequest buildValidRequest() {
        counter++;
        return remember(new TransactionRequest(
                "TXN-CTX-" + counter, "REQ-CTX-" + counter, "ACC-CTX-" + counter,
                TransactionType.CREDIT, new BigDecimal("10"), "USD", 1));
    }

    public TransactionRequest buildRequest(String transactionId, String requestId, String accountId,
                                            TransactionType type, BigDecimal amount, long sequenceNumber) {
        return remember(new TransactionRequest(transactionId, requestId, accountId, type, amount, "USD", sequenceNumber));
    }

    public TransactionRequest buildRequest(String transactionId, String requestId, String accountId,
                                            TransactionType type, BigDecimal amount, long sequenceNumber,
                                            int simulatedFailures) {
        TransactionRequest request = new TransactionRequest(transactionId, requestId, accountId,
                type, amount, "USD", sequenceNumber, simulatedFailures);
        return remember(request);
    }

    private TransactionRequest remember(TransactionRequest request) {
        requestsByRequestId.put(request.getRequestId(), request);
        currentRequest = request;
        return request;
    }

    public void submit(TransactionRequest request) {
        remember(request);
        batch.add(request);
        processor.process(request);
    }
}
