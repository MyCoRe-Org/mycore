package org.mycore.frontend.editor.validation;

public class MCRMaxLengthValidator extends MCRValidator {

    @Override
    public boolean hasRequiredPropertiesForValidation() {
        return hasProperty("maxLength");
    }

    @Override
    public boolean isValid(String input) {
        int maxLength = Integer.parseInt(getProperty("maxLength"));
        return (input.length() <= maxLength);
    }
}
