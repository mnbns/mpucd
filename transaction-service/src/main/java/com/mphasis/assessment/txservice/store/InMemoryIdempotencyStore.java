package com.mphasis.assessment.txservice.store;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final Set<String> processedRequestIds = ConcurrentHashMap.newKeySet();
    private final Set<String> processedTransactionIds = ConcurrentHashMap.newKeySet();

    @Override
    public boolean isProcessed(String requestId) {
        return processedRequestIds.contains(requestId);
    }

    @Override
    public void markProcessed(String requestId) {
        processedRequestIds.add(requestId);
    }

    @Override
    public boolean isTransactionProcessed(String transactionId) {
        return processedTransactionIds.contains(transactionId);
    }

    @Override
    public void markTransactionProcessed(String transactionId) {
        processedTransactionIds.add(transactionId);
    }
}
