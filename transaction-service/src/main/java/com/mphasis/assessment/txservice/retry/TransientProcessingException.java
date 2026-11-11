package com.mphasis.assessment.txservice.retry;

/** L2-FR-08: thrown to simulate a recoverable, transient processing failure. */
public class TransientProcessingException extends RuntimeException {
    public TransientProcessingException(String message) {
        super(message);
    }
}
