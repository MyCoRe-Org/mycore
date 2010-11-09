package org.mycore.frontend.editor.validation;

public class MCRMaxDecimalValidator extends MCRDecimalValidator {

    @Override
    public boolean hasRequiredPropertiesForValidation() {
        return super.hasRequiredPropertiesForValidation() && hasProperty("max");
    }

    @Override
    public boolean isValid(String input) throws Exception {
        double value = string2double(input);
        double max = string2double(getProperty("max"));
        return (value <= max);
    }
}
