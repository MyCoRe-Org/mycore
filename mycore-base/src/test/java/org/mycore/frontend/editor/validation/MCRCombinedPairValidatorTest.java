package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mycore.frontend.editor.validation.pair.MCRCombinedPairValidator;
import org.mycore.frontend.editor.validation.pair.MCRIntegerPairValidator;
import org.mycore.frontend.editor.validation.pair.MCRStringPairValidator;

public class MCRCombinedPairValidatorTest extends MCRPairValidatorTest {

    @Before
    public void setup() {
        validator = new MCRCombinedPairValidator();
        lowerValue = "123";
        higherValue = "456";
    }

    @Test
    public void testEmptyCombinedValidator() {
        assertFalse(validator.hasRequiredProperties());
        assertTrue(validator.isValidPair(lowerValue, higherValue));
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
        assertTrue(validator.isValidPair(lowerValue, higherValue));
        assertFalse(validator.isValidPair(higherValue, lowerValue));
    }

    @Test
    public void testPredefinedValidators() {
        validator = MCRValidatorBuilder.buildPredefinedCombinedPairValidator();
        validator.setProperty("type", "integer");
        validator.setProperty("operator", "<");
        assertTrue(validator.isValidPair(lowerValue, higherValue));
        assertFalse(validator.isValidPair(higherValue, lowerValue));
    }
}
