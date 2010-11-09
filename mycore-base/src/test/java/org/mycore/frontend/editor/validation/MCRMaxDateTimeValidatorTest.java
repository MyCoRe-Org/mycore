package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

public class MCRMaxDateTimeValidatorTest {

    MCRMaxDateTimeValidator validator;

    @Before
    public void setup() {
        validator = new MCRMaxDateTimeValidator();
        validator.setProperty("type", "datetime");
        validator.setProperty("format", "dd.MM.yyyy");
    }

    @Test
    public void testPropertiesMissing() {
        assertFalse(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("max", "22.04.1971");
        assertTrue(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testMaxValid() {
        validator.setProperty("max", "22.04.1971");
        assertTrue(validator.isValidExceptionsCatched("22.04.1971"));
    }

    @Test
    public void testMaxInvalid() {
        validator.setProperty("max", "22.04.1971");
        assertFalse(validator.isValidExceptionsCatched("23.04.1971"));
    }
}
