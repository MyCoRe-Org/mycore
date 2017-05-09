package org.mycore.frontend.xeditor.validation;

import org.mycore.frontend.xeditor.validation.MCRDateTimeConverter;

public class MCRDateTimeValidator extends MCRValidator {

    private MCRDateTimeConverter dateTimeConverter;

    @Override
    public boolean hasRequiredAttributes() {
        return getAttributeValue("dateTimeFormat") != null;
    }

    @Override
    public void configure() {
        String pattern = getAttributeValue("dateTimeFormat");
        dateTimeConverter = new MCRDateTimeConverter(pattern);
    }

    @Override
    protected boolean isValid(String value) {
        return (dateTimeConverter.string2date(value) != null);
    }
}