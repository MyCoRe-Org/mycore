package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.Before;
import org.mycore.frontend.editor.validation.MCRExternalValidator;

public class MCRExternalValidatorTest extends MCRValidatorTest {

    @Before
    public void setup() {
        validator = new MCRExternalValidator();
        validator.setProperty("class", this.getClass().getName());
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("method", "externalTestString");
        assertTrue(validator.hasRequiredProperties());
    }

    @Test
    public void testString() {
        validator.setProperty("method", "externalTestString");
        assertTrue(validator.isValid("valid"));
        assertFalse(validator.isValid("invalid"));
    }

    @Ignore
    public static boolean externalTestString(String value) {
        return "valid".equals(value);
    }

    @Test
    public void testStringPair() {
        validator.setProperty("method", "externalTestStringPair");
        assertTrue(validator.isValid("Testcase", "case"));
        assertFalse(validator.isValid("Testcase", "foo"));
    }

    @Ignore
    public static boolean externalTestStringPair(String valueA, String valueB) {
        return valueA.endsWith(valueB);
    }
}
