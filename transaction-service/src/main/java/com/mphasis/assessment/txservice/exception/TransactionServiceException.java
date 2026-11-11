package com.mphasis.assessment.txservice.exception;

/**
 * Root of this service's exception hierarchy. Unchecked (extends
 * RuntimeException) so the pipeline's method signatures stay clean -
 * callers that need to react to a specific failure catch the concrete
 * subtype; everything else propagates and fails the batch loudly rather
 * than silently continuing on bad state.
 */
public abstract class TransactionServiceException extends RuntimeException {

    protected TransactionServiceException(String message) {
        super(message);
    }

    protected TransactionServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
