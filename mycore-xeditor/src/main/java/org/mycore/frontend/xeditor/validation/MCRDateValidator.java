package org.mycore.frontend.xeditor.validation;

import org.mycore.frontend.xeditor.validation.MCRDateConverter;

public class MCRDateValidator extends MCRValidator {

    protected MCRDateConverter dateConverter;

    @Override
    public boolean hasRequiredAttributes() {
        return "date".equals(getAttributeValue("type")) && (getAttributeValue("format") != null);
    }

    @Override
    public void configure() {
        String format = getAttributeValue("format");
        dateConverter = new MCRDateConverter(format);
    }

    @Override
    protected boolean isValid(String value) {
        return (dateConverter.string2date(value) != null);
    }
}