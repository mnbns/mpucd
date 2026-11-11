package com.mphasis.assessment.txservice.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryIdempotencyStore implements IdempotencyStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryIdempotencyStore.class);

    private final Set<String> processedRequestIds = ConcurrentHashMap.newKeySet();
    private final Set<String> processedTransactionIds = ConcurrentHashMap.newKeySet();

    @Override
    public boolean isProcessed(String requestId) {
        return processedRequestIds.contains(requestId);
    }

    @Override
    public void markProcessed(String requestId) {
        processedRequestIds.add(requestId);
        log.debug("idempotency store: requestId={} marked processed ({} total)", requestId, processedRequestIds.size());
    }

    @Override
    public boolean isTransactionProcessed(String transactionId) {
        return processedTransactionIds.contains(transactionId);
    }

    @Override
    public void markTransactionProcessed(String transactionId) {
        processedTransactionIds.add(transactionId);
        log.debug("idempotency store: transactionId={} marked processed ({} total)", transactionId, processedTransactionIds.size());
    }
}
