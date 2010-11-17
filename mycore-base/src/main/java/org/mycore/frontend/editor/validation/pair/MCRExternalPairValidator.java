package org.mycore.frontend.editor.validation.pair;

import org.mycore.frontend.editor.validation.MCRExternalValidationInvoker;

public class MCRExternalPairValidator extends MCRPairValidatorBase implements MCRPairValidator {

    @Override
    public boolean hasRequiredProperties() {
        return hasProperty("class") && hasProperty("method");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isValidPairOrDie(String valueA, String valueB) throws Exception {
        Class[] argTypes = { String.class, String.class };
        Object[] args = { valueA, valueB };
        return new MCRExternalValidationInvoker(this, argTypes).validateExternally(args);
    }
}
