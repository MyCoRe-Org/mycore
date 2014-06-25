package org.mycore.frontend.editor.validation.value;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;
import org.mycore.frontend.editor.validation.MCRValidatorTest;
import org.mycore.frontend.editor.validation.value.MCRDecimalValidator;

public class MCRDecimalValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
        validator = new MCRDecimalValidator();
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("type", "decimal");
        assertTrue(validator.hasRequiredProperties());
    }

    @Test
    public void testIntegerIsValid() {
        assertTrue(validator.isValid("123"));
    }

    @Test
    public void testDecimalValidForLocaleEN() {
        validator.setProperty("format", "en");
        assertTrue(validator.isValid("123.45"));
        assertTrue(validator.isValid("123,456.78"));
    }

    @Test
    public void testDecimalValidForLocaleDE() {
        validator.setProperty("format", "de");
        assertTrue(validator.isValid("123,45"));
        assertTrue(validator.isValid("123.456,78"));
    }

    @Test
    public void testDecimalInvalidForLocaleDE() {
        validator.setProperty("format", "de");
        assertFalse(validator.isValid("123,456,78"));
    }

    @Test
    public void testDecimalInvalidForLocaleEN() {
        validator.setProperty("format", "en");
        assertFalse(validator.isValid("123.456.78"));
    }

    @Test
    public void testTextIsInvalid() {
        assertFalse(validator.isValid("123 EUR"));
        assertFalse(validator.isValid("text"));
    }
}
