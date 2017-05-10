package org.mycore.frontend.xeditor.validation;

public class MCRMaxIntegerValidator extends MCRIntegerValidator {

    private int max;

    @Override
    public boolean hasRequiredAttributes() {
        return super.hasRequiredAttributes() && hasAttributeValue("max");
    }

    @Override
    public void configure() {
        max = Integer.parseInt(getAttributeValue("max"));
    }

    @Override
    protected boolean isValid(String value) {
        if (!super.isValid(value))
            return false;
        else
            return Integer.parseInt(value) <= max;
    }
}