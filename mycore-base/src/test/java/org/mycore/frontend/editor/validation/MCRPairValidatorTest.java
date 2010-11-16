package org.mycore.frontend.editor.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public abstract class MCRPairValidatorTest {

    protected MCRPairValidator validator;

    protected String emptyValue = "";

    protected String lowerValue;

    protected String higherValue;

    @Test
    public void testPropertiesMissing() {
        assertFalse(validator.hasRequiredProperties());
    }

    @Test
    public void testIncompleteInput() {
        assertTrue(validator.isValidPair(lowerValue, null));
        assertTrue(validator.isValidPair(null, lowerValue));
        assertTrue(validator.isValidPair(lowerValue, emptyValue));
        assertTrue(validator.isValidPair(emptyValue, lowerValue));
        assertTrue(validator.isValidPair(emptyValue, emptyValue));
        assertTrue(validator.isValidPair(null, null));
    }

}
