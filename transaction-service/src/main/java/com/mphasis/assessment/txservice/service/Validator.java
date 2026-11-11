package com.mphasis.assessment.txservice.service;

import com.mphasis.assessment.txservice.model.TransactionRequest;

/** US-02 / L2-FR-02: rejects malformed records before they reach the idempotency/ordering/processing pipeline. */
public class Validator {

    public ValidationResult validate(TransactionRequest request) {
        if (isBlank(request.getTransactionId())) {
            return ValidationResult.fail("transactionId is required");
        }
        if (isBlank(request.getRequestId())) {
            return ValidationResult.fail("requestId is required");
        }
        if (isBlank(request.getAccountId())) {
            return ValidationResult.fail("accountId is required");
        }
        if (request.getTransactionType() == null) {
            return ValidationResult.fail("transactionType is required");
        }
        if (isBlank(request.getCurrency())) {
            return ValidationResult.fail("currency is required");
        }
        if (request.getSequenceNumber() <= 0) {
            return ValidationResult.fail("sequenceNumber must be positive");
        }
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            return ValidationResult.fail("amount must be a positive number");
        }
        return ValidationResult.ok();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
