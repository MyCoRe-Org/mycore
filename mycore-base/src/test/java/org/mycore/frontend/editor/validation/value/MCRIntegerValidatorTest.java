package org.mycore.frontend.editor.validation.value;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mycore.frontend.editor.validation.MCRValidatorTest;

public class MCRIntegerValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
        validator = new MCRIntegerValidator();
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("type", "integer");
        assertTrue(validator.hasRequiredProperties());
    }

    @Test
    public void testValidInputFormat() {
        assertTrue(validator.isValid("123"));
    }

    @Test
    public void testInvalidInputFormat() {
        assertFalse(validator.isValid("text"));
    }
}
