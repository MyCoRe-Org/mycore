package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.Before;

public class MCRExternalValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
        validator = new MCRExternalValidator();
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("class", "org.mycore.frontend.editor.validation.MCRExternalValidatorTest");
        validator.setProperty("method", "externalTestForValue");
        assertTrue(validator.hasRequiredPropertiesForValidation());
    }

    @Test
    public void testExternal() {
        validator.setProperty("class", "org.mycore.frontend.editor.validation.MCRExternalValidatorTest");
        validator.setProperty("method", "externalTestForValue");
        assertTrue(validator.isValidExceptionsCatched("valid"));
        assertFalse(validator.isValidExceptionsCatched("invalid"));
    }

    @Ignore
    public static boolean externalTestForValue(String value) {
        return "valid".equals(value);
    }
}
