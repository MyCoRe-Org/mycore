package org.mycore.frontend.editor.validation.value;

import org.mycore.frontend.editor.validation.MCRDateTimeConverter;

public class MCRDateTimeValidator extends MCRSingleValueValidator {

    @Override
    public boolean hasRequiredProperties() {
        return "datetime".equals(getProperty("type")) && hasProperty("format");
    }

    @Override
    protected boolean isValidOrDie(String input) throws Exception {
        new MCRDateTimeConverter(this).string2date(input);
        return true;
    }
}
