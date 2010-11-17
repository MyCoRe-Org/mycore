package org.mycore.frontend.editor.validation.pair;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mycore.frontend.editor.validation.MCRValidatorBuilder;
import org.mycore.frontend.editor.validation.pair.MCRCombinedPairValidator;
import org.mycore.frontend.editor.validation.pair.MCRIntegerPairValidator;
import org.mycore.frontend.editor.validation.pair.MCRStringPairValidator;

public class MCRCombinedPairValidatorTest extends MCRPairValidatorTest {

    @Before
    public void setup() {
        validator = new MCRCombinedPairValidator();
    }

    @Test
    public void testEmptyCombinedValidator() {
        assertFalse(validator.hasRequiredProperties());
        assertTrue(validator.isValidPair("123", "456"));
    }

    @Test
    public void testHasRequiredProperties() {
        ((MCRCombinedPairValidator) validator).addValidator(new MCRIntegerPairValidator());
        validator.setProperty("type", "integer");
        validator.setProperty("operator", "<");
        assertTrue(validator.hasRequiredProperties());
    }

    @Test
    public void testSingleValidator() {
        ((MCRCombinedPairValidator) validator).addValidator(new MCRStringPairValidator());
        validator.setProperty("type", "string");
        validator.setProperty("operator", "<");
        assertTrue(validator.isValidPair("abc", "abd"));
        assertFalse(validator.isValidPair("abd", "abc"));
    }

    @Test
    public void testPredefinedValidators() {
        validator = MCRValidatorBuilder.buildPredefinedCombinedPairValidator();
        validator.setProperty("type", "integer");
        validator.setProperty("operator", "<");
        validator.setProperty("class", this.getClass().getName());
        validator.setProperty("method", "externalTestForPair");
        assertTrue(validator.isValidPair("123", "456"));
        assertFalse(validator.isValidPair("123", "122"));
        assertFalse(validator.isValidPair("99", "100"));
    }

    @Ignore
    public static boolean externalTestForPair(String valueA, String valueB) {
        return valueA.length() == valueB.length();
    }
}
