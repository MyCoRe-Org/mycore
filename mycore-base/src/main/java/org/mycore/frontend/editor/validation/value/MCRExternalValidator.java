package org.mycore.frontend.editor.validation.value;

import org.mycore.frontend.editor.validation.MCRExternalValidationInvoker;

public class MCRExternalValidator extends MCRValidatorBase {

    @Override
    public boolean hasRequiredProperties() {
        return hasProperty("class") && hasProperty("method");
    }

    @Override
    @SuppressWarnings("unchecked")
    protected boolean isValidOrDie(String input) throws Exception {
        Class[] argTypes = { String.class };
        Object[] args = { input };
        return new MCRExternalValidationInvoker(this, argTypes).validateExternally(args);
    }
}
