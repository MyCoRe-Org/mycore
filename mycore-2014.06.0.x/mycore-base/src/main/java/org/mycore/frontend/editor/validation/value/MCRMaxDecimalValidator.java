package org.mycore.frontend.editor.validation.value;

import org.mycore.frontend.editor.validation.MCRDecimalConverter;

public class MCRMaxDecimalValidator extends MCRDecimalValidator {

    @Override
    public boolean hasRequiredProperties() {
        return super.hasRequiredProperties() && hasProperty("max");
    }

    @Override
    protected boolean isValidOrDie(String input) throws Exception {
        MCRDecimalConverter converter = new MCRDecimalConverter(this);
        double value = converter.string2double(input);
        double max = converter.string2double(getProperty("max"));
        return (value <= max);
    }
}
