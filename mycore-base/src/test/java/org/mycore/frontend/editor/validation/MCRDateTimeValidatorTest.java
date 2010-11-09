package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

public class MCRDateTimeValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
        validator = new MCRDateTimeValidator();
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("type", "datetime");
        validator.setProperty("format", "dd.MM.yyyy");
        assertTrue(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testValidDates() {
        validator.setProperty("type", "datetime");
        validator.setProperty("format", "dd.MM.yyyy");
        assertTrue(validator.isValidExceptionsCatched("22.04.1971"));
        assertTrue(validator.isValidExceptionsCatched("29.02.2000"));
    }

    @Test
    public void testInvalidDates() {
        validator.setProperty("type", "datetime");
        validator.setProperty("format", "dd.MM.yyyy");
        assertFalse(validator.isValidExceptionsCatched("text"));
        assertFalse(validator.isValidExceptionsCatched("29.02.2001"));
    }

    @Test
    public void testMultipleFormats() {
        validator.setProperty("type", "datetime");
        validator.setProperty("format", "dd.MM.yyyy ; yyyy-MM-dd");
        assertTrue(validator.isValidExceptionsCatched("22.04.1971"));
        assertTrue(validator.isValidExceptionsCatched("1971-04-22"));
    }
}
