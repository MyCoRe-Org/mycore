package org.mycore.frontend.editor.validation;

public class MCRCombinedValidator extends MCRCombinedValidatorBase implements MCRValidator {

    public boolean isValid(String input) {
        for (MCRConfigurable validator : validators) {
            if (!validator.hasRequiredProperties())
                continue;
            if (!((MCRValidator) validator).isValid(input))
                return false;
        }
        return true;
    }

    public void addValidator(MCRValidator validator) {
        validators.add(validator);
    }
}
