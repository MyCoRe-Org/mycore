package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

public class MCRMaxIntegerValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
        validator = new MCRMaxIntegerValidator();
        validator.setProperty("type", "integer");
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("max", "3");
        assertTrue(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testMaxValid() {
        validator.setProperty("max", "3");
        assertTrue(validator.isValidExceptionsCatched("3"));
    }

    @Test
    public void testMaxInvalid() {
        validator.setProperty("max", "3");
        assertFalse(validator.isValidExceptionsCatched("4"));
    }

    @Test
    public void testInvalidInputFormat() {
        validator.setProperty("max", "3");
        assertFalse(validator.isValidExceptionsCatched("text"));
    }
}
