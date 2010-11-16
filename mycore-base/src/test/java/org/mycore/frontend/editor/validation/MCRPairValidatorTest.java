package org.mycore.frontend.editor.validation;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

public abstract class MCRPairValidatorTest {
    MCRPairValidator validator;

    @Test
    public void testPropertiesMissing() {
        assertFalse(validator.hasRequiredProperties());
    }
}
