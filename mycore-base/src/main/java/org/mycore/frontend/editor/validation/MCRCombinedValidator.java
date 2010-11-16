package org.mycore.frontend.editor.validation;

import java.util.ArrayList;
import java.util.List;

public class MCRCombinedValidator extends MCRValidatorBase {

    private List<MCRValidator> validators = new ArrayList<MCRValidator>();

    @Override
    public void setProperty(String name, String value) {
        super.setProperty(name, value);
        for (MCRValidator validator : validators)
            validator.setProperty(name, value);
    }

    @Override
    public boolean hasRequiredProperties() {
        for (MCRValidator validator : validators) {
            if (validator.hasRequiredProperties())
                return true;
        }
        return false;
    }

    @Override
    protected boolean isValidOrDie(String input) throws Exception {
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
