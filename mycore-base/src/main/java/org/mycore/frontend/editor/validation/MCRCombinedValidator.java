package org.mycore.frontend.editor.validation;

import java.util.ArrayList;
import java.util.List;

public class MCRCombinedValidator implements MCRValidator {

    private List<MCRValidator> validators = new ArrayList<MCRValidator>();

    public void setProperty(String name, String value) {
        for (MCRConfigurable validator : validators)
            validator.setProperty(name, value);
    }

    public boolean hasRequiredProperties() {
        for (MCRConfigurable validator : validators) {
            if (validator.hasRequiredProperties())
                return true;
        }
        return false;
    }

    public boolean isValid(String input) {
        for (MCRValidator validator : validators) {
            if (!validator.hasRequiredProperties())
                continue;
            if (!validator.isValid(input))
                return false;
        }
        return true;
    }

    public void addValidator(MCRValidator validator) {
        validators.add(validator);
    }
}
