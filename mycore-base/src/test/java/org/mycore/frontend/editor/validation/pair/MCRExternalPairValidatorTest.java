package org.mycore.frontend.editor.validation.pair;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class MCRExternalPairValidatorTest extends MCRPairValidatorTest {

    @Before
    public void setup() {
        validator = new MCRExternalPairValidator();
        validator.setProperty("class", this.getClass().getName());
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("method", "externalTestForPair");
        assertTrue(validator.hasRequiredProperties());
    }

    @Test
    public void testExternal() {
        validator.setProperty("method", "externalTestForPair");
        assertTrue(validator.isValidPair("Testcase", "case"));
        assertFalse(validator.isValidPair("Testcase", "foo"));
    }

    @Ignore
    public static boolean externalTestForPair(String valueA, String valueB) {
        return valueA.endsWith(valueB);
    }
}
