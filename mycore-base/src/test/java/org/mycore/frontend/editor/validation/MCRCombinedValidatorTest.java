package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class MCRCombinedValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
        validator = new MCRCombinedValidator();
    }

    @Test
    public void testEmptyCombinedValidator() {
        assertFalse(validator.hasRequiredPropertiesForValidation());
        assertTrue(validator.isValidExceptionsCatched("foo"));
    }

    @Test
    public void testHasRequiredProperties() {
        ((MCRCombinedValidator) validator).addValidator(new MCRMinLengthValidator());
        validator.setProperty("minLength", "3");
        assertTrue(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testSingleValidator() {
        ((MCRCombinedValidator) validator).addValidator(new MCRMinLengthValidator());
        validator.setProperty("minLength", "3");
        assertTrue(validator.isValidExceptionsCatched("123"));
        assertFalse(validator.isValidExceptionsCatched("12"));
    }

    @Test
    public void testMultipleValidators() {
        ((MCRCombinedValidator) validator).addValidator(new MCRMinLengthValidator());
        ((MCRCombinedValidator) validator).addValidator(new MCRMaxLengthValidator());
        validator.setProperty("minLength", "3");
        validator.setProperty("maxLength", "5");
        assertTrue(validator.isValidExceptionsCatched("123"));
        assertFalse(validator.isValidExceptionsCatched("12"));
        assertFalse(validator.isValidExceptionsCatched("123456"));
    }

    @Test
    public void testPredefinedValidators() {
        ((MCRCombinedValidator) validator).addPredefinedValidators();
        validator.setProperty("minLength", "3");
        validator.setProperty("type", "string");
        validator.setProperty("max", "abc");
        assertTrue(validator.isValidExceptionsCatched("123"));
        assertFalse(validator.isValidExceptionsCatched("12"));
        assertFalse(validator.isValidExceptionsCatched("abd"));
    }
}
