package org.mycore.frontend.editor.validation.value;

import java.util.Date;

import org.mycore.frontend.editor.validation.MCRDateTimeConverter;

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
