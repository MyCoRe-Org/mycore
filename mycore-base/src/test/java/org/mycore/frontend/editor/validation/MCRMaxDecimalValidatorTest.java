package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

public class MCRMaxDecimalValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
        validator = new MCRMaxDecimalValidator();
        validator.setProperty("type", "decimal");
        validator.setProperty("format", "de");
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("max", "3,14");
        assertTrue(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testMaxValid() {
        validator.setProperty("max", "3,14");
        assertTrue(validator.isValidExceptionsCatched("3,13"));
    }

    @Test
    public void testMaxInvalid() {
        validator.setProperty("max", "3,14");
        assertFalse(validator.isValidExceptionsCatched("4"));
    }
}
