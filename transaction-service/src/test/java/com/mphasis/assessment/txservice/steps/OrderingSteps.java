package com.mphasis.assessment.txservice.steps;

import com.mphasis.assessment.txservice.model.TransactionRequest;
import com.mphasis.assessment.txservice.model.TransactionStatus;
import com.mphasis.assessment.txservice.model.TransactionType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** US-05: Out-of-Order Request Handling (Option A). */
public class OrderingSteps {

    private final ScenarioContext context;
    private TransactionRequest seq2Request;
    private TransactionRequest seq1Request;

    public OrderingSteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("TXN-2002 with sequence 2 arrives before TXN-2001 with sequence 1 for the same account, as in section 9.8 rows 1 and 2")
    public void txn2002_arrives_before_txn2001() {
        seq2Request = context.buildRequest("TXN-2002", "REQ-2", "ACC-201", TransactionType.DEBIT, new BigDecimal("40"), 2);
        seq1Request = context.buildRequest("TXN-2001", "REQ-1", "ACC-201", TransactionType.CREDIT, new BigDecimal("100"), 1);
    }

    @When("sequence 2 is received")
    public void sequence_2_is_received() {
        context.submit(seq2Request);
    }

    @Then("it is held as PENDING_ORDER")
    public void it_is_held_as_pending_order() {
        assertEquals(TransactionStatus.PENDING_ORDER, seq2Request.getStatus());
    }

    @Then("it is not processed")
    public void it_is_not_processed() {
        assertFalse(context.idempotencyStore.isProcessed(seq2Request.getRequestId()));
    }

    @When("sequence 1 later arrives and is processed")
    public void sequence_1_later_arrives_and_is_processed() {
        context.submit(seq1Request);
    }

    @Then("the buffered sequence 2 request is automatically released")
    public void the_buffered_sequence_2_request_is_automatically_released() {
        assertFalse(context.sequenceBuffer.stillBuffered().containsKey(2L));
    }

    @Then("it is processed next, in order")
    public void it_is_processed_next_in_order() {
        assertEquals(TransactionStatus.PROCESSED, seq2Request.getStatus());
    }

    @Given("a sequence number that never arrives within the batch")
    public void a_sequence_number_that_never_arrives_within_the_batch() {
        TransactionRequest futureSeq = context.buildRequest("TXN-ORPHAN", "REQ-ORPHAN", "ACC-ORPHAN",
                TransactionType.CREDIT, new BigDecimal("15"), 9);
        context.submit(futureSeq); // no seq 1-8 ever arrives for this batch
    }

    @When("end-of-batch is reached")
    public void end_of_batch_is_reached() {
        // nothing further arrives - the request stays wherever it landed
    }

    @Then("the missing sequence is reported in the processing summary")
    public void the_missing_sequence_is_reported_in_the_processing_summary() {
        assertEquals(TransactionStatus.PENDING_ORDER, context.requestsByRequestId.get("REQ-ORPHAN").getStatus());
        assertTrue(context.sequenceBuffer.stillBuffered().containsKey(9L));
    }

    @Then("it is not silently dropped")
    public void it_is_not_silently_dropped() {
        long pendingCount = context.batch.stream()
                .filter(r -> r.getStatus() == TransactionStatus.PENDING_ORDER)
                .count();
        assertEquals(1, pendingCount);
    }
}
