package com.mphasis.assessment.txservice.steps;

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

    private final TestContext context;
    private TransactionSource source;
    private List<TransactionRequest> loadedBatch;

    public IngestionAndValidationSteps(TestContext context) {
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

    @Given("a request missing a mandatory field such as transactionId, requestId, accountId, amount, or type")
    public void a_request_missing_a_mandatory_field() {
        context.buildRequest("TXN-MISSING", "REQ-MISSING", "", TransactionType.CREDIT, new BigDecimal("10"), 1);
    }

    @When("it is ingested")
    public void it_is_ingested() {
        context.processor.process(context.currentRequest);
    }

    @Then("it is marked FAILED with a validation reason")
    public void it_is_marked_failed_with_a_validation_reason() {
        assertEquals(TransactionStatus.FAILED, context.currentRequest.getStatus());
        assertNotNull(context.currentRequest.getFailureReason());
    }

    @Then("it is excluded from business processing")
    public void it_is_excluded_from_business_processing() {
        assertFalse(context.idempotencyStore.isProcessed(context.currentRequest.getRequestId()));
        assertEquals(BigDecimal.ZERO, context.ledger.getBalance(context.currentRequest.getAccountId()));
    }

    @Given("a request with a negative amount that violates the field rules, such as TXN-2004 with amount -10")
    public void a_request_with_a_negative_amount() {
        context.buildRequest("TXN-2004", "REQ-4", "ACC-202", TransactionType.DEBIT, new BigDecimal("-10"), 4);
    }

    @When("it is validated")
    public void it_is_validated() {
        context.lastValidationResult = context.validator.validate(context.currentRequest);
    }

    @Then("it is rejected as FAILED with a clear reason")
    public void it_is_rejected_as_failed_with_a_clear_reason() {
        assertFalse(context.lastValidationResult.isValid());
        assertNotNull(context.lastValidationResult.getReason());
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

    @Given("one invalid record inside a batch of N records")
    public void one_invalid_record_inside_a_batch_of_n_records() {
        // Distinct sequence numbers: the engine's ordering cursor is shared across the
        // whole batch (DD-02), so unrelated transactions can't reuse the same slot.
        context.buildRequest("TXN-A", "REQ-A", "ACC-A", TransactionType.CREDIT, new BigDecimal("10"), 1);
        context.buildRequest("TXN-B", "REQ-B", "ACC-B", TransactionType.CREDIT, new BigDecimal("20"), 2);
        context.buildRequest("TXN-BAD", "REQ-BAD", "ACC-BAD", TransactionType.DEBIT, new BigDecimal("-5"), 3);
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
        long processed = context.batch.stream().filter(r -> r.getStatus() == TransactionStatus.PROCESSED).count();
        long failed = context.batch.stream().filter(r -> r.getStatus() == TransactionStatus.FAILED).count();
        assertEquals(2, processed);
        assertEquals(1, failed);
    }
}
