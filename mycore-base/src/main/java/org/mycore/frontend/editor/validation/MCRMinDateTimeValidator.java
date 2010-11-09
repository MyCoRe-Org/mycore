package org.mycore.frontend.editor.validation;

import java.util.Date;

public class MCRMinDateTimeValidator extends MCRDateTimeValidator {

    @Override
    public boolean hasRequiredPropertiesForValidation() {
        return super.hasRequiredPropertiesForValidation() && hasProperty("min");
    }

    @Override
    public boolean isValid(String input) throws Exception {
        Date value = string2date(input);
        Date min = string2date(getProperty("min"));
        return min.equals(value) || min.before(value);
    }
}
