package org.mycore.frontend.xeditor.validation;

import org.mycore.frontend.xeditor.validation.MCRDateTimeConverter;
import org.w3c.dom.Node;

public class MCRDateTimeFormatRule extends MCRValidationRule {

    private MCRDateTimeConverter dateTimeConverter;

    public MCRDateTimeFormatRule(String baseXPath, Node ruleElement) {
        super(baseXPath, ruleElement);
        String pattern = getAttributeValue("dateTimeFormat");
        dateTimeConverter = new MCRDateTimeConverter(pattern);

    }

    @Override
    protected boolean isValid(String value) {
        return (dateTimeConverter.string2date(value) != null);
    }
}