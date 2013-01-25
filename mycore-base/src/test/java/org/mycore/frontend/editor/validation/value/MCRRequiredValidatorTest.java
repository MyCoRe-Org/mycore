package org.mycore.frontend.editor.validation.value;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;
import org.mycore.frontend.editor.validation.MCRValidatorTest;
import org.mycore.frontend.editor.validation.value.MCRRequiredValidator;

public class MCRRequiredValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
        validator = new MCRRequiredValidator();
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("required", "true");
        assertTrue(validator.hasRequiredProperties());
    }

    @Test
    public void testRequiredAndValid() {
        validator.setProperty("required", "true");
        assertTrue(validator.isValid("input"));
    }

    @Test
    public void testRequiredAndMissing() {
        validator.setProperty("required", "true");
        assertFalse(validator.isValid(""));
        assertFalse(validator.isValid(" "));
        assertFalse(validator.isValid((Object[])null));
    }

    @Test
    public void testNotRequired() {
        validator.setProperty("required", "false");
        assertTrue(validator.isValid(""));
        assertTrue(validator.isValid(" "));
        assertTrue(validator.isValid("input"));
    }
}
