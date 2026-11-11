package com.mphasis.assessment.txservice.steps;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mphasis.assessment.txservice.model.TransactionType;
import com.mphasis.assessment.txservice.service.ProcessingSummary;
import com.mphasis.assessment.txservice.service.TransactionProcessor;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** US-08: Observability and Processing Summary. */
public class ObservabilitySteps {

    private final TestContext context;
    private ListAppender<ILoggingEvent> appender;

    public ObservabilitySteps(TestContext context) {
        this.context = context;
    }

    @Given("a batch run has completed")
    public void a_batch_run_has_completed() {
        context.submit(context.buildRequest("TXN-OBS-1", "REQ-OBS-1", "ACC-OBS-1", TransactionType.CREDIT, new BigDecimal("10"), 1));
        context.submit(context.buildRequest("TXN-OBS-2", "REQ-OBS-2", "ACC-OBS-2", TransactionType.DEBIT, new BigDecimal("-5"), 1));
        context.submit(context.buildRequest("TXN-OBS-1", "REQ-OBS-1", "ACC-OBS-1", TransactionType.CREDIT, new BigDecimal("10"), 1));
    }

    @When("the summary is generated")
    public void the_summary_is_generated() {
        context.lastSummary = ProcessingSummary.of(context.batch);
    }

    @Then("it reports counts by status for RECEIVED, PROCESSED, DUPLICATE, FAILED, RETRY_PENDING, and PENDING_ORDER")
    public void it_reports_counts_by_status() {
        String report = context.lastSummary.report();
        for (String status : new String[] {"RECEIVED", "PROCESSED", "DUPLICATE", "FAILED", "RETRY_PENDING", "PENDING_ORDER"}) {
            assertTrue(report.contains(status), "expected summary to mention " + status + ": " + report);
        }
    }

    @Given("any transaction event such as received, duplicate, retried, or failed")
    public void any_transaction_event() {
        appender = new ListAppender<>();
        appender.start();
        ((Logger) LoggerFactory.getLogger(TransactionProcessor.class)).addAppender(appender);
    }

    @When("it occurs")
    public void it_occurs() {
        context.submit(context.buildRequest("TXN-LOG", "REQ-LOG", "ACC-LOG-FULL", TransactionType.CREDIT, new BigDecimal("10"), 1));
    }

    @Then("a structured log line is emitted with transactionId, requestId, status, and timestamp")
    public void a_structured_log_line_is_emitted() {
        List<ILoggingEvent> events = appender.list;
        assertFalse(events.isEmpty());
        String message = events.get(0).getFormattedMessage();
        assertTrue(message.contains("transactionId=TXN-LOG"));
        assertTrue(message.contains("requestId=REQ-LOG"));
        assertTrue(message.contains("->"));
        assertTrue(events.get(0).getTimeStamp() > 0);
    }

    @Then("it contains no sensitive fields")
    public void it_contains_no_sensitive_fields() {
        for (ILoggingEvent event : appender.list) {
            assertFalse(event.getFormattedMessage().contains("ACC-LOG-FULL"),
                    "log line must never contain the unmasked accountId");
        }
        ((Logger) LoggerFactory.getLogger(TransactionProcessor.class)).detachAppender(appender);
    }
}
