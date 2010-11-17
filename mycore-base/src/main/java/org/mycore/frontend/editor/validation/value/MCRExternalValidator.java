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
        String clazz = getProperty("class");
        String method = getProperty("method");
        Class[] argTypes = { String.class };

        MCRExternalValidationInvoker invoker = new MCRExternalValidationInvoker(clazz, method, argTypes);

        Object[] args = { input };
        return invoker.validateExternally(args);
    }
}
