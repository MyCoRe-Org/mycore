package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mycore.frontend.editor.validation.value.MCRMaxLengthValidator;
import org.mycore.frontend.editor.validation.value.MCRMinLengthValidator;

public class MCRCombinedValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
        validator = new MCRCombinedValidator();
    }

    @Test
    public void testEmptyCombinedValidator() {
        assertFalse(validator.hasRequiredProperties());
        assertTrue(validator.isValid("foo"));
    }

    @Test
    public void testHasRequiredProperties() {
        ((MCRCombinedValidator) validator).addValidator(new MCRMinLengthValidator());
        validator.setProperty("minLength", "3");
        assertTrue(validator.hasRequiredProperties());
    }

    @Test
    public void testSingleValidator() {
        ((MCRCombinedValidator) validator).addValidator(new MCRMinLengthValidator());
        validator.setProperty("minLength", "3");
        assertTrue(validator.isValid("123"));
        assertFalse(validator.isValid("12"));
    }

    @Test
    public void testMultipleValidators() {
        ((MCRCombinedValidator) validator).addValidator(new MCRMinLengthValidator());
        ((MCRCombinedValidator) validator).addValidator(new MCRMaxLengthValidator());
        validator.setProperty("minLength", "3");
        validator.setProperty("maxLength", "5");
        assertTrue(validator.isValid("123"));
        assertFalse(validator.isValid("12"));
        assertFalse(validator.isValid("123456"));
    }

    @Test
    public void testPredefinedValidators() {
        validator = MCRValidatorBuilder.buildPredefinedCombinedValidator();
        validator.setProperty("minLength", "3");
        validator.setProperty("type", "string");
        validator.setProperty("max", "abc");
        assertTrue(validator.isValid("123"));
        assertFalse(validator.isValid("12"));
        assertFalse(validator.isValid("abd"));
    }
}
