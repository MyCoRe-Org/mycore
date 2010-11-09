package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

public class MCRDecimalValidatorTest {

    MCRDecimalValidator validator;

    @Before
    public void setup() {
        validator = new MCRDecimalValidator();
    }

    @Test
    public void testPropertiesMissing() {
        assertFalse(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("type", "decimal");
        assertTrue(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testIntegerIsValid() {
        assertTrue(validator.isValidExceptionsCatched("123"));
    }

    @Test
    public void testDecimalValidForLocaleEN() {
        validator.setProperty("format", "en");
        assertTrue(validator.isValidExceptionsCatched("123.45"));
        assertTrue(validator.isValidExceptionsCatched("123,456.78"));
    }

    @Test
    public void testDecimalValidForLocaleDE() {
        validator.setProperty("format", "de");
        assertTrue(validator.isValidExceptionsCatched("123,45"));
        assertTrue(validator.isValidExceptionsCatched("123.456,78"));
    }

    @Test
    public void testDecimalInvalidForLocaleDE() {
        validator.setProperty("format", "de");
        assertFalse(validator.isValidExceptionsCatched("123,456,78"));
    }

    @Test
    public void testDecimalInvalidForLocaleEN() {
        validator.setProperty("format", "en");
        assertFalse(validator.isValidExceptionsCatched("123.456.78"));
    }

    @Test
    public void testTextIsInvalid() {
        assertFalse(validator.isValidExceptionsCatched("123 EUR"));
        assertFalse(validator.isValidExceptionsCatched("text"));
    }
}
