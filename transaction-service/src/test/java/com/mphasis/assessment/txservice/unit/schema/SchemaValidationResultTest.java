package com.mphasis.assessment.txservice.unit.schema;

import com.mphasis.assessment.txservice.schema.SchemaValidationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaValidationResultTest {

    @Test
    void okIsValidWithNullReason() {
        SchemaValidationResult result = SchemaValidationResult.ok();
        assertTrue(result.isValid());
        assertNull(result.getReason());
    }

    @Test
    void failIsInvalidWithGivenReason() {
        SchemaValidationResult result = SchemaValidationResult.fail("bad shape");
        assertFalse(result.isValid());
        assertEquals("bad shape", result.getReason());
    }
}
