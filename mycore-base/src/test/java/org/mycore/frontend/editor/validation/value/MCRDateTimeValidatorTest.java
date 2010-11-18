package org.mycore.frontend.editor.validation.value;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;
import org.mycore.frontend.editor.validation.MCRValidatorTest;
import org.mycore.frontend.editor.validation.value.MCRDateTimeValidator;

public class MCRDateTimeValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
        validator = new MCRDateTimeValidator();
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("type", "datetime");
        validator.setProperty("format", "dd.MM.yyyy");
        assertTrue(validator.hasRequiredProperties());
    }

    @Test
    public void testValidDates() {
        validator.setProperty("format", "dd.MM.yyyy");
        assertTrue(validator.isValid("22.04.1971"));
        assertTrue(validator.isValid("29.02.2000"));
    }

    @Test
    public void testInvalidDates() {
        validator.setProperty("format", "dd.MM.yyyy");
        assertFalse(validator.isValid("text"));
        assertFalse(validator.isValid("29.02.2001"));
    }

    @Test
    public void testMultipleFormats() {
        validator.setProperty("format", "dd.MM.yyyy ; yyyy-MM-dd");
        assertTrue(validator.isValid("22.04.1971"));
        assertTrue(validator.isValid("1971-04-22"));
    }
}
