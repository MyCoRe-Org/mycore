package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;
import org.mycore.frontend.editor.validation.value.MCRMaxLengthValidator;

public class MCRMaxLengthValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
        validator = new MCRMaxLengthValidator();
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("maxLength", "3");
        assertTrue(validator.hasRequiredProperties());
    }

    @Test
    public void testMaxLengthValid() {
        validator.setProperty("maxLength", "3");
        assertTrue(validator.isValid("123"));
    }

    @Test
    public void testMaxLengthInvalid() {
        validator.setProperty("maxLength", "3");
        assertFalse(validator.isValid("1234"));
    }

    @Test
    public void testMaxLengthNull() {
        validator.setProperty("maxLength", "0");
        assertTrue(validator.isValid(""));
    }
}
