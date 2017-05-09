package org.mycore.frontend.xeditor.validation;

public class MCRMaxLengthRule extends MCRValidationRule {

    private int maxLength;

    @Override
    public boolean hasRequiredAttributes() {
        return getAttributeValue("maxLength") != null;
    }

    @Override
    public void configure() {
        maxLength = Integer.parseInt(getAttributeValue("maxLength"));
    }

    @Override
    protected boolean isValid(String value) {
        return value.length() <= maxLength;
    }
}