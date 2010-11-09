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
        else if (input == null)
            return false;
        else if (input.trim().length() == 0)
            return false;
        else
            return true;
    }
}
