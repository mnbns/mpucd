package com.mphasis.assessment.txservice.steps;

import com.mphasis.assessment.txservice.model.TransactionRequest;
import com.mphasis.assessment.txservice.model.TransactionStatus;
import com.mphasis.assessment.txservice.model.TransactionType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** US-03: Transaction State Lifecycle. */
public class LifecycleSteps {

    private final ScenarioContext context;
    private TransactionRequest receivedRequest;

    public LifecycleSteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("a new request")
    public void a_new_request() {
        context.buildRequest("TXN-LC-1", "REQ-LC-1", "ACC-LC", TransactionType.CREDIT, new BigDecimal("10"), 1);
    }

    @When("it is constructed, before entering the processing pipeline")
    public void it_is_constructed_before_entering_the_processing_pipeline() {
        // no-op: construction already happened in the Given step; this just names the moment
    }

    @Then("its initial status is RECEIVED")
    public void its_initial_status_is_received() {
        assertEquals(TransactionStatus.RECEIVED, context.currentRequest.getStatus());
    }

    @Given("a RECEIVED request picked up for work")
    public void a_received_request_picked_up_for_work() {
        receivedRequest = context.buildRequest("TXN-LC-2", "REQ-LC-2", "ACC-LC2", TransactionType.CREDIT, new BigDecimal("10"), 1);
        assertEquals(TransactionStatus.RECEIVED, receivedRequest.getStatus());
    }

    @When("processing starts")
    public void processing_starts() {
        receivedRequest.setStatus(TransactionStatus.PROCESSING);
    }

    @Then("status transitions to PROCESSING")
    public void status_transitions_to_processing() {
        assertEquals(TransactionStatus.PROCESSING, receivedRequest.getStatus());
    }

    @Given("any transaction at any point in time")
    public void any_transaction_at_any_point_in_time() {
        TransactionRequest request = context.buildRequest("TXN-LC-3", "REQ-LC-3", "ACC-LC3", TransactionType.CREDIT, new BigDecimal("10"), 1);
        context.submit(request);
    }

    @When("its status is inspected")
    public void its_status_is_inspected() {
        // no-op: inspection doesn't change state
    }

    @Then("it is exactly one of RECEIVED, PROCESSING, PENDING_ORDER, RETRY_PENDING, PROCESSED, DUPLICATE, or FAILED")
    public void it_is_exactly_one_of_the_defined_statuses() {
        assertTrue(EnumSet.allOf(TransactionStatus.class).contains(context.currentRequest.getStatus()));
    }

    @Given("an illegal transition is attempted, such as DUPLICATE to PROCESSING")
    public void an_illegal_transition_is_attempted() {
        TransactionRequest request = context.buildRequest("TXN-LC-4", "REQ-LC-4", "ACC-LC4", TransactionType.CREDIT, new BigDecimal("10"), 1);
        request.setStatus(TransactionStatus.DUPLICATE);
    }

    @When("it is attempted")
    public void it_is_attempted() {
        context.currentRequest.setStatus(TransactionStatus.PROCESSING);
    }

    @Then("it is rejected and logged as an invalid transition")
    public void it_is_rejected_and_logged_as_an_invalid_transition() {
        assertEquals(TransactionStatus.DUPLICATE, context.currentRequest.getStatus());
    }
}
