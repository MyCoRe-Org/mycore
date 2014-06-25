package org.mycore.frontend.editor.validation.value;

import org.mycore.frontend.editor.validation.MCRDecimalConverter;

public class MCRDecimalValidator extends MCRSingleValueValidator {

    @Override
    public boolean hasRequiredProperties() {
        return "decimal".equals(getProperty("type"));
    }

    @Override
    protected boolean isValidOrDie(String input) throws Exception {
        new MCRDecimalConverter(this).string2double(input);
        return true;
    }
}
