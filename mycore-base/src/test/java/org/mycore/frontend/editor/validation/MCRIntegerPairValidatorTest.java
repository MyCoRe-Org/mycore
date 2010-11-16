package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

public class MCRIntegerPairValidatorTest extends MCRComparingValidatorTest {

    @Before
    public void setup() {
        validator = new MCRIntegerPairValidator();
        lowerValue = "5";
        higherValue = "6";
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("type", "integer");
        validator.setProperty("operator", "=");
        assertTrue(validator.hasRequiredProperties());
    }
}
