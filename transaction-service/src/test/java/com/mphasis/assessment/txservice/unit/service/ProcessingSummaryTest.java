package com.mphasis.assessment.txservice.unit.service;

import com.mphasis.assessment.txservice.model.TransactionRequest;
import com.mphasis.assessment.txservice.model.TransactionStatus;
import com.mphasis.assessment.txservice.model.TransactionType;
import com.mphasis.assessment.txservice.service.ProcessingSummary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for {@link ProcessingSummary}: tallies by TERMINAL status only, computed once over the batch. */
class ProcessingSummaryTest {

    private TransactionRequest requestWithStatus(String requestId, TransactionStatus status) {
        TransactionRequest request = new TransactionRequest("TXN-" + requestId, requestId, "ACC-1",
                TransactionType.CREDIT, new BigDecimal("1"), "USD", 1);
        // Drive to PROCESSING first for statuses only reachable that way, then to the target.
        if (status != TransactionStatus.RECEIVED) {
            if (status == TransactionStatus.DUPLICATE || status == TransactionStatus.PENDING_ORDER) {
                request.setStatus(status);
            } else {
                request.setStatus(TransactionStatus.PROCESSING);
                request.setStatus(status);
            }
        }
        return request;
    }

    @Test
    void countsEachStatusAcrossTheBatch() {
        List<TransactionRequest> batch = List.of(
                requestWithStatus("R1", TransactionStatus.PROCESSED),
                requestWithStatus("R2", TransactionStatus.PROCESSED),
                requestWithStatus("R3", TransactionStatus.FAILED),
                requestWithStatus("R4", TransactionStatus.DUPLICATE));

        ProcessingSummary summary = ProcessingSummary.of(batch);

        assertEquals(2, summary.countOf(TransactionStatus.PROCESSED));
        assertEquals(1, summary.countOf(TransactionStatus.FAILED));
        assertEquals(1, summary.countOf(TransactionStatus.DUPLICATE));
        assertEquals(0, summary.countOf(TransactionStatus.RECEIVED));
        assertEquals(0, summary.countOf(TransactionStatus.PENDING_ORDER));
    }

    @Test
    void countOfDefaultsToZeroForAnUnseenStatus() {
        ProcessingSummary summary = ProcessingSummary.of(List.of());
        for (TransactionStatus status : TransactionStatus.values()) {
            assertEquals(0, summary.countOf(status));
        }
    }

    @Test
    void reportListsEveryStatusWithItsCount() {
        ProcessingSummary summary = ProcessingSummary.of(List.of(requestWithStatus("R1", TransactionStatus.PROCESSED)));
        String report = summary.report();
        assertTrue(report.contains("PROCESSED=1"));
        assertTrue(report.contains("FAILED=0"));
        assertTrue(report.startsWith("SUMMARY:"));
    }
}
