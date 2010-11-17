package org.mycore.frontend.editor.validation.value;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;
import org.mycore.frontend.editor.validation.value.MCRMinDateTimeValidator;

public class MCRMinDateTimeValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
        validator = new MCRMinDateTimeValidator();
        validator.setProperty("type", "datetime");
        validator.setProperty("format", "dd.MM.yyyy");
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("min", "22.04.1971");
        assertTrue(validator.hasRequiredProperties());
    }

    @Test
    public void testMinValid() {
        validator.setProperty("min", "22.04.1971");
        assertTrue(validator.isValid("22.04.1971"));
    }

    @Test
    public void testMinInvalid() {
        validator.setProperty("min", "22.04.1971");
        assertFalse(validator.isValid("21.04.1971"));
    }
}
