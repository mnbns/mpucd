package com.mphasis.assessment.txservice.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mphasis.assessment.txservice.input.InMemoryTransactionSource;
import com.mphasis.assessment.txservice.input.TransactionSource;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** US-01: Configurable Transaction Ingestion / US-02: Input Validation. */
public class IngestionAndValidationSteps {

    private final ScenarioContext context;
    private TransactionSource source;
    private List<TransactionRequest> loadedBatch;

    /** Set by a US-02 Given step; read back by its own Then step(s) to assert against the same fixture's expected-* fields. */
    private String currentFixturePath;

    public IngestionAndValidationSteps(ScenarioContext context) {
        this.context = context;
    }

    // --- US-01 ---

    @Given("an in-memory list of transaction requests")
    public void an_in_memory_list_of_transaction_requests() {
        source = new InMemoryTransactionSource("sample-transactions.json", new ObjectMapper());
    }

    @When("the service starts")
    public void the_service_starts() {
        loadedBatch = source.fetchBatch();
    }

    @Then("it loads all requests through a single TransactionSource interface")
    public void it_loads_all_requests_through_a_single_transaction_source_interface() {
        assertTrue(source instanceof TransactionSource);
        assertNotNull(loadedBatch);
        assertFalse(loadedBatch.isEmpty());
    }

    @Given("the input source implementation is swapped for a file reader")
    public void the_input_source_implementation_is_swapped_for_a_file_reader() {
        // InMemoryTransactionSource already reads a bundled JSON file - "swapping" it in for
        // the processor proves the processor never needed to know the difference.
        source = new InMemoryTransactionSource("sample-transactions.json", new ObjectMapper());
    }

    @When("the service runs")
    public void the_service_runs() {
        loadedBatch = source.fetchBatch();
        context.submit(loadedBatch.get(1)); // REQ-1, seq=1 - resolves cleanly on its own
    }

    @Then("no change is required in the validator, processor, or state-store classes")
    public void no_change_is_required_in_the_validator_processor_or_state_store_classes() {
        assertEquals(TransactionStatus.PROCESSED, loadedBatch.get(1).getStatus());
    }

    // --- US-02 ---

    @Given("a request missing a mandatory field such as transactionId, requestId, accountId, amount, or type, loaded from {string}")
    public void a_request_missing_a_mandatory_field(String fixturePath) {
        currentFixturePath = fixturePath;
        context.remember(TestDataLoader.loadRequest(fixturePath));
    }

    @When("it is ingested")
    public void it_is_ingested() {
        context.processor.process(context.currentRequest);
    }

    @Then("it is marked FAILED with a validation reason")
    public void it_is_marked_failed_with_a_validation_reason() {
        JsonNode expected = TestDataLoader.load(currentFixturePath);
        assertEquals(expected.get("expectedStatus").asText(), context.currentRequest.getStatus().name());
        if (expected.get("expectedFailureReasonPresent").asBoolean()) {
            assertNotNull(context.currentRequest.getFailureReason());
        }
    }

    @Then("it is excluded from business processing")
    public void it_is_excluded_from_business_processing() {
        assertFalse(context.idempotencyStore.isProcessed(context.currentRequest.getRequestId()));
        assertEquals(BigDecimal.ZERO, context.ledger.getBalance(context.currentRequest.getAccountId()));
    }

    @Given("a request with a negative amount that violates the field rules, loaded from {string}")
    public void a_request_with_a_negative_amount(String fixturePath) {
        currentFixturePath = fixturePath;
        context.remember(TestDataLoader.loadRequest(fixturePath));
    }

    @When("it is validated")
    public void it_is_validated() {
        context.lastValidationResult = context.validator.validate(context.currentRequest);
    }

    @Then("it is rejected as FAILED with a clear reason")
    public void it_is_rejected_as_failed_with_a_clear_reason() {
        JsonNode expected = TestDataLoader.loadExpected(currentFixturePath, "expectedValidation");
        assertEquals(expected.get("valid").asBoolean(), context.lastValidationResult.isValid());
        if (expected.get("reasonPresent").asBoolean()) {
            assertNotNull(context.lastValidationResult.getReason());
        }
    }

    @Then("the rest of the batch still processes")
    public void the_rest_of_the_batch_still_processes() {
        TransactionRequest invalid = context.currentRequest;
        TransactionRequest valid = context.buildRequest("TXN-OK", "REQ-OK", "ACC-OK",
                TransactionType.CREDIT, new BigDecimal("20"), 1);
        context.submit(invalid);
        context.submit(valid);
        assertEquals(TransactionStatus.FAILED, invalid.getStatus());
        assertEquals(TransactionStatus.PROCESSED, valid.getStatus());
    }

    @Given("one invalid record inside a batch of N records, loaded from {string}")
    public void one_invalid_record_inside_a_batch_of_n_records(String fixturePath) {
        currentFixturePath = fixturePath;
        // Each fixture request keeps its own distinct sequence number: the engine's ordering
        // cursor is shared across the whole batch (DD-02), so unrelated transactions can't
        // reuse the same slot.
        for (TransactionRequest request : TestDataLoader.loadRequests(fixturePath)) {
            context.remember(request);
        }
    }

    @When("the batch runs")
    public void the_batch_runs() {
        for (TransactionRequest request : context.requestsByRequestId.values()) {
            context.processor.process(request);
            context.batch.add(request);
        }
    }

    @Then("the remaining N-1 valid records are still processed normally")
    public void the_remaining_valid_records_are_still_processed_normally() {
        JsonNode expectedCounts = TestDataLoader.loadExpected(currentFixturePath, "expectedCounts");
        long processed = context.batch.stream().filter(r -> r.getStatus() == TransactionStatus.PROCESSED).count();
        long failed = context.batch.stream().filter(r -> r.getStatus() == TransactionStatus.FAILED).count();
        assertEquals(expectedCounts.get("PROCESSED").asLong(), processed);
        assertEquals(expectedCounts.get("FAILED").asLong(), failed);
    }
}
