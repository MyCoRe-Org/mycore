package org.mycore.frontend.editor.validation;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

public abstract class MCRValidatorTest {
    MCRValidator validator;

    @Test
    public void testPropertiesMissing() {
        assertFalse(validator.hasRequiredProperties());
    }
}
