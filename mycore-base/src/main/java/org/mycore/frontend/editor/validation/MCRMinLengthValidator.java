package org.mycore.frontend.editor.validation;

public class MCRMinLengthValidator extends MCRValidator {

    @Override
    public boolean hasRequiredPropertiesForValidation() {
        return hasProperty("minLength");
    }

    @Override
    public boolean isValid(String input) {
        int minLength = Integer.parseInt(getProperty("minLength"));
        return (input.length() >= minLength);
    }
}
