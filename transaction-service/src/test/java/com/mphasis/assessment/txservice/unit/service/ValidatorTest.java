package com.mphasis.assessment.txservice.unit.service;

import com.mphasis.assessment.txservice.model.TransactionRequest;
import com.mphasis.assessment.txservice.model.TransactionType;
import com.mphasis.assessment.txservice.service.ValidationResult;
import com.mphasis.assessment.txservice.service.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for {@link Validator}, the concrete implementation of the field-level business validation rules. */
class ValidatorTest {

    private final Validator validator = new Validator();

    private TransactionRequest validRequest() {
        return new TransactionRequest("TXN-1", "REQ-1", "ACC-1",
                TransactionType.CREDIT, new BigDecimal("10"), "USD", 1);
    }

    @Test
    void validRequestPasses() {
        ValidationResult result = validator.validate(validRequest());
        assertTrue(result.isValid());
        assertNull(result.getReason());
    }

    @Test
    void blankTransactionIdFails() {
        TransactionRequest request = new TransactionRequest("  ", "REQ-1", "ACC-1",
                TransactionType.CREDIT, new BigDecimal("10"), "USD", 1);
        ValidationResult result = validator.validate(request);
        assertFalseValid(result, "transactionId is required");
    }

    @Test
    void nullTransactionIdFails() {
        TransactionRequest request = new TransactionRequest(null, "REQ-1", "ACC-1",
                TransactionType.CREDIT, new BigDecimal("10"), "USD", 1);
        assertFalseValid(validator.validate(request), "transactionId is required");
    }

    @Test
    void blankRequestIdFails() {
        TransactionRequest request = new TransactionRequest("TXN-1", "", "ACC-1",
                TransactionType.CREDIT, new BigDecimal("10"), "USD", 1);
        assertFalseValid(validator.validate(request), "requestId is required");
    }

    @Test
    void blankAccountIdFails() {
        TransactionRequest request = new TransactionRequest("TXN-1", "REQ-1", null,
                TransactionType.CREDIT, new BigDecimal("10"), "USD", 1);
        assertFalseValid(validator.validate(request), "accountId is required");
    }

    @Test
    void nullTransactionTypeFails() {
        TransactionRequest request = new TransactionRequest("TXN-1", "REQ-1", "ACC-1",
                null, new BigDecimal("10"), "USD", 1);
        assertFalseValid(validator.validate(request), "transactionType is required");
    }

    @Test
    void blankCurrencyFails() {
        TransactionRequest request = new TransactionRequest("TXN-1", "REQ-1", "ACC-1",
                TransactionType.CREDIT, new BigDecimal("10"), " ", 1);
        assertFalseValid(validator.validate(request), "currency is required");
    }

    @Test
    void zeroOrNegativeSequenceNumberFails() {
        TransactionRequest zero = new TransactionRequest("TXN-1", "REQ-1", "ACC-1",
                TransactionType.CREDIT, new BigDecimal("10"), "USD", 0);
        assertFalseValid(validator.validate(zero), "sequenceNumber must be positive");

        TransactionRequest negative = new TransactionRequest("TXN-1", "REQ-1", "ACC-1",
                TransactionType.CREDIT, new BigDecimal("10"), "USD", -5);
        assertFalseValid(validator.validate(negative), "sequenceNumber must be positive");
    }

    @Test
    void nullAmountFails() {
        TransactionRequest request = new TransactionRequest("TXN-1", "REQ-1", "ACC-1",
                TransactionType.CREDIT, null, "USD", 1);
        assertFalseValid(validator.validate(request), "amount must be a positive number");
    }

    @Test
    void zeroOrNegativeAmountFails() {
        TransactionRequest zero = new TransactionRequest("TXN-1", "REQ-1", "ACC-1",
                TransactionType.CREDIT, BigDecimal.ZERO, "USD", 1);
        assertFalseValid(validator.validate(zero), "amount must be a positive number");

        TransactionRequest negative = new TransactionRequest("TXN-1", "REQ-1", "ACC-1",
                TransactionType.CREDIT, new BigDecimal("-1"), "USD", 1);
        assertFalseValid(validator.validate(negative), "amount must be a positive number");
    }

    private void assertFalseValid(ValidationResult result, String expectedReason) {
        assertTrue(!result.isValid());
        assertEquals(expectedReason, result.getReason());
    }
}
