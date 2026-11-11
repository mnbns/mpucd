package com.mphasis.assessment.txservice.service;

import com.mphasis.assessment.txservice.model.TransactionRequest;
import com.mphasis.assessment.txservice.model.TransactionStatus;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * US-08 / L2-FR-11: a final tally by status, computed once over the whole
 * batch rather than incrementally during processing - so transient states
 * (PROCESSING, RETRY_PENDING mid-retry) never pollute the count, only each
 * request's terminal status does.
 */
public class ProcessingSummary {

    private final Map<TransactionStatus, Integer> countsByStatus = new EnumMap<>(TransactionStatus.class);

    public static ProcessingSummary of(List<TransactionRequest> requests) {
        ProcessingSummary summary = new ProcessingSummary();
        for (TransactionRequest request : requests) {
            summary.countsByStatus.merge(request.getStatus(), 1, Integer::sum);
        }
        return summary;
    }

    public int countOf(TransactionStatus status) {
        return countsByStatus.getOrDefault(status, 0);
    }

    public String report() {
        StringBuilder sb = new StringBuilder("SUMMARY: ");
        for (TransactionStatus status : TransactionStatus.values()) {
            sb.append(status).append('=').append(countOf(status)).append("  ");
        }
        return sb.toString().trim();
    }
}
