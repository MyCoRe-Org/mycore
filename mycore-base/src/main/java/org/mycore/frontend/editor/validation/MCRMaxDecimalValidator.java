package org.mycore.frontend.editor.validation;

public class MCRMaxDecimalValidator extends MCRDecimalValidator {

    @Override
    public boolean hasRequiredProperties() {
        return super.hasRequiredProperties() && hasProperty("max");
    }

    @Override
    protected boolean isValidOrDie(String input) throws Exception {
        MCRDecimalConverter converter = getConverter();
        double value = converter.string2double(input);
        double max = converter.string2double(getProperty("max"));
        return (value <= max);
    }
}
