package org.mycore.frontend.editor.validation.value;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;
import org.mycore.frontend.editor.validation.MCRValidatorTest;
import org.mycore.frontend.editor.validation.value.MCRMaxDecimalValidator;

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
        assertTrue(validator.hasRequiredProperties());
    }

    @Test
    public void testMaxValid() {
        validator.setProperty("max", "3,14");
        assertTrue(validator.isValid("3,13"));
    }

    @Test
    public void testMaxInvalid() {
        validator.setProperty("max", "3,14");
        assertFalse(validator.isValid("4"));
    }
}
