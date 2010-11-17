package org.mycore.frontend.editor.validation.pair;

import org.mycore.frontend.editor.validation.MCRCombinedValidatorBase;
import org.mycore.frontend.editor.validation.MCRConfigurable;

public class MCRCombinedPairValidator extends MCRCombinedValidatorBase implements MCRPairValidator {

    public boolean isValidPair(String valueA, String valueB) {
        for (MCRConfigurable validator : validators) {
            if (!validator.hasRequiredProperties())
                continue;
            if (!((MCRPairValidator) validator).isValidPair(valueA, valueB)) {
                reportValidationResult(validator, false, valueA, valueB);
                return false;
            }
        }
        reportValidationResult(this, true, valueA, valueB);
        return true;
    }

    public void addValidator(MCRPairValidator validator) {
        validators.add(validator);
    }
}
