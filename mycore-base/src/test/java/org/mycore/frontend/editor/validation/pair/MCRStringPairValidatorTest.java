package org.mycore.frontend.editor.validation.pair;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class MCRStringPairValidatorTest extends MCRComparingValidatorTest {

    @Before
    public void setup() {
        validator = new MCRStringPairValidator();
        lowerValue = "abc";
        higherValue = "def";
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("type", "string");
        validator.setProperty("operator", "=");
        assertTrue(validator.hasRequiredProperties());
    }
}
