package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

public class MCRMaxStringValidatorTest {

    MCRMaxStringValidator validator;

    @Before
    public void setup() {
        validator = new MCRMaxStringValidator();
        validator.setProperty("type", "string");
    }

    @Test
    public void testPropertiesMissing() {
        assertFalse(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("max", "abc");
        assertTrue(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testMaxValid() {
        validator.setProperty("max", "abc");
        assertTrue(validator.isValidExceptionsCatched("abc"));
    }

    @Test
    public void testMaxInvalid() {
        validator.setProperty("max", "abc");
        assertFalse(validator.isValidExceptionsCatched("abd"));
    }
}
