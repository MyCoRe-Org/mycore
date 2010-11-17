package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;
import org.mycore.frontend.editor.validation.value.MCRXSLConditionValidator;

public class MCRXSLConditionValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
        validator = new MCRXSLConditionValidator();
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("xsl", "true");
        assertTrue(validator.hasRequiredProperties());
    }

    @Test
    public void testValidPattern() {
        validator.setProperty("xsl", "starts-with(.,'http://')");
        assertTrue(validator.isValid("http://www.foo.bar"));
    }

    @Test
    public void testInvalidPattern() {
        validator.setProperty("xsl", "starts-with(.,'http://')");
        assertFalse(validator.isValid("somethingDifferent"));
    }
}
