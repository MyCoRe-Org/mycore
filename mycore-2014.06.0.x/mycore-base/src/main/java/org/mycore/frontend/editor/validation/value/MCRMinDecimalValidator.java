package org.mycore.frontend.editor.validation.value;

import org.mycore.frontend.editor.validation.MCRDecimalConverter;

public class MCRMinDecimalValidator extends MCRDecimalValidator {

    @Override
    public boolean hasRequiredProperties() {
        return super.hasRequiredProperties() && hasProperty("min");
    }

    @Override
    protected boolean isValidOrDie(String input) throws Exception {
        MCRDecimalConverter converter = new MCRDecimalConverter(this);
        double value = converter.string2double(input);
        double min = converter.string2double(getProperty("min"));
        return (min <= value);
    }
}
