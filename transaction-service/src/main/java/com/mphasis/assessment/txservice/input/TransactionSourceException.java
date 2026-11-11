package com.mphasis.assessment.txservice.input;

import com.mphasis.assessment.txservice.exception.TransactionServiceException;

/** US-01: raised when a {@link TransactionSource} cannot produce a batch - a missing or unreadable input. */
public class TransactionSourceException extends TransactionServiceException {

    private TransactionSourceException(String message) {
        super(message);
    }

    private TransactionSourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public static TransactionSourceException resourceNotFound(String resourcePath) {
        return new TransactionSourceException("Sample data resource not found on classpath: " + resourcePath);
    }

    public static TransactionSourceException readFailed(String resourcePath, Throwable cause) {
        return new TransactionSourceException("Failed to read " + resourcePath, cause);
    }

    public static TransactionSourceException malformedBatch(String resourcePath, String reason) {
        return new TransactionSourceException("Malformed batch in " + resourcePath + ": " + reason);
    }
}
