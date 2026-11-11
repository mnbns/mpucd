package com.mphasis.assessment.txservice.steps;

import com.mphasis.assessment.txservice.model.TransactionRequest;
import com.mphasis.assessment.txservice.model.TransactionStatus;
import com.mphasis.assessment.txservice.model.TransactionType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * US-06: Retry-Safe Resilience. The engine resolves a request to a terminal
 * status synchronously within one process() call, so RETRY_PENDING is never
 * observable as a final state - retryCount > 0 is the available proxy for
 * "it passed through at least one retry".
 */
public class RetrySteps {

    private final ScenarioContext context;
    /** Set per-scenario so the generic "it is marked PROCESSED exactly once" Then step below can check either case. */
    private String accountIdUnderTest;
    private BigDecimal expectedBalance;

    public RetrySteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("a transaction simulated to fail transiently, as in section 9.8 row 6, loaded from {string}")
    public void a_transaction_simulated_to_fail_transiently(String fixturePath) {
        // The fixture's own "note" explains why its sequenceNumber (1) deliberately differs
        // from the real batch's (5): this isolated scenario's fresh, empty SequenceBuffer
        // would otherwise buffer it instead of treating it as immediately ready.
        context.remember(TestDataLoader.loadRequest(fixturePath));
    }

    @When("processing fails")
    public void processing_fails() {
        context.submit(context.currentRequest);
    }

    @Then("status becomes RETRY_PENDING")
    public void status_becomes_retry_pending() {
        assertTrue(context.currentRequest.getRetryCount() >= 1, "expected at least one retry to have occurred");
    }

    @Then("retryCount is incremented")
    public void retryCount_is_incremented() {
        assertEquals(1, context.currentRequest.getRetryCount());
        assertEquals(TransactionStatus.PROCESSED, context.currentRequest.getStatus());
    }

    @Given("a transaction retried up to the configured max retry count and still failing")
    public void a_transaction_retried_up_to_max_and_still_failing() {
        context.buildRequest("TXN-NEVER", "REQ-NEVER", "ACC-NEVER", TransactionType.CREDIT, new BigDecimal("75"), 1, 99);
    }

    @When("the last retry fails")
    public void the_last_retry_fails() {
        context.submit(context.currentRequest);
    }

    @Then("status becomes FAILED with a recorded reason")
    public void status_becomes_failed_with_a_recorded_reason() {
        assertEquals(TransactionStatus.FAILED, context.currentRequest.getStatus());
        assertNotNull(context.currentRequest.getFailureReason());
        assertEquals(3, context.currentRequest.getRetryCount());
    }

    @Then("no partial effect remains")
    public void no_partial_effect_remains() {
        assertEquals(BigDecimal.ZERO, context.ledger.getBalance("ACC-NEVER"));
    }

    @Given("a transaction that succeeds on a later retry attempt")
    public void a_transaction_that_succeeds_on_a_later_retry_attempt() {
        context.buildRequest("TXN-EVENTUAL", "REQ-EVENTUAL", "ACC-EVENTUAL", TransactionType.CREDIT, new BigDecimal("60"), 1, 2);
        accountIdUnderTest = "ACC-EVENTUAL";
        expectedBalance = new BigDecimal("60");
    }

    @Given("a transaction that succeeds only on the final allowed retry attempt")
    public void a_transaction_that_succeeds_only_on_the_final_allowed_retry_attempt() {
        // maxRetries=3 (DD-04): simulatedFailures=3 fails attempts 1-3 (retryCount reaches 3,
        // the same count as the exhaustion case) and only succeeds on attempt 4 - the exact
        // boundary where RetryPolicy.shouldRetry's `<` would become `<=` in an off-by-one regression.
        context.buildRequest("TXN-BOUNDARY", "REQ-BOUNDARY", "ACC-BOUNDARY", TransactionType.CREDIT, new BigDecimal("45"), 1, 3);
        accountIdUnderTest = "ACC-BOUNDARY";
        expectedBalance = new BigDecimal("45");
    }

    @When("it succeeds")
    public void it_succeeds() {
        context.submit(context.currentRequest);
    }

    @Then("it is marked PROCESSED exactly once")
    public void it_is_marked_processed_exactly_once() {
        assertEquals(TransactionStatus.PROCESSED, context.currentRequest.getStatus());
        assertEquals(expectedBalance, context.ledger.getBalance(accountIdUnderTest));
    }

    @Then("retryCount stopped at the configured maximum")
    public void retryCount_stopped_at_the_configured_maximum() {
        assertEquals(3, context.currentRequest.getRetryCount());
    }

    @Then("the idempotency store prevents any earlier partial attempt from also taking effect")
    public void the_idempotency_store_prevents_any_earlier_partial_attempt() {
        assertTrue(context.idempotencyStore.isProcessed(context.currentRequest.getRequestId()));
        TransactionRequest replay = context.buildRequest("TXN-EVENTUAL", "REQ-EVENTUAL", "ACC-EVENTUAL",
                TransactionType.CREDIT, new BigDecimal("60"), 1, 2);
        context.submit(replay);
        assertEquals(TransactionStatus.DUPLICATE, replay.getStatus());
        assertEquals(new BigDecimal("60"), context.ledger.getBalance("ACC-EVENTUAL"));
    }
}
