package com.mphasis.assessment.txservice.steps;

import com.mphasis.assessment.txservice.model.TransactionRequest;
import com.mphasis.assessment.txservice.model.TransactionStatus;
import com.mphasis.assessment.txservice.model.TransactionType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * US-12: Decoupled Background Processing Worker - verifies the ingestion
 * loop (producer) and {@link com.mphasis.assessment.txservice.worker.BackgroundWorker}
 * (single consumer) are a real producer/consumer split, not an inline call
 * stack re-labelled.
 */
public class BackgroundWorkerSteps {

    private final ScenarioContext context;
    private List<TransactionRequest> orderedPair;

    public BackgroundWorkerSteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("a background worker wired to the scenario's transaction processor")
    public void a_background_worker_wired_to_the_scenarios_transaction_processor() {
        // no-op: ScenarioContext.worker is already constructed around this scenario's processor.
    }

    @When("requestId {string} is submitted to the background worker before it is started")
    public void requestId_is_submitted_to_the_background_worker_before_it_is_started(String requestId) {
        TransactionRequest request = context.buildRequest(
                "TXN-" + requestId, requestId, "ACC-" + requestId, TransactionType.CREDIT, new BigDecimal("10"), 1);
        context.batch.add(request);
        context.worker.submit(request);
    }

    @When("{int} valid requests in sequence are submitted to the background worker before it is started")
    public void n_valid_requests_in_sequence_are_submitted_to_the_background_worker_before_it_is_started(int count) {
        for (int i = 1; i <= count; i++) {
            TransactionRequest request = context.buildRequest(
                    "TXN-SEQ-" + i, "REQ-SEQ-" + i, "ACC-SEQ-" + i, TransactionType.CREDIT, new BigDecimal("10"), i);
            context.batch.add(request);
            context.worker.submit(request);
        }
    }

    @Then("requestId {string} has not been touched by processing yet")
    public void requestId_has_not_been_touched_by_processing_yet(String requestId) {
        TransactionRequest request = context.requestsByRequestId.get(requestId);
        assertEquals(TransactionStatus.RECEIVED, request.getStatus());
        assertFalse(context.idempotencyStore.isProcessed(requestId));
    }

    @Then("none of the submitted requests have been touched by processing yet")
    public void none_of_the_submitted_requests_have_been_touched_by_processing_yet() {
        for (TransactionRequest request : context.batch) {
            assertEquals(TransactionStatus.RECEIVED, request.getStatus());
            assertFalse(context.idempotencyStore.isProcessed(request.getRequestId()));
        }
    }

    @When("the background worker is started and allowed to drain")
    public void the_background_worker_is_started_and_allowed_to_drain() {
        context.worker.start();
        context.worker.awaitDrained();
    }

    @Given("two sequence-dependent requests for the same account, as in section 9.8 rows 1 and 2, loaded from {string}")
    public void two_sequence_dependent_requests_for_the_same_account(String fixturePath) {
        orderedPair = TestDataLoader.loadRequests(fixturePath);
        for (TransactionRequest request : orderedPair) {
            context.remember(request);
        }
    }

    @When("they are submitted to the background worker in their documented arrival order, before it is started")
    public void they_are_submitted_to_the_background_worker_in_their_documented_arrival_order() {
        for (TransactionRequest request : orderedPair) {
            context.batch.add(request);
            context.worker.submit(request);
        }
    }

    @Then("the background worker used exactly one consumer thread")
    public void the_background_worker_used_exactly_one_consumer_thread() {
        assertEquals(1, context.worker.consumerThreadCount());
    }
}
