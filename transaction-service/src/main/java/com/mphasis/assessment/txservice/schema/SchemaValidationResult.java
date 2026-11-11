package com.mphasis.assessment.txservice.schema;

/** Outcome of {@link RequestSchemaValidator#validate(com.fasterxml.jackson.databind.JsonNode)}. */
public final class SchemaValidationResult {

    private final boolean valid;
    private final String reason;

    private SchemaValidationResult(boolean valid, String reason) {
        this.valid = valid;
        this.reason = reason;
    }

    public static SchemaValidationResult ok() {
        return new SchemaValidationResult(true, null);
    }

    public static SchemaValidationResult fail(String reason) {
        return new SchemaValidationResult(false, reason);
    }

    public boolean isValid() {
        return valid;
    }

    public String getReason() {
        return reason;
    }
}
