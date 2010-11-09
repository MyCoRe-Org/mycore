package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

public class MCRRegExpValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
        validator = new MCRRegExpValidator();
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("regexp", "a*");
        assertTrue(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testValidPattern() {
        validator.setProperty("regexp", "a*");
        assertTrue(validator.isValidExceptionsCatched("aaa"));
    }

    @Test
    public void testInvalidPattern() {
        validator.setProperty("regexp", "a*");
        assertFalse(validator.isValidExceptionsCatched("aba"));
    }
}
