package org.mycore.frontend.editor.validation;

public class MCRMinDecimalValidator extends MCRDecimalValidator {

    @Override
    public boolean hasRequiredPropertiesForValidation() {
        return super.hasRequiredPropertiesForValidation() && hasProperty("min");
    }
    
    @Override
    public boolean isValid(String input) throws Exception {
        double value = string2double(input);
        double min = string2double(getProperty("min"));
        return (min <= value);
    }
}
