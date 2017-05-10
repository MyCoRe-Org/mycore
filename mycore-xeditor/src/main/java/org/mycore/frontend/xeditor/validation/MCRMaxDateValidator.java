package org.mycore.frontend.xeditor.validation;

import java.util.Date;

public class MCRMaxDateValidator extends MCRDateValidator {

    private Date maxDate;

    @Override
    public boolean hasRequiredAttributes() {
        return super.hasRequiredAttributes() && hasAttributeValue("max");
    }

    @Override
    public void configure() {
        super.configure();
        String max = getAttributeValue("max");
        this.maxDate = dateConverter.string2date(max);
    }

    @Override
    protected boolean isValid(String value) {
        Date date = dateConverter.string2date(value);
        if (date == null)
            return false;
        else
            return maxDate.after(date) || maxDate.equals(date);
    }
}
