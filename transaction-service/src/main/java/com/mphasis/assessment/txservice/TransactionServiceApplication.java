package com.mphasis.assessment.txservice;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner runBatch() {
        return args -> {
            TransactionSource source = new InMemoryTransactionSource("sample-transactions.json", new ObjectMapper());
            IdempotencyStore idempotencyStore = new InMemoryIdempotencyStore();
            SequenceBuffer sequenceBuffer = new SequenceBuffer();
            RetryPolicy retryPolicy = new RetryPolicy(3);
            AccountLedger ledger = new AccountLedger();
            TransactionProcessor processor = new TransactionProcessor(
                    new Validator(), idempotencyStore, sequenceBuffer, retryPolicy, ledger);

            List<TransactionRequest> batch = source.fetchBatch();
            for (TransactionRequest request : batch) {
                processor.process(request);
            }

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
