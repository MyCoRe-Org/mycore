package org.mycore.frontend.editor.validation;

import java.util.ArrayList;
import java.util.List;

public class MCRCombinedValidator extends MCRValidatorBase implements MCRValidator {

    protected List<MCRValidator> validators = new ArrayList<MCRValidator>();

    public void addValidator(MCRValidator validator) {
        validators.add(validator);
    }

    public void setProperty(String name, String value) {
        super.setProperty(name, value);
        for (MCRValidator validator : validators)
            validator.setProperty(name, value);
    }

    public boolean hasRequiredProperties() {
        return validators.stream().anyMatch(MCRValidator::hasRequiredProperties);
    }

    protected boolean isValidOrDie(Object... input) {
        return validators.stream()
                         .filter(MCRValidator::hasRequiredProperties)
                         .allMatch(validator -> validator.isValid(input));
    }
}
