package org.mycore.frontend.editor.validation.value;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mycore.frontend.editor.validation.MCRValidatorTest;

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
