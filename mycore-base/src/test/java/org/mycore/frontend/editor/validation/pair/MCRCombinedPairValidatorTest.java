package org.mycore.frontend.editor.validation.pair;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mycore.frontend.editor.validation.MCRCombinedValidator;
import org.mycore.frontend.editor.validation.MCRValidatorBuilder;
import org.mycore.frontend.editor.validation.MCRValidatorTest;
import org.mycore.frontend.editor.validation.pair.MCRIntegerPairValidator;
import org.mycore.frontend.editor.validation.pair.MCRStringPairValidator;

public class MCRCombinedPairValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
        validator = new MCRCombinedValidator();
    }

    @Test
    public void testEmptyCombinedValidator() {
        assertFalse(validator.hasRequiredProperties());
        assertTrue(validator.isValid("123", "456"));
    }

    @Test
    public void testHasRequiredProperties() {
        ((MCRCombinedValidator) validator).addValidator(new MCRIntegerPairValidator());
        validator.setProperty("type", "integer");
        validator.setProperty("operator", "<");
        assertTrue(validator.hasRequiredProperties());
    }

    @Test
    public void testSingleValidator() {
        ((MCRCombinedValidator) validator).addValidator(new MCRStringPairValidator());
        validator.setProperty("type", "string");
        validator.setProperty("operator", "<");
        assertTrue(validator.isValid("abc", "abd"));
        assertFalse(validator.isValid("abd", "abc"));
    }

    @Test
    public void testPredefinedValidators() {
        validator = MCRValidatorBuilder.buildPredefinedCombinedPairValidator();
        validator.setProperty("type", "integer");
        validator.setProperty("operator", "<");
        validator.setProperty("class", this.getClass().getName());
        validator.setProperty("method", "externalTestForPair");
        assertTrue(validator.isValid("123", "456"));
        assertFalse(validator.isValid("123", "122"));
        assertFalse(validator.isValid("99", "100"));
    }

    @Ignore
    public static boolean externalTestForPair(String valueA, String valueB) {
        return valueA.length() == valueB.length();
    }
}
