package org.mycore.frontend.editor.validation;

public class MCRRegExpValidator extends MCRValidator {

    @Override
    public boolean hasRequiredPropertiesForValidation() {
        return hasProperty("regexp");
    }

    @Override
    public boolean isValid(String input) {
        String pattern = getProperty("regexp");
        return input.matches(pattern);
    }

}
