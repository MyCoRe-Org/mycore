package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

public class MCRRequiredValidatorTest {

    MCRRequiredValidator validator;

    @Before
    public void setup() {
        validator = new MCRRequiredValidator();
    }

    @Test
    public void testPropertiesMissing() {
        assertFalse(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("required", "true");
        assertTrue(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testRequiredAndValid() {
        validator.setProperty("required", "true");
        assertTrue(validator.isValidExceptionsCatched("input"));
    }

    @Test
    public void testRequiredAndMissing() {
        validator.setProperty("required", "true");
        assertFalse(validator.isValidExceptionsCatched(""));
        assertFalse(validator.isValidExceptionsCatched(" "));
        assertFalse(validator.isValidExceptionsCatched(null));
    }

    @Test
    public void testNotRequired() {
        validator.setProperty("required", "false");
        assertTrue(validator.isValidExceptionsCatched(""));
        assertTrue(validator.isValidExceptionsCatched(" "));
        assertTrue(validator.isValidExceptionsCatched("input"));
    }
}
