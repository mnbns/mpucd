package com.mphasis.assessment.txservice.service;

import com.mphasis.assessment.txservice.model.TransactionRequest;
import com.mphasis.assessment.txservice.model.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(ProcessingSummary.class);

    private final Map<TransactionStatus, Integer> countsByStatus = new EnumMap<>(TransactionStatus.class);

    /**
     * Gap found comparing the real cucumber-tests.log / transaction-service.log
     * against AC-08.1/NFR-05: the summary was only ever asserted in-memory by
     * tests and shown in the Extent HTML (via Hooks) - it never reached any
     * persisted or machine-readable artifact, because the only place it was
     * printed was a raw System.out.println that bypasses SLF4J entirely.
     * Logging it here, once, at the point every call site already computes
     * it, closes that gap for every caller (the app's CommandLineRunner and
     * every test that generates a summary) without touching each call site.
     */
    public static ProcessingSummary of(List<TransactionRequest> requests) {
        ProcessingSummary summary = new ProcessingSummary();
        for (TransactionRequest request : requests) {
            summary.countsByStatus.merge(request.getStatus(), 1, Integer::sum);
        }
        log.info(summary.report());
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
