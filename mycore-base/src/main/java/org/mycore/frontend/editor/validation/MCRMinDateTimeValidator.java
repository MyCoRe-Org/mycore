package org.mycore.frontend.editor.validation;

import java.util.Date;

public class MCRMinDateTimeValidator extends MCRDateTimeValidator {

    @Override
    public boolean hasRequiredProperties() {
        return super.hasRequiredProperties() && hasProperty("min");
    }

    @Override
    protected boolean isValidOrDie(String input) throws Exception {
        Date value = string2date(input);
        Date min = string2date(getProperty("min"));
        return min.equals(value) || min.before(value);
    }
}
