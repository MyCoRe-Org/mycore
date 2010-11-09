package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

public class MCRMaxLengthValidatorTest {

    MCRMaxLengthValidator validator;

    @Before
    public void setup() {
        validator = new MCRMaxLengthValidator();
    }

    @Test
    public void testPropertiesMissing() {
        assertFalse(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("maxLength", "3");
        assertTrue(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testMaxLengthValid() {
        validator.setProperty("maxLength", "3");
        assertTrue(validator.isValidExceptionsCatched("123"));
    }

    @Test
    public void testMaxLengthInvalid() {
        validator.setProperty("maxLength", "3");
        assertFalse(validator.isValidExceptionsCatched("1234"));
    }

    @Test
    public void testMaxLengthNull() {
        validator.setProperty("maxLength", "0");
        assertTrue(validator.isValidExceptionsCatched(""));
    }
}
