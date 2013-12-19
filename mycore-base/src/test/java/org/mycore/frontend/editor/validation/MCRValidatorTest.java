package org.mycore.frontend.editor.validation;

import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.frontend.editor.validation.MCRValidator;

public abstract class MCRValidatorTest extends MCRTestCase{
    
    protected MCRValidator validator;

    @Test
    public void testPropertiesMissing() {
        assertFalse(validator.hasRequiredProperties());
    }
}
