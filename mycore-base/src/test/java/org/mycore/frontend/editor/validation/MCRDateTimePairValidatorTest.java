package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

public class MCRDateTimePairValidatorTest extends MCRComparingValidatorTest {

    @Before
    public void setup() {
        validator = new MCRDateTimePairValidator();
        validator.setProperty("format", "dd.MM.yyyy");
        lowerValue = "22.04.1971";
        higherValue = "17.11.2010";
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("type", "datetime");
        validator.setProperty("operator", "=");
        assertTrue(validator.hasRequiredProperties());
    }
}
