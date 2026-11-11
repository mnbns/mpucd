package com.mphasis.assessment.txservice.schema;

import com.mphasis.assessment.txservice.exception.TransactionServiceException;

/** Thrown by {@link RequestSchemaValidator#validateOrThrow} for a record that does not match the exact request schema. */
public class SchemaValidationException extends TransactionServiceException {

    public SchemaValidationException(String message) {
        super(message);
    }
}
