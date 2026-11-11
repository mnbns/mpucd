package com.mphasis.assessment.txservice.steps;

import com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter;
import com.mphasis.assessment.txservice.model.TransactionRequest;
import com.mphasis.assessment.txservice.model.TransactionStatus;
import com.mphasis.assessment.txservice.service.ProcessingSummary;
import com.mphasis.assessment.txservice.service.ValidationResult;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeStep;
import io.cucumber.java.Scenario;

import java.nio.charset.StandardCharsets;

/**
 * Two separate report destinations need two separate calls:
 * - {@code ExtentCucumberAdapter.addTestStepLog(String)} appends raw HTML
 *   into the current step's log node in the Extent HTML report. (Plain
 *   {@code Scenario#attach} with "text/html" is NOT rendered by this
 *   adapter - tech.grasshopper's ExtentCucumberAdapter only auto-renders
 *   "image/*" attachments as embedded screenshots; confirmed via javap -
 *   there is no text/html branch in its embed handler.)
 * - {@code Scenario#attach} is still needed separately for cucumber.json:
 *   Cucumber's own built-in JSON formatter records every {@code attach}
 *   call as a base64 "embeddings" entry on that step, independent of
 *   whatever the Extent adapter does with it. Without this call,
 *   cucumber.json carried zero business-outcome data (confirmed: 0
 *   embeddings across all 104 steps) - pass/fail/tags/timing only.
 * Both calls build the same HTML snapshot of ScenarioContext's live request,
 * response, and summary state after every step. Read-only: never affects
 * pass/fail, only what the two reports display.
 */
public class Hooks {

    private final ScenarioContext context;

    public Hooks(ScenarioContext context) {
        this.context = context;
    }

    /**
     * Resolves this scenario's {@link FeatureContext} from its URI (Cucumber
     * fires no hook at the feature boundary itself, so this is how that
     * boundary gets synthesized) and wires {@link ScenarioContext#feature}
     * to it before any step runs.
     */
    @Before
    public void resolveFeature(Scenario scenario) {
        String uri = scenario.getUri().toString();
        context.feature = TestContext.instance().featureFor(uri, featureNameFrom(uri));
        context.feature.recordScenarioStarted();
    }

    @BeforeStep
    public void startStep() {
        context.step.startNext();
    }

    @AfterStep
    public void attachStepSnapshot(Scenario scenario) {
        String html = buildHtml();
        if (html != null) {
            ExtentCucumberAdapter.addTestStepLog(html);
            scenario.attach(html.getBytes(StandardCharsets.UTF_8), "text/html", "Step Snapshot");
        }
    }

    private String featureNameFrom(String uri) {
        String fileName = uri.substring(uri.lastIndexOf('/') + 1);
        return fileName.endsWith(".feature") ? fileName.substring(0, fileName.length() - ".feature".length()) : fileName;
    }

    private String buildHtml() {
        TransactionRequest current = context.currentRequest;
        if (current == null && context.lastSummary == null) {
            return null;
        }

        StringBuilder html = new StringBuilder("<div style=\"font-family:Consolas,monospace;font-size:12px\">");

        if (current != null) {
            html.append(heading("Request / Response"));
            html.append(openTable());
            appendRow(html, "transactionId", current.getTransactionId());
            appendRow(html, "requestId", current.getRequestId());
            appendRow(html, "accountId", mask(current.getAccountId()));
            appendRow(html, "type", String.valueOf(current.getTransactionType()));
            appendRow(html, "amount", String.valueOf(current.getAmount()));
            appendRow(html, "sequence", String.valueOf(current.getSequenceNumber()));
            appendRow(html, "status", String.valueOf(current.getStatus()));
            appendRow(html, "retryCount", String.valueOf(current.getRetryCount()));
            appendRow(html, "failureReason", current.getFailureReason() == null ? "-" : current.getFailureReason());
            html.append(closeTable());
        }

        if (context.lastValidationResult != null) {
            ValidationResult validation = context.lastValidationResult;
            html.append(heading("Validation Result"));
            html.append(openTable());
            appendRow(html, "valid", String.valueOf(validation.isValid()));
            appendRow(html, "reason", validation.getReason() == null ? "-" : validation.getReason());
            html.append(closeTable());
        }

        if (context.batch.size() > 1) {
            html.append(heading("Batch So Far (" + context.batch.size() + " request(s))"));
            html.append("<table border=\"1\" cellpadding=\"4\" cellspacing=\"0\">");
            html.append("<tr><th>requestId</th><th>transactionId</th><th>seq</th><th>status</th></tr>");
            for (TransactionRequest request : context.batch) {
                html.append("<tr><td>").append(escape(request.getRequestId()))
                        .append("</td><td>").append(escape(request.getTransactionId()))
                        .append("</td><td>").append(request.getSequenceNumber())
                        .append("</td><td>").append(request.getStatus()).append("</td></tr>");
            }
            html.append("</table>");
        }

        if (context.lastSummary != null) {
            ProcessingSummary summary = context.lastSummary;
            html.append(heading("Processing Summary"));
            html.append("<table border=\"1\" cellpadding=\"4\" cellspacing=\"0\">");
            html.append("<tr><th>Status</th><th>Count</th></tr>");
            for (TransactionStatus status : TransactionStatus.values()) {
                html.append("<tr><td>").append(status).append("</td><td>")
                        .append(summary.countOf(status)).append("</td></tr>");
            }
            html.append("</table>");
        }

        html.append("</div>");
        return html.toString();
    }

    private String heading(String text) {
        return "<h4 style=\"margin:6px 0 2px\">" + escape(text) + "</h4>";
    }

    private String openTable() {
        return "<table border=\"1\" cellpadding=\"4\" cellspacing=\"0\">";
    }

    private String closeTable() {
        return "</table>";
    }

    private void appendRow(StringBuilder html, String field, String value) {
        html.append("<tr><td><b>").append(escape(field)).append("</b></td><td>")
                .append(escape(value)).append("</td></tr>");
    }

    private String escape(String value) {
        if (value == null) {
            return "-";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Same masking rule as TransactionProcessor (US-09/NFR-06) - the report must not leak full account IDs either. */
    private String mask(String accountId) {
        if (accountId == null || accountId.length() <= 4) {
            return "***";
        }
        return "***" + accountId.substring(accountId.length() - 3);
    }
}
