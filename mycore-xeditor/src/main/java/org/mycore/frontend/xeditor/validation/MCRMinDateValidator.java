package org.mycore.frontend.xeditor.validation;

import java.util.Date;

public class MCRMinDateValidator extends MCRDateValidator {

    private Date minDate;

    @Override
    public boolean hasRequiredAttributes() {
        return super.hasRequiredAttributes() && getAttributeValue("min") != null;
    }

    @Override
    public void configure() {
        super.configure();
        String min = getAttributeValue("min");
        this.minDate = dateConverter.string2date(min);
    }

    @Override
    protected boolean isValid(String value) {
        Date date = dateConverter.string2date(value);
        if (date == null)
            return false;
        else
            return minDate.before(date) || minDate.equals(date);
    }
}
