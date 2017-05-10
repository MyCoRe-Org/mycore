package org.mycore.frontend.xeditor.validation;

public class MCRMinIntegerValidator extends MCRIntegerValidator {

    private int min;

    @Override
    public boolean hasRequiredAttributes() {
        return super.hasRequiredAttributes() && hasAttributeValue("min");
    }

    @Override
    public void configure() {
        min = Integer.parseInt(getAttributeValue("min"));
    }

    @Override
    protected boolean isValid(String value) {
        if (!super.isValid(value))
            return false;
        else
            return min <= Integer.parseInt(value);
    }
}