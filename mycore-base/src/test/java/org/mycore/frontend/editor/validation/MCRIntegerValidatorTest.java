package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

public class MCRIntegerValidatorTest {

    MCRIntegerValidator validator;

    @Before
    public void setup() {
        validator = new MCRIntegerValidator();
    }

    @Test
    public void testPropertiesMissing() {
        assertFalse(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("type", "integer");
        assertTrue(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testValidInputFormat() {
        assertTrue(validator.isValidExceptionsCatched("123"));
    }

    @Test
    public void testInvalidInputFormat() {
        assertFalse(validator.isValidExceptionsCatched("text"));
    }
}
