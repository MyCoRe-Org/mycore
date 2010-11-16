package org.mycore.frontend.editor.validation;

public class MCRMinDecimalValidator extends MCRDecimalValidator {

    @Override
    public boolean hasRequiredProperties() {
        return super.hasRequiredProperties() && hasProperty("min");
    }
    
    @Override
    protected boolean isValidOrDie(String input) throws Exception {
        double value = string2double(input);
        double min = string2double(getProperty("min"));
        return (min <= value);
    }
}
