package com.mphasis.assessment.txservice.unit.util;

import com.mphasis.assessment.txservice.util.AccountIdMasking;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for {@link AccountIdMasking}: the single NFR-06 masking rule. */
class AccountIdMaskingTest {

    @Test
    void nullAccountIdMasksToAllAsterisks() {
        assertEquals("***", AccountIdMasking.mask(null));
    }

    @Test
    void emptyStringMasksToAllAsterisks() {
        assertEquals("***", AccountIdMasking.mask(""));
    }

    @Test
    void lengthAtOrBelowFourMasksToAllAsterisks() {
        assertEquals("***", AccountIdMasking.mask("A"));
        assertEquals("***", AccountIdMasking.mask("AB12"));
    }

    @Test
    void lengthAboveFourKeepsTheLastThreeCharacters() {
        assertEquals("***201", AccountIdMasking.mask("ACC-201"));
        assertEquals("***CDE", AccountIdMasking.mask("ABCDE"));
    }
}
