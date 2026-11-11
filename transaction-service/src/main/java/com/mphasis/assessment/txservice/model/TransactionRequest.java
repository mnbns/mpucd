package com.mphasis.assessment.txservice.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Minimum transaction request model per §5 of the problem statement.
 * Mutable status/retryCount/failureReason are updated by TransactionProcessor
 * during the pipeline; the model itself carries no business rules.
 * Deserializes directly from sample-transactions.json via the constructor
 * below, so no separate DTO class is needed.
 */
public class TransactionRequest {

    private static final Logger log = LoggerFactory.getLogger(TransactionRequest.class);

    /** US-03 / AC-03.4: the enforced lifecycle - every status always allows staying put; otherwise only forward moves listed here. */
    private static final Map<TransactionStatus, Set<TransactionStatus>> ALLOWED_TRANSITIONS = buildAllowedTransitions();

    private static Map<TransactionStatus, Set<TransactionStatus>> buildAllowedTransitions() {
        Map<TransactionStatus, Set<TransactionStatus>> transitions = new EnumMap<>(TransactionStatus.class);
        transitions.put(TransactionStatus.RECEIVED, EnumSet.of(
                TransactionStatus.RECEIVED, TransactionStatus.PROCESSING, TransactionStatus.PENDING_ORDER,
                TransactionStatus.DUPLICATE, TransactionStatus.FAILED));
        transitions.put(TransactionStatus.PROCESSING, EnumSet.of(
                TransactionStatus.PROCESSING, TransactionStatus.PROCESSED, TransactionStatus.RETRY_PENDING, TransactionStatus.FAILED));
        transitions.put(TransactionStatus.RETRY_PENDING, EnumSet.of(
                TransactionStatus.RETRY_PENDING, TransactionStatus.PROCESSING));
        transitions.put(TransactionStatus.PENDING_ORDER, EnumSet.of(
                TransactionStatus.PENDING_ORDER, TransactionStatus.PROCESSING));
        transitions.put(TransactionStatus.PROCESSED, EnumSet.of(TransactionStatus.PROCESSED));
        transitions.put(TransactionStatus.DUPLICATE, EnumSet.of(TransactionStatus.DUPLICATE));
        transitions.put(TransactionStatus.FAILED, EnumSet.of(TransactionStatus.FAILED));
        return transitions;
    }

    private final String transactionId;
    private final String requestId;
    private final String accountId;
    private final TransactionType transactionType;
    private final BigDecimal amount;
    private final String currency;
    private final long sequenceNumber;

    /** Number of leading attempts that should fail transiently before succeeding; 0 = never fails. */
    private final int simulatedFailures;

    private TransactionStatus status = TransactionStatus.RECEIVED;
    private int retryCount = 0;
    private String failureReason;

    @JsonCreator
    public TransactionRequest(@JsonProperty("transactionId") String transactionId,
                               @JsonProperty("requestId") String requestId,
                               @JsonProperty("accountId") String accountId,
                               @JsonProperty("transactionType") TransactionType transactionType,
                               @JsonProperty("amount") BigDecimal amount,
                               @JsonProperty("currency") String currency,
                               @JsonProperty("sequenceNumber") long sequenceNumber,
                               @JsonProperty("simulatedFailures") int simulatedFailures) {
        this.transactionId = transactionId;
        this.requestId = requestId;
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.currency = currency;
        this.sequenceNumber = sequenceNumber;
        this.simulatedFailures = simulatedFailures;
    }

    public TransactionRequest(String transactionId, String requestId, String accountId,
                               TransactionType transactionType, BigDecimal amount, String currency,
                               long sequenceNumber) {
        this(transactionId, requestId, accountId, transactionType, amount, currency, sequenceNumber, 0);
    }

    public String getTransactionId() { return transactionId; }
    public String getRequestId() { return requestId; }
    public String getAccountId() { return accountId; }
    public TransactionType getTransactionType() { return transactionType; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public long getSequenceNumber() { return sequenceNumber; }
    public int getSimulatedFailures() { return simulatedFailures; }

    public TransactionStatus getStatus() { return status; }

    public void setStatus(TransactionStatus newStatus) {
        if (!ALLOWED_TRANSITIONS.get(this.status).contains(newStatus)) {
            log.warn("Rejected illegal status transition for requestId={}: {} -> {}", requestId, this.status, newStatus);
            return;
        }
        this.status = newStatus;
    }

    public int getRetryCount() { return retryCount; }
    public void incrementRetryCount() { this.retryCount++; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionRequest)) return false;
        TransactionRequest that = (TransactionRequest) o;
        return Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId);
    }

    @Override
    public String toString() {
        return "TransactionRequest{" +
                "transactionId='" + transactionId + '\'' +
                ", requestId='" + requestId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", transactionType=" + transactionType +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", sequenceNumber=" + sequenceNumber +
                ", status=" + status +
                ", retryCount=" + retryCount +
                ", failureReason='" + failureReason + '\'' +
                '}';
    }
}
