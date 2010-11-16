package org.mycore.frontend.editor.validation;

public class MCRCombinedPairValidator extends MCRCombinedValidatorBase implements MCRPairValidator {

    public boolean isValidPair(String valueA, String valueB) {
        for (MCRConfigurable validator : validators) {
            if (!validator.hasRequiredProperties())
                continue;
            if (!((MCRPairValidator) validator).isValidPair(valueA, valueB))
                return false;
        }
        return true;
    }

    public void addValidator(MCRPairValidator validator) {
        validators.add(validator);
    }
}
