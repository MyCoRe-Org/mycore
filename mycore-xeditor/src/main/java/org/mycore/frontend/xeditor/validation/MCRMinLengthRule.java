package org.mycore.frontend.xeditor.validation;

public class MCRMinLengthRule extends MCRValidationRule {

    private int minLength;

    @Override
    public boolean hasRequiredAttributes() {
        return getAttributeValue("minLength") != null;
    }

    @Override
    public void configure() {
        minLength = Integer.parseInt(getAttributeValue("minLength"));
    }

    @Override
    protected boolean isValid(String value) {
        return minLength <= value.length();
    }
}