package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;
import org.mycore.frontend.editor.validation.pair.MCRStringPairValidator;

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
