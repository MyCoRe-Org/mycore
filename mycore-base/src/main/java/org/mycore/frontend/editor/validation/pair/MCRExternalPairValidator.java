package org.mycore.frontend.editor.validation.pair;

import org.mycore.frontend.editor.validation.MCRExternalValidationInvoker;

public class MCRExternalPairValidator extends MCRPairValidatorBase implements MCRPairValidator {

    @Override
    public boolean hasRequiredProperties() {
        return hasProperty("class") && hasProperty("method");
    }

    @Override
    public boolean isValidPairOrDie(String valueA, String valueB) throws Exception {
        String clazz = getProperty("class");
        String method = getProperty("method");
        Class[] argTypes = { String.class, String.class };

        MCRExternalValidationInvoker invoker = new MCRExternalValidationInvoker(clazz, method, argTypes);

        Object[] args = { valueA, valueB };
        return invoker.validateExternally(args);
    }
}
