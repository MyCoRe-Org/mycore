package org.mycore.frontend.editor.validation;

public class MCRMinIntegerValidator extends MCRIntegerValidator {

    @Override
    public boolean hasRequiredPropertiesForValidation() {
        return super.hasRequiredPropertiesForValidation() && hasProperty("min");
    }

    @Override
    public boolean isValid(String input) {
        int min = Integer.parseInt(getProperty("min"));
        int value = Integer.parseInt(input);
        return (min <= value);
    }
}
