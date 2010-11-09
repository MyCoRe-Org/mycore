package org.mycore.frontend.editor.validation;

public class MCRIntegerValidator extends MCRValidator {

    @Override
    public boolean hasRequiredPropertiesForValidation() {
        return "integer".equals(getProperty("type"));
    }

    @Override
    public boolean isValid(String input) throws Exception {
        Integer.parseInt(input);
        return true;
    }
}
