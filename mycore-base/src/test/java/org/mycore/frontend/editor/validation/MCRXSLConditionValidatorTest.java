package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

public class MCRXSLConditionValidatorTest {

    MCRXSLConditionValidator validator;

    @Before
    public void setup() {
        validator = new MCRXSLConditionValidator();
    }

    @Test
    public void testPropertiesMissing() {
        assertFalse(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("xsl", "true");
        assertTrue(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testValidPattern() {
        validator.setProperty("xsl", "starts-with(.,'http://')");
        assertTrue(validator.isValidExceptionsCatched("http://www.foo.bar"));
    }

    @Test
    public void testInvalidPattern() {
        validator.setProperty("xsl", "starts-with(.,'http://')");
        assertFalse(validator.isValidExceptionsCatched("somethingDifferent"));
    }
}
