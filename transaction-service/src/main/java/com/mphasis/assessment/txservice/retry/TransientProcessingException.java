package com.mphasis.assessment.txservice.retry;

import com.mphasis.assessment.txservice.exception.TransactionServiceException;

/** L2-FR-08: thrown to simulate a recoverable, transient processing failure. */
public class TransientProcessingException extends TransactionServiceException {
    public TransientProcessingException(String message) {
        super(message);
    }
}
