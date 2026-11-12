package com.mphasis.assessment.txservice.unit.service;

import com.mphasis.assessment.txservice.service.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationResultTest {

    @Test
    void okIsValidWithNullReason() {
        ValidationResult result = ValidationResult.ok();
        assertTrue(result.isValid());
        assertNull(result.getReason());
    }

    @Test
    void failIsInvalidWithGivenReason() {
        ValidationResult result = ValidationResult.fail("something went wrong");
        assertFalse(result.isValid());
        assertEquals("something went wrong", result.getReason());
    }
}
