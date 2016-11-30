package org.mycore.frontend.editor.validation.value;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mycore.frontend.editor.validation.MCRValidatorTest;

public class MCRMaxIntegerValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
        validator = new MCRMaxIntegerValidator();
        validator.setProperty("type", "integer");
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("max", "3");
        assertTrue(validator.hasRequiredProperties());
    }

    @Test
    public void testMaxValid() {
        validator.setProperty("max", "3");
        assertTrue(validator.isValid("3"));
    }

    @Test
    public void testMaxInvalid() {
        validator.setProperty("max", "3");
        assertFalse(validator.isValid("4"));
    }

    @Test
    public void testInvalidInputFormat() {
        validator.setProperty("max", "3");
        assertFalse(validator.isValid("text"));
    }
}
