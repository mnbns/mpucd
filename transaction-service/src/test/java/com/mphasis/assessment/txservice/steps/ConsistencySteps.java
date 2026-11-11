package com.mphasis.assessment.txservice.steps;

import com.mphasis.assessment.txservice.model.TransactionStatus;
import com.mphasis.assessment.txservice.model.TransactionType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** US-07: Data Consistency. */
public class ConsistencySteps {

    private final TestContext context;
    private Thread first;
    private Thread second;

    public ConsistencySteps(TestContext context) {
        this.context = context;
    }

    @Given("a transaction being processed")
    public void a_transaction_being_processed() {
        context.buildRequest("TXN-CONSIST", "REQ-CONSIST", "ACC-CONSIST", TransactionType.CREDIT, new BigDecimal("40"), 1, 99);
    }

    @When("a failure occurs mid-step")
    public void a_failure_occurs_mid_step() {
        context.submit(context.currentRequest); // retries exhausted -> FAILED, never reaches the ledger
    }

    @Then("the visible state is either fully PROCESSED or fully FAILED or RETRY_PENDING")
    public void the_visible_state_is_a_defined_terminal_status() {
        assertEquals(TransactionStatus.FAILED, context.currentRequest.getStatus());
    }

    @Then("it is never a mix of updated-but-unmarked state")
    public void it_is_never_a_mix_of_updated_but_unmarked_state() {
        assertEquals(BigDecimal.ZERO, context.ledger.getBalance("ACC-CONSIST"));
    }

    @Given("concurrent requests for the same accountId")
    public void concurrent_requests_for_the_same_accountId() throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        first = new Thread(() -> {
            await(start);
            context.ledger.apply("ACC-CONCURRENT", TransactionType.CREDIT, new BigDecimal("100"));
        });
        second = new Thread(() -> {
            await(start);
            context.ledger.apply("ACC-CONCURRENT", TransactionType.CREDIT, new BigDecimal("50"));
        });
        first.start();
        second.start();
        start.countDown();
    }

    @When("both are processed")
    public void both_are_processed() throws InterruptedException {
        first.join();
        second.join();
    }

    @Then("updates are serialized per account to avoid lost updates")
    public void updates_are_serialized_per_account_to_avoid_lost_updates() {
        assertEquals(new BigDecimal("150"), context.ledger.getBalance("ACC-CONCURRENT"));
    }

    private void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(2, java.util.concurrent.TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
