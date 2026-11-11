package com.mphasis.assessment.txservice.steps;

import com.mphasis.assessment.txservice.model.TransactionRequest;
import com.mphasis.assessment.txservice.model.TransactionStatus;
import com.mphasis.assessment.txservice.model.TransactionType;
import com.mphasis.assessment.txservice.service.ProcessingSummary;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** end_to_end_batch.feature: replays the full §9.8 sample batch as one stateful run. */
public class EndToEndSteps {

    private final TestContext context;

    public EndToEndSteps(TestContext context) {
        this.context = context;
    }

    @Given("the following transaction requests are submitted in this exact order:")
    public void the_following_transaction_requests_are_submitted_in_this_exact_order(DataTable table) {
        for (Map<String, String> row : table.asMaps(String.class, String.class)) {
            TransactionRequest request = context.buildRequest(
                    row.get("transactionId"), row.get("requestId"), row.get("accountId"),
                    TransactionType.valueOf(row.get("type")), new BigDecimal(row.get("amount")),
                    Long.parseLong(row.get("sequence")));
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
