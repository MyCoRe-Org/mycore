package org.mycore.frontend.editor.validation;

public class MCRMaxIntegerValidator extends MCRIntegerValidator {

    @Override
    public boolean hasRequiredPropertiesForValidation() {
        return super.hasRequiredPropertiesForValidation() && hasProperty("max");
    }

    @Override
    public boolean isValid(String input) {
        int max = Integer.parseInt(getProperty("max"));
        int value = Integer.parseInt(input);
        return (value <= max);
    }
}
