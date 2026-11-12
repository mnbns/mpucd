package com.mphasis.assessment.txservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mphasis.assessment.txservice.config.TransactionServiceProperties;
import com.mphasis.assessment.txservice.input.InMemoryTransactionSource;
import com.mphasis.assessment.txservice.input.TransactionSource;
import com.mphasis.assessment.txservice.model.TransactionRequest;
import com.mphasis.assessment.txservice.ordering.SequenceBuffer;
import com.mphasis.assessment.txservice.retry.RetryPolicy;
import com.mphasis.assessment.txservice.service.ProcessingSummary;
import com.mphasis.assessment.txservice.service.TransactionProcessor;
import com.mphasis.assessment.txservice.service.Validator;
import com.mphasis.assessment.txservice.store.AccountLedger;
import com.mphasis.assessment.txservice.store.IdempotencyStore;
import com.mphasis.assessment.txservice.store.InMemoryIdempotencyStore;
import com.mphasis.assessment.txservice.worker.BackgroundWorker;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * DD-01: plain console run, no web layer. Loads the §9.8 sample batch,
 * processes it through the engine in order, then prints the final
 * per-status summary (US-08).
 */
@SpringBootApplication
@EnableConfigurationProperties(TransactionServiceProperties.class)
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }

    /**
     * DD-05 / L2-FR-10: the input resource name, retry ceiling, and worker
     * thread name are bound from {@code application.properties} via
     * {@link TransactionServiceProperties} - none of them is a literal
     * here, so changing behavior never means editing this class.
     */
    @Bean
    public CommandLineRunner runBatch(TransactionServiceProperties properties) {
        return args -> {
            TransactionSource source = new InMemoryTransactionSource(
                    properties.getInput().getResourcePath(), new ObjectMapper());
            IdempotencyStore idempotencyStore = new InMemoryIdempotencyStore();
            SequenceBuffer sequenceBuffer = new SequenceBuffer();
            RetryPolicy retryPolicy = new RetryPolicy(properties.getRetry().getMaxAttempts());
            AccountLedger ledger = new AccountLedger();
            TransactionProcessor processor = new TransactionProcessor(
                    new Validator(), idempotencyStore, sequenceBuffer, retryPolicy, ledger);

            // US-12 / L2-FR-13 / DD-12: ingestion (producer) and processing (single
            // consumer worker) are separate roles now, not one inline call stack -
            // the ingesting thread enqueues the whole batch and returns immediately,
            // then waits once for the worker to signal every request has drained.
            BackgroundWorker worker = new BackgroundWorker(processor, properties.getWorker().getThreadName());
            worker.start();

            List<TransactionRequest> batch = source.fetchBatch();
            for (TransactionRequest request : batch) {
                worker.submit(request);
            }
            worker.awaitDrained();

            System.out.println("---");
            System.out.println(ProcessingSummary.of(batch).report());

            System.out.println("---");
            Set<String> accountIds = new LinkedHashSet<>();
            for (TransactionRequest request : batch) {
                accountIds.add(request.getAccountId());
            }
            for (String accountId : accountIds) {
                System.out.println("BALANCE " + accountId + " = " + ledger.getBalance(accountId));
            }
        };
    }
}
