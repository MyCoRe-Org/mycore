package org.mycore.frontend.editor.validation;

import java.util.Date;

public class MCRMaxDateTimeValidator extends MCRDateTimeValidator {

    @Override
    public boolean hasRequiredProperties() {
        return super.hasRequiredProperties() && hasProperty("max");
    }

    @Override
    protected boolean isValidOrDie(String input) throws Exception {
        MCRDateTimeConverter converter = getConverter();
        Date value = converter.string2date(input);
        Date max = converter.string2date(getProperty("max"));
        return value.before(max) || value.equals(max);
    }
}
