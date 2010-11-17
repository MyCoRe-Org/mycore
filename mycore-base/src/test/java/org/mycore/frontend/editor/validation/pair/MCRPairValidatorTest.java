package org.mycore.frontend.editor.validation.pair;

import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.mycore.frontend.editor.validation.pair.MCRPairValidator;

public abstract class MCRPairValidatorTest {

    protected MCRPairValidator validator;

    protected String emptyValue = "";

    protected String lowerValue;

    protected String higherValue;

    @Test
    public void testPropertiesMissing() {
        assertFalse(validator.hasRequiredProperties());
    }
}
