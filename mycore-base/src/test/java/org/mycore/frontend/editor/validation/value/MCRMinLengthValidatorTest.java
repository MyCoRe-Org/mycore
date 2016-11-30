package org.mycore.frontend.editor.validation.value;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mycore.frontend.editor.validation.MCRValidatorTest;

public class MCRMinLengthValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
        validator = new MCRMinLengthValidator();
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("minLength", "3");
        assertTrue(validator.hasRequiredProperties());
    }

    @Test
    public void testMinLengthValid() {
        validator.setProperty("minLength", "3");
        assertTrue(validator.isValid("123"));
    }

    @Test
    public void testMinLengthInvalid() {
        validator.setProperty("minLength", "3");
        assertFalse(validator.isValid("12"));
    }

    @Test
    public void testMinLengthNull() {
        validator.setProperty("minLength", "0");
        assertTrue(validator.isValid(""));
    }
}
