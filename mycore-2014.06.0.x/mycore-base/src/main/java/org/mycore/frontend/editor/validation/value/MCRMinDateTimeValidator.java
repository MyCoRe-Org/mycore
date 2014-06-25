package org.mycore.frontend.editor.validation.value;

import java.util.Date;

import org.mycore.frontend.editor.validation.MCRDateTimeConverter;

public class MCRMinDateTimeValidator extends MCRDateTimeValidator {

    @Override
    public boolean hasRequiredProperties() {
        return super.hasRequiredProperties() && hasProperty("min");
    }

    @Override
    protected boolean isValidOrDie(String input) throws Exception {
        MCRDateTimeConverter converter = new MCRDateTimeConverter(this);
        Date value = converter.string2date(input);
        Date min = converter.string2date(getProperty("min"));
        return min.equals(value) || min.before(value);
    }
}
