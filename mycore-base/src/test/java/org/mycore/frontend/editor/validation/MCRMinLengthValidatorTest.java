package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

public class MCRMinLengthValidatorTest {

    MCRMinLengthValidator validator;

    @Before
    public void setup() {
        validator = new MCRMinLengthValidator();
    }

    @Test
    public void testPropertiesMissing() {
        assertFalse(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("minLength", "3");
        assertTrue(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testMinLengthValid() {
        validator.setProperty("minLength", "3");
        assertTrue(validator.isValidExceptionsCatched("123"));
    }

    @Test
    public void testMinLengthInvalid() {
        validator.setProperty("minLength", "3");
        assertFalse(validator.isValidExceptionsCatched("12"));
    }

    @Test
    public void testMinLengthNull() {
        validator.setProperty("minLength", "0");
        assertTrue(validator.isValidExceptionsCatched(""));
    }
}
