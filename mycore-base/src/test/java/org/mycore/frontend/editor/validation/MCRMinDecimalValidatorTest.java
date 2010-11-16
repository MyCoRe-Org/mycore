package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

public class MCRMinDecimalValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
        validator = new MCRMinDecimalValidator();
        validator.setProperty("type", "decimal");
        validator.setProperty("format", "de");
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("min", "3,14");
        assertTrue(validator.hasRequiredProperties());
    }

    @Test
    public void testMinValid() {
        validator.setProperty("min", "3,14");
        assertTrue(validator.isValid("3,14"));
    }

    @Test
    public void testMinInvalid() {
        validator.setProperty("min", "3,14");
        assertFalse(validator.isValid("2"));
    }
}
