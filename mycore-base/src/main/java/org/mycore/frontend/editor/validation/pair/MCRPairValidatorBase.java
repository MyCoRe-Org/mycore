package org.mycore.frontend.editor.validation.pair;

import org.mycore.frontend.editor.validation.MCRConfigurableBase;

public abstract class MCRPairValidatorBase extends MCRConfigurableBase implements MCRPairValidator {

    protected abstract boolean isValidPairOrDie(String valueA, String valueB) throws Exception;

    public boolean isValidPair(String valueA, String valueB) {
        try {
            return isValidPairOrDie(valueA, valueB);
        } catch (Exception ex) {
            return false;
        }
    }
}
