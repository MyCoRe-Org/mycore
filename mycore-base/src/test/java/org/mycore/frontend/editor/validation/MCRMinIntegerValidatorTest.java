package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

public class MCRMinIntegerValidatorTest {

    MCRMinIntegerValidator validator;

    @Before
    public void setup() {
        validator = new MCRMinIntegerValidator();
        validator.setProperty("type", "integer");
    }

    @Test
    public void testPropertiesMissing() {
        assertFalse(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("min", "3");
        assertTrue(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testMinValid() {
        validator.setProperty("min", "3");
        assertTrue(validator.isValidExceptionsCatched("3"));
    }

    @Test
    public void testMinInvalid() {
        validator.setProperty("min", "3");
        assertFalse(validator.isValidExceptionsCatched("2"));
    }

    @Test
    public void testInvalidInputFormat() {
        validator.setProperty("min", "3");
        assertFalse(validator.isValidExceptionsCatched("text"));
    }
}
