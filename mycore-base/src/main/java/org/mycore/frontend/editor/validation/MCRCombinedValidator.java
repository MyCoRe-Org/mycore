package org.mycore.frontend.editor.validation;

import java.util.ArrayList;
import java.util.List;

public class MCRCombinedValidator extends MCRValidator {

    private List<MCRValidator> validators = new ArrayList<MCRValidator>();

    @Override
    public boolean hasRequiredPropertiesForValidation() {
        for (MCRValidator validator : validators) {
            if (validator.hasRequiredPropertiesForValidation())
                return true;
        }
        return false;
    }

    @Override
    public boolean isValid(String input) throws Exception {
        for (MCRValidator validator : validators) {
            if (!validator.hasRequiredPropertiesForValidation())
                continue;
            if (!validator.isValidExceptionsCatched(input))
                return false;
        }
        return true;
    }

    public void addValidator(MCRValidator validator) {
        validator.setProperties(getProperties());
        validators.add(validator);
    }

    public void addPredefinedValidators() {
        addValidator(new MCRMaxLengthValidator());
        addValidator(new MCRMinLengthValidator());
        addValidator(new MCRRegExpValidator());
        addValidator(new MCRXSLConditionValidator());
        addValidator(new MCRExternalValidator());
        addValidator(new MCRDateTimeValidator());
        addValidator(new MCRMaxDateTimeValidator());
        addValidator(new MCRMinDateTimeValidator());
        addValidator(new MCRIntegerValidator());
        addValidator(new MCRMaxIntegerValidator());
        addValidator(new MCRMinIntegerValidator());
        addValidator(new MCRDecimalValidator());
        addValidator(new MCRMaxDecimalValidator());
        addValidator(new MCRMinDecimalValidator());
        addValidator(new MCRMaxStringValidator());
        addValidator(new MCRMinStringValidator());
    }
}
