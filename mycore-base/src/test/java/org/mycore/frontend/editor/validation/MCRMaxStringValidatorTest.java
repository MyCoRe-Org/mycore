package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;
import org.mycore.frontend.editor.validation.value.MCRMaxStringValidator;

public class MCRMaxStringValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
        validator = new MCRMaxStringValidator();
        validator.setProperty("type", "string");
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("max", "abc");
        assertTrue(validator.hasRequiredProperties());
    }

    @Test
    public void testMaxValid() {
        validator.setProperty("max", "abc");
        assertTrue(validator.isValid("abc"));
    }

    @Test
    public void testMaxInvalid() {
        validator.setProperty("max", "abc");
        assertFalse(validator.isValid("abd"));
    }
}
