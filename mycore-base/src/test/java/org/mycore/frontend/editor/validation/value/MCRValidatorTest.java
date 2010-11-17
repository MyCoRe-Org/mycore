package org.mycore.frontend.editor.validation.value;

import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.mycore.frontend.editor.validation.value.MCRValidator;

public abstract class MCRValidatorTest {
    MCRValidator validator;

    @Test
    public void testPropertiesMissing() {
        assertFalse(validator.hasRequiredProperties());
    }
}
