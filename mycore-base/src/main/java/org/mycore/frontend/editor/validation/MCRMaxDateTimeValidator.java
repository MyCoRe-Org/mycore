package org.mycore.frontend.editor.validation;

import java.util.Date;

public class MCRMaxDateTimeValidator extends MCRDateTimeValidator {

    @Override
    public boolean hasRequiredPropertiesForValidation() {
        return super.hasRequiredPropertiesForValidation() && hasProperty("max");
    }

    @Override
    public boolean isValid(String input) throws Exception {
        Date value = string2date(input);
        Date max = string2date(getProperty("max"));
        return value.before(max) || value.equals(max);
    }
}
