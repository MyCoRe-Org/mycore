package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

public class MCRMinDateTimeValidatorTest {

    MCRMinDateTimeValidator validator;

    @Before
    public void setup() {
        validator = new MCRMinDateTimeValidator();
        validator.setProperty("type", "datetime");
        validator.setProperty("format", "dd.MM.yyyy");
    }

    @Test
    public void testPropertiesMissing() {
        assertFalse(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("min", "22.04.1971");
        assertTrue(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testMinValid() {
        validator.setProperty("min", "22.04.1971");
        assertTrue(validator.isValidExceptionsCatched("22.04.1971"));
    }

    @Test
    public void testMinInvalid() {
        validator.setProperty("min", "22.04.1971");
        assertFalse(validator.isValidExceptionsCatched("21.04.1971"));
    }
}
