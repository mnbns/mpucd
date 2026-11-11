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

/** US-04: Idempotent Processing and Duplicate Detection. */
public class IdempotencySteps {

    private final ScenarioContext context;
    private BigDecimal balanceAfterFirstSubmission;
    /** Which account the two generic "no side effect" Then steps below should check - varies per scenario. */
    private String accountIdUnderTest;
    private TransactionRequest staleReplay;

    public IdempotencySteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("a requestId already marked PROCESSED")
    public void a_requestId_already_marked_processed() {
        TransactionRequest first = context.buildRequest("TXN-DUP-1", "REQ-DUP-1", "ACC-DUP", TransactionType.CREDIT, new BigDecimal("50"), 1);
        context.submit(first);
        assertEquals(TransactionStatus.PROCESSED, first.getStatus());
        accountIdUnderTest = "ACC-DUP";
        balanceAfterFirstSubmission = context.ledger.getBalance(accountIdUnderTest);
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
        assertEquals(balanceAfterFirstSubmission, context.ledger.getBalance(accountIdUnderTest));
    }

    @Given("two requests referencing the same business transaction, as in section 9.8 rows 1 and 3, loaded from {string}")
    public void two_requests_referencing_the_same_business_transaction(String fixturePath) {
        for (TransactionRequest request : TestDataLoader.loadRequests(fixturePath)) {
            context.remember(request);
        }
    }

    @When("the first request is processed first")
    public void the_first_request_is_processed_first() {
        context.submit(context.requestsByRequestId.get("6820394")); // buffered: seq=2 before seq=1 has arrived
        context.submit(context.requestsByRequestId.get("4193857")); // releases the seq=2 request
        assertEquals(TransactionStatus.PROCESSED, context.requestsByRequestId.get("6820394").getStatus());
        // A second, independent duplicate-transactionId pair in the same batch (seq=3),
        // proving dedup isn't a one-shot fluke within a single run.
        context.submit(context.requestsByRequestId.get("3157924"));
        assertEquals(TransactionStatus.PROCESSED, context.requestsByRequestId.get("3157924").getStatus());
    }

    @Then("the duplicate request is detected as DUPLICATE even though it arrives later in the batch")
    public void the_duplicate_is_detected_as_duplicate() {
        context.submit(context.requestsByRequestId.get("9304857"));
        assertEquals(TransactionStatus.DUPLICATE, context.requestsByRequestId.get("9304857").getStatus());
        context.submit(context.requestsByRequestId.get("8642079"));
        assertEquals(TransactionStatus.DUPLICATE, context.requestsByRequestId.get("8642079").getStatus());
        // compareTo, not equals(): amounts round-trip through a JsonNode tree on
        // their way to BigDecimal, which drops the trailing zero (75.60 -> scale-1
        // 75.6) even though the value is unchanged - equals() is scale-sensitive,
        // compareTo() correctly treats 75.60 and 75.6 as the same number.
        assertEquals(0, new BigDecimal("75.60").compareTo(context.ledger.getBalance("5917264083")));
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

    // --- AC-04.4: a third, distinct dedup path (FC-03's stale-sequence branch),
    // separate from the requestId/transactionId checks above, since a stale replay
    // can carry a brand-new requestId AND transactionId. ---

    @Given("a batch where sequence 1 through 3 have already resolved, loaded from {string}")
    public void a_batch_where_sequence_1_through_3_have_already_resolved(String fixturePath) {
        List<TransactionRequest> requests = TestDataLoader.loadRequests(fixturePath);
        for (TransactionRequest request : requests) {
            if ("9047235".equals(request.getRequestId())) {
                staleReplay = request;
            } else {
                context.submit(request);
            }
        }
        accountIdUnderTest = "2890376145";
        balanceAfterFirstSubmission = context.ledger.getBalance(accountIdUnderTest);
    }

    @When("a brand-new request reuses an already-resolved sequence number")
    public void a_brand_new_request_reuses_an_already_resolved_sequence_number() {
        context.submit(staleReplay);
    }
}
