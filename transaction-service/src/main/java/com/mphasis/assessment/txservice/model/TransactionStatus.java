package com.mphasis.assessment.txservice.model;

/**
 * PENDING_ORDER is an addition beyond the lifecycle named in L2-FR-04 ("such as ..."),
 * used to park requests that arrive ahead of their expected sequence.
 */
public enum TransactionStatus {
    RECEIVED,
    PROCESSING,
    PENDING_ORDER,
    RETRY_PENDING,
    PROCESSED,
    DUPLICATE,
    FAILED
}
