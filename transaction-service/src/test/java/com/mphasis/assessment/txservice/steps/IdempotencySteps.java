package com.mphasis.assessment.txservice.steps;

import com.mphasis.assessment.txservice.model.TransactionRequest;
import com.mphasis.assessment.txservice.model.TransactionStatus;
import com.mphasis.assessment.txservice.model.TransactionType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** US-04: Idempotent Processing and Duplicate Detection. */
public class IdempotencySteps {

    private final ScenarioContext context;
    private BigDecimal balanceAfterFirstSubmission;

    public IdempotencySteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("a requestId already marked PROCESSED")
    public void a_requestId_already_marked_processed() {
        TransactionRequest first = context.buildRequest("TXN-DUP-1", "REQ-DUP-1", "ACC-DUP", TransactionType.CREDIT, new BigDecimal("50"), 1);
        context.submit(first);
        assertEquals(TransactionStatus.PROCESSED, first.getStatus());
        balanceAfterFirstSubmission = context.ledger.getBalance("ACC-DUP");
    }

    @When("the same requestId is submitted again")
    public void the_same_requestId_is_submitted_again() {
        TransactionRequest replay = context.buildRequest("TXN-DUP-1", "REQ-DUP-1", "ACC-DUP", TransactionType.CREDIT, new BigDecimal("50"), 1);
        context.submit(replay);
    }

    @Then("it is marked DUPLICATE")
    public void it_is_marked_duplicate() {
        assertEquals(TransactionStatus.DUPLICATE, context.currentRequest.getStatus());
    }

    @Then("no balance or ledger side effect occurs")
    public void no_balance_or_ledger_side_effect_occurs() {
        assertEquals(balanceAfterFirstSubmission, context.ledger.getBalance("ACC-DUP"));
    }

    @Given("REQ-2 and REQ-2-DUP both referencing TXN-2002, as in section 9.8 rows 1 and 3")
    public void req2_and_req2_dup_both_referencing_txn_2002() {
        context.buildRequest("TXN-2001", "REQ-1", "ACC-201", TransactionType.CREDIT, new BigDecimal("100"), 1);
        context.buildRequest("TXN-2002", "REQ-2", "ACC-201", TransactionType.DEBIT, new BigDecimal("40"), 2);
        context.buildRequest("TXN-2002", "REQ-2-DUP", "ACC-201", TransactionType.DEBIT, new BigDecimal("40"), 2);
    }

    @When("REQ-2 is processed first")
    public void req2_is_processed_first() {
        context.submit(context.requestsByRequestId.get("REQ-2")); // buffered: seq=2 before seq=1 has arrived
        context.submit(context.requestsByRequestId.get("REQ-1")); // releases REQ-2
        assertEquals(TransactionStatus.PROCESSED, context.requestsByRequestId.get("REQ-2").getStatus());
    }

    @Then("REQ-2-DUP is detected as DUPLICATE even though it arrives later in the batch")
    public void req2_dup_is_detected_as_duplicate() {
        context.submit(context.requestsByRequestId.get("REQ-2-DUP"));
        assertEquals(TransactionStatus.DUPLICATE, context.requestsByRequestId.get("REQ-2-DUP").getStatus());
    }

    @Given("the idempotency store keyed by requestId")
    public void the_idempotency_store_keyed_by_requestId() {
        // exercised directly below by submitting the same requestId several times
    }

    @When("the same request is submitted multiple times")
    public void the_same_request_is_submitted_multiple_times() {
        for (int i = 0; i < 3; i++) {
            TransactionRequest attempt = context.buildRequest("TXN-REPEAT", "REQ-REPEAT", "ACC-REPEAT",
                    TransactionType.CREDIT, new BigDecimal("30"), 1);
            context.submit(attempt);
        }
    }

    @Then("the final ledger effect is identical to processing it exactly once")
    public void the_final_ledger_effect_is_identical_to_processing_it_once() {
        assertEquals(new BigDecimal("30"), context.ledger.getBalance("ACC-REPEAT"));
    }
}
