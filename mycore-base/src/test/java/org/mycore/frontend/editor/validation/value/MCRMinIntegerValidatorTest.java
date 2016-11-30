package org.mycore.frontend.editor.validation.value;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mycore.frontend.editor.validation.MCRValidatorTest;

public class MCRMinIntegerValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
        validator = new MCRMinIntegerValidator();
        validator.setProperty("type", "integer");
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("min", "3");
        assertTrue(validator.hasRequiredProperties());
    }

    @Test
    public void testMinValid() {
        validator.setProperty("min", "3");
        assertTrue(validator.isValid("3"));
    }

    @Test
    public void testMinInvalid() {
        validator.setProperty("min", "3");
        assertFalse(validator.isValid("2"));
    }

    @Test
    public void testInvalidInputFormat() {
        validator.setProperty("min", "3");
        assertFalse(validator.isValid("text"));
    }
}
