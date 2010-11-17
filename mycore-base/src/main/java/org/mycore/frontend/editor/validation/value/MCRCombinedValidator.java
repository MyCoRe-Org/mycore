package org.mycore.frontend.editor.validation.value;

import org.mycore.frontend.editor.validation.MCRCombinedValidatorBase;
import org.mycore.frontend.editor.validation.MCRConfigurable;

public class MCRCombinedValidator extends MCRCombinedValidatorBase implements MCRValidator {

    public boolean isValid(String input) {
        for (MCRConfigurable validator : validators) {
            if (!validator.hasRequiredProperties())
                continue;
            if (!((MCRValidator) validator).isValid(input)) {
                reportValidationResult(validator, false, input);
                return false;
            }
        }
        reportValidationResult(this, true, input);
        return true;
    }

    public void addValidator(MCRValidator validator) {
        validators.add(validator);
    }
}
