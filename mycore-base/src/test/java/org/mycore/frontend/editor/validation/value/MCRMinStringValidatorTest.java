package org.mycore.frontend.editor.validation.value;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mycore.frontend.editor.validation.MCRValidatorTest;

public class MCRMinStringValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
        validator = new MCRMinStringValidator();
        validator.setProperty("type", "string");
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("min", "abc");
        assertTrue(validator.hasRequiredProperties());
    }

    @Test
    public void testMinValid() {
        validator.setProperty("min", "abc");
        assertTrue(validator.isValid("abc"));
    }

    @Test
    public void testMinInvalid() {
        validator.setProperty("min", "abc");
        assertFalse(validator.isValid("abb"));
    }
}
