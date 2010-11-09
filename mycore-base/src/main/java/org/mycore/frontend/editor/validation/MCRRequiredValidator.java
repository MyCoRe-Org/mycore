package org.mycore.frontend.editor.validation;

public class MCRRequiredValidator extends MCRValidator {

    @Override
    public boolean hasRequiredPropertiesForValidation() {
        return hasProperty("required");
    }

    @Override
    public boolean isValid(String input) throws Exception {
        boolean required = Boolean.getBoolean(getProperty("required"));

        if (!required)
            return true;
        if (input == null)
            return false;

        return !input.trim().isEmpty();
    }
}
