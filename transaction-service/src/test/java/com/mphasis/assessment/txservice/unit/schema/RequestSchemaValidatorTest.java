package com.mphasis.assessment.txservice.unit.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mphasis.assessment.txservice.schema.RequestSchemaValidator;
import com.mphasis.assessment.txservice.schema.SchemaValidationException;
import com.mphasis.assessment.txservice.schema.SchemaValidationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link RequestSchemaValidator}: the structural/shape gate that runs
 * before a raw JSON record is ever deserialized into a {@code TransactionRequest}.
 */
class RequestSchemaValidatorTest {

    private final RequestSchemaValidator validator = new RequestSchemaValidator();
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode json(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final String VALID = "{"
            + "\"transactionId\":\"TXN-1\","
            + "\"requestId\":\"REQ-1\","
            + "\"accountId\":\"ACC-1\","
            + "\"transactionType\":\"CREDIT\","
            + "\"amount\":\"10.50\","
            + "\"currency\":\"USD\","
            + "\"sequenceNumber\":1"
            + "}";

    @Test
    void validRecordPasses() {
        SchemaValidationResult result = validator.validate(json(VALID));
        assertTrue(result.isValid());
    }

    @Test
    void validRecordWithOptionalSimulatedFailuresPasses() {
        String withOptional = VALID.substring(0, VALID.length() - 1) + ",\"simulatedFailures\":2}";
        assertTrue(validator.validate(json(withOptional)).isValid());
    }

    @Test
    void nullNodeFails() {
        SchemaValidationResult result = validator.validate(null);
        assertTrue(!result.isValid());
        assertTrue(result.getReason().contains("not a JSON object"));
    }

    @Test
    void nonObjectNodeFails() {
        SchemaValidationResult result = validator.validate(json("[1,2,3]"));
        assertTrue(!result.isValid());
        assertTrue(result.getReason().contains("not a JSON object"));
    }

    @Test
    void missingRequiredFieldFails() {
        String missingAmount = "{"
                + "\"transactionId\":\"TXN-1\","
                + "\"requestId\":\"REQ-1\","
                + "\"accountId\":\"ACC-1\","
                + "\"transactionType\":\"CREDIT\","
                + "\"currency\":\"USD\","
                + "\"sequenceNumber\":1"
                + "}";
        SchemaValidationResult result = validator.validate(json(missingAmount));
        assertTrue(!result.isValid());
        assertTrue(result.getReason().contains("missing required field: amount"));
    }

    @Test
    void unexpectedExtraFieldFails() {
        String extra = VALID.substring(0, VALID.length() - 1) + ",\"extraField\":\"oops\"}";
        SchemaValidationResult result = validator.validate(json(extra));
        assertTrue(!result.isValid());
        assertTrue(result.getReason().contains("unexpected field not in schema: extraField"));
    }

    @Test
    void blankIdFieldFails() {
        String blankRequestId = replaceField(VALID, "requestId", "\"\"");
        SchemaValidationResult result = validator.validate(json(blankRequestId));
        assertTrue(!result.isValid());
        assertTrue(result.getReason().contains("requestId must not be null or blank"));
    }

    @Test
    void negativeNumericIdFails() {
        String negative = replaceField(VALID, "accountId", "\"-5\"");
        SchemaValidationResult result = validator.validate(json(negative));
        assertTrue(!result.isValid());
        assertTrue(result.getReason().contains("must not be a negative number"));
    }

    @Test
    void allZeroIdFails() {
        String zero = replaceField(VALID, "transactionId", "\"0000\"");
        SchemaValidationResult result = validator.validate(json(zero));
        assertTrue(!result.isValid());
        assertTrue(result.getReason().contains("must not be zero"));
    }

    @Test
    void degenerateRepeatedCharacterIdFails() {
        String repeated = replaceField(VALID, "requestId", "\"AAAAAAAA\"");
        SchemaValidationResult result = validator.validate(json(repeated));
        assertTrue(!result.isValid());
        assertTrue(result.getReason().contains("degenerate duplicate value"));
    }

    @Test
    void legitimateHumanReadableIdsAreNotRejectedAsDegenerate() {
        // guards against the pattern over-matching single-character ids
        String singleChar = replaceField(VALID, "requestId", "\"1\"");
        assertTrue(validator.validate(json(singleChar)).isValid());
    }

    @Test
    void invalidTransactionTypeFails() {
        String bad = replaceField(VALID, "transactionType", "\"WIRE\"");
        SchemaValidationResult result = validator.validate(json(bad));
        assertTrue(!result.isValid());
        assertTrue(result.getReason().contains("transactionType must be one of"));
    }

    @Test
    void malformedAmountFails() {
        String bad = replaceField(VALID, "amount", "\"not-a-number\"");
        SchemaValidationResult result = validator.validate(json(bad));
        assertTrue(!result.isValid());
        assertTrue(result.getReason().contains("amount must be a well-formed decimal number"));
    }

    @Test
    void currencyNotThreeUppercaseLettersFails() {
        String bad = replaceField(VALID, "currency", "\"usd\"");
        SchemaValidationResult result = validator.validate(json(bad));
        assertTrue(!result.isValid());
        assertTrue(result.getReason().contains("currency must be a 3-letter uppercase code"));
    }

    @Test
    void zeroOrNegativeSequenceNumberFails() {
        String zero = replaceField(VALID, "sequenceNumber", "0");
        SchemaValidationResult result = validator.validate(json(zero));
        assertTrue(!result.isValid());
        assertTrue(result.getReason().contains("sequenceNumber must be a positive integer"));
    }

    @Test
    void nonIntegralSequenceNumberFails() {
        String decimal = replaceField(VALID, "sequenceNumber", "1.5");
        SchemaValidationResult result = validator.validate(json(decimal));
        assertTrue(!result.isValid());
        assertTrue(result.getReason().contains("sequenceNumber must be a positive integer"));
    }

    @Test
    void validateOrThrowDoesNotThrowForAValidRecord() {
        assertDoesNotThrow(() -> validator.validateOrThrow(json(VALID)));
    }

    @Test
    void validateOrThrowThrowsSchemaValidationExceptionWithTheReason() {
        String missingCurrency = "{"
                + "\"transactionId\":\"TXN-1\","
                + "\"requestId\":\"REQ-1\","
                + "\"accountId\":\"ACC-1\","
                + "\"transactionType\":\"CREDIT\","
                + "\"amount\":\"10\","
                + "\"sequenceNumber\":1"
                + "}";
        SchemaValidationException ex = assertThrows(SchemaValidationException.class,
                () -> validator.validateOrThrow(json(missingCurrency)));
        assertTrue(ex.getMessage().contains("missing required field: currency"));
    }

    /** Replaces the value of one top-level field in the VALID literal with a raw JSON value literal. */
    private String replaceField(String source, String field, String rawJsonValue) {
        String quotedField = "\"" + field + "\":";
        int start = source.indexOf(quotedField) + quotedField.length();
        int end = start;
        if (source.charAt(start) == '"') {
            end = source.indexOf('"', start + 1) + 1;
        } else {
            int comma = source.indexOf(',', start);
            int brace = source.indexOf('}', start);
            end = (comma == -1 || brace < comma) ? brace : comma;
        }
        return source.substring(0, start) + rawJsonValue + source.substring(end);
    }
}
