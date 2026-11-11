package com.mphasis.assessment.txservice.schema;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * US-02 / L2-FR-02 hardening: validates a raw incoming JSON record against
 * the service's exact expected request schema, before it is ever
 * deserialized into a {@link com.mphasis.assessment.txservice.model.TransactionRequest}.
 * <p>
 * Deliberately kept separate from {@code Validator} (which runs business
 * field rules - e.g. "amount must be positive" - on an already-deserialized
 * request): this class only answers "is this JSON record even shaped like a
 * request at all", so every JSON-accepting entry point can reject a
 * structurally malformed record before business logic ever sees it.
 */
public final class RequestSchemaValidator {

    private static final Set<String> REQUIRED_FIELDS = Set.of(
            "transactionId", "requestId", "accountId", "transactionType",
            "amount", "currency", "sequenceNumber");
    private static final Set<String> OPTIONAL_FIELDS = Set.of("simulatedFailures");

    // Deliberately NOT pinned to one id convention (e.g. 16-hex-digit hash,
    // 7-digit number): this validator gates every JSON source InMemoryTransactionSource
    // reads, including the official sample-transactions.json batch, which still
    // uses the original human-readable ids (TXN-2001, REQ-1, ACC-201...). Enforcing
    // a specific id *shape* here would reject that legitimate, unchanged data -
    // these checks instead catch values that are null/blank, numerically negative,
    // zero, or a degenerate repeated-character placeholder, regardless of convention.
    private static final Pattern NEGATIVE_NUMBER = Pattern.compile("^-\\d+(\\.\\d+)?$");
    private static final Pattern ALL_ZERO = Pattern.compile("^0+(\\.0+)?$");
    /** Every character identical (e.g. "0000000000", "AAAAAAAAAA", "----------") - a degenerate placeholder, not a real id. */
    private static final Pattern ALL_SAME_CHARACTER = Pattern.compile("^(.)\\1*$");
    private static final Pattern DECIMAL_NUMBER = Pattern.compile("^-?\\d+(\\.\\d+)?$");
    private static final Pattern TRANSACTION_TYPE = Pattern.compile("^(CREDIT|DEBIT|REVERSAL)$");
    private static final Pattern CURRENCY_CODE = Pattern.compile("^[A-Z]{3}$");

    /** Validates one record; never throws. */
    public SchemaValidationResult validate(JsonNode node) {
        if (node == null || !node.isObject()) {
            return SchemaValidationResult.fail("record is not a JSON object");
        }

        SchemaValidationResult shape = validateShape(node);
        if (!shape.isValid()) {
            return shape;
        }

        return validateFieldFormats(node);
    }

    /** Validates one record; throws {@link SchemaValidationException} with the rejection reason if invalid. */
    public void validateOrThrow(JsonNode node) {
        SchemaValidationResult result = validate(node);
        if (!result.isValid()) {
            throw new SchemaValidationException(result.getReason());
        }
    }

    private SchemaValidationResult validateShape(JsonNode node) {
        for (String required : REQUIRED_FIELDS) {
            if (!node.has(required)) {
                return SchemaValidationResult.fail("missing required field: " + required);
            }
        }
        Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String name = fieldNames.next();
            if (!REQUIRED_FIELDS.contains(name) && !OPTIONAL_FIELDS.contains(name)) {
                return SchemaValidationResult.fail("unexpected field not in schema: " + name);
            }
        }
        return SchemaValidationResult.ok();
    }

    private SchemaValidationResult validateFieldFormats(JsonNode node) {
        SchemaValidationResult transactionId = validateId(node, "transactionId");
        if (!transactionId.isValid()) {
            return transactionId;
        }

        SchemaValidationResult requestId = validateId(node, "requestId");
        if (!requestId.isValid()) {
            return requestId;
        }

        SchemaValidationResult accountId = validateId(node, "accountId");
        if (!accountId.isValid()) {
            return accountId;
        }

        String typeText = asText(node, "transactionType");
        if (typeText == null || !TRANSACTION_TYPE.matcher(typeText).matches()) {
            return SchemaValidationResult.fail("transactionType must be one of CREDIT, DEBIT, REVERSAL");
        }

        String amountText = asText(node, "amount");
        if (amountText == null || !DECIMAL_NUMBER.matcher(amountText).matches()) {
            return SchemaValidationResult.fail("amount must be a well-formed decimal number");
        }

        String currency = asText(node, "currency");
        if (currency == null || !CURRENCY_CODE.matcher(currency).matches()) {
            return SchemaValidationResult.fail("currency must be a 3-letter uppercase code");
        }

        JsonNode seqNode = node.get("sequenceNumber");
        if (seqNode == null || !seqNode.isIntegralNumber() || seqNode.asLong() <= 0) {
            return SchemaValidationResult.fail("sequenceNumber must be a positive integer");
        }

        return SchemaValidationResult.ok();
    }

    /**
     * Shared id-field assertions, convention-agnostic by design (see the
     * class-level note on why no fixed id shape is enforced here): present,
     * non-blank, not a negative number, not zero, and not a degenerate
     * repeated-character "duplicate" value.
     */
    private SchemaValidationResult validateId(JsonNode node, String field) {
        String value = asText(node, field);
        if (value == null || value.isBlank()) {
            return SchemaValidationResult.fail(field + " must not be null or blank");
        }
        if (NEGATIVE_NUMBER.matcher(value).matches()) {
            return SchemaValidationResult.fail(field + " must not be a negative number, got: " + value);
        }
        if (ALL_ZERO.matcher(value).matches()) {
            return SchemaValidationResult.fail(field + " must not be zero, got: " + value);
        }
        if (value.length() > 1 && ALL_SAME_CHARACTER.matcher(value).matches()) {
            return SchemaValidationResult.fail(field + " must not be a degenerate duplicate value (every character the same): " + value);
        }
        return SchemaValidationResult.ok();
    }

    private String asText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }
}
