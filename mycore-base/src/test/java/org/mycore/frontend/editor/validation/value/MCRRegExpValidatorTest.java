package org.mycore.frontend.editor.validation.value;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mycore.frontend.editor.validation.MCRValidatorTest;

public class MCRRegExpValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
        validator = new MCRRegExpValidator();
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("regexp", "a*");
        assertTrue(validator.hasRequiredProperties());
    }

    @Test
    public void testValidPattern() {
        validator.setProperty("regexp", "a*");
        assertTrue(validator.isValid("aaa"));
    }

    @Test
    public void testInvalidPattern() {
        validator.setProperty("regexp", "a*");
        assertFalse(validator.isValid("aba"));
    }
}
