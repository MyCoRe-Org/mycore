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
        for (MCRValidator validator : validators) {
            if (validator.hasRequiredProperties())
                return true;
        }
        return false;
    }

    protected boolean isValidOrDie(Object... input) {
        for (MCRValidator validator : validators) {
            if (!validator.hasRequiredProperties())
                continue;
            if (!validator.isValid(input)) {
                return false;
            }
        }
        return true;
    }
}
