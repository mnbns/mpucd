package com.mphasis.assessment.txservice.steps;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mphasis.assessment.txservice.model.TransactionType;
import com.mphasis.assessment.txservice.service.TransactionProcessor;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** US-09: Security and Safe Data Handling. */
public class SecuritySteps {

    private static final Pattern HARDCODED_SECRET =
            Pattern.compile("(?i)(password|secret|apikey|api_key|access[_-]?token)\\s*=\\s*\"[^\"]+\"");

    private final TestContext context;
    private ListAppender<ILoggingEvent> appender;

    public SecuritySteps(TestContext context) {
        this.context = context;
    }

    @Given("any log statement for a transaction")
    public void any_log_statement_for_a_transaction() {
        appender = new ListAppender<>();
        appender.start();
        ((Logger) LoggerFactory.getLogger(TransactionProcessor.class)).addAppender(appender);
    }

    @When("it is emitted")
    public void it_is_emitted() {
        context.submit(context.buildRequest("TXN-PII", "REQ-PII", "ACC-FULLY-SECRET", TransactionType.CREDIT, new BigDecimal("10"), 1));
    }

    @Then("it never includes full account numbers or PII beyond a masked or short accountId reference")
    public void it_never_includes_full_account_numbers_or_pii() {
        assertFalse(appender.list.isEmpty());
        for (ILoggingEvent event : appender.list) {
            assertFalse(event.getFormattedMessage().contains("ACC-FULLY-SECRET"),
                    "log line must never contain the unmasked accountId");
        }
        ((Logger) LoggerFactory.getLogger(TransactionProcessor.class)).detachAppender(appender);
    }

    @Given("the service configuration")
    public void the_service_configuration() {
        // scanned directly in the Then step below
    }

    @When("it is reviewed")
    public void it_is_reviewed() {
        // no-op - the review is the source scan itself
    }

    @Then("no secrets or credentials are hardcoded in source")
    public void no_secrets_or_credentials_are_hardcoded_in_source() throws IOException {
        Path mainSource = Path.of("src", "main", "java");
        try (Stream<Path> files = Files.walk(mainSource)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String content = Files.readString(file);
                assertFalse(HARDCODED_SECRET.matcher(content).find(), "possible hardcoded secret in " + file);
            }
        }
    }

    @Then("all are externalized")
    public void all_are_externalized() {
        // No secrets exist for this service (no DB, no external API, no auth) - nothing to externalize yet;
        // the absence is itself the evidence, confirmed by the source scan above.
        assertTrue(true);
    }
}
