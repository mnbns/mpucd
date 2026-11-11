package com.mphasis.assessment.txservice.steps;

import com.mphasis.assessment.txservice.model.TransactionRequest;
import com.mphasis.assessment.txservice.model.TransactionStatus;
import com.mphasis.assessment.txservice.service.ProcessingSummary;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** end_to_end_batch.feature: replays the full §9.8 sample batch as one stateful run. */
public class EndToEndSteps {

    private final ScenarioContext context;

    public EndToEndSteps(ScenarioContext context) {
        this.context = context;
    }

    /**
     * Loads the real {@code sample-transactions.json} (the same file US-01's
     * InMemoryTransactionSource reads) instead of a hand-typed Gherkin table,
     * so this scenario can't silently drift from the actual §9.8 batch - a
     * table previously duplicated it by hand in two scenarios and dropped
     * TXN-2005's simulatedFailures=1 in the process (see DEFECT note in
     * branch-notes for this change).
     */
    @Given("the full section 9.8 sample batch is submitted in its documented input order")
    public void the_full_section_9_8_sample_batch_is_submitted() {
        for (TransactionRequest request : TestDataLoader.loadRequestArray("sample-transactions.json")) {
            context.remember(request);
            context.batch.add(request);
        }
    }

    @When("the batch finishes processing, including any retries")
    public void the_batch_finishes_processing_including_any_retries() {
        for (TransactionRequest request : context.batch) {
            context.processor.process(request);
        }
    }

    @Then("requestId {string} has final status {string}")
    public void requestId_has_final_status(String requestId, String expectedStatus) {
        assertEquals(TransactionStatus.valueOf(expectedStatus), context.requestsByRequestId.get(requestId).getStatus());
    }

    @And("the processing summary is generated")
    public void the_processing_summary_is_generated() {
        context.lastSummary = ProcessingSummary.of(context.batch);
    }

    @Then("the summary counts exactly {int} PROCESSED, {int} DUPLICATE, and {int} FAILED")
    public void the_summary_counts_exactly(int processed, int duplicate, int failed) {
        assertEquals(processed, context.lastSummary.countOf(TransactionStatus.PROCESSED));
        assertEquals(duplicate, context.lastSummary.countOf(TransactionStatus.DUPLICATE));
        assertEquals(failed, context.lastSummary.countOf(TransactionStatus.FAILED));
    }

    @Then("no count is missing or double-counted")
    public void no_count_is_missing_or_double_counted() {
        int total = 0;
        for (TransactionStatus status : TransactionStatus.values()) {
            total += context.lastSummary.countOf(status);
        }
        assertEquals(context.batch.size(), total);
    }
}
