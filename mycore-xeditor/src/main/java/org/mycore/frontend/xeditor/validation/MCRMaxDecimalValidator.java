package org.mycore.frontend.xeditor.validation;

public class MCRMaxDecimalValidator extends MCRDecimalValidator {

    private double max;

    @Override
    public boolean hasRequiredAttributes() {
        return super.hasRequiredAttributes() && hasAttributeValue("max");
    }

    @Override
    public void configure() {
        super.configure();
        max = converter.string2double(getAttributeValue("max"));
    }

    @Override
    protected boolean isValid(String value) {
        Double d = converter.string2double(value);
        if (d == null)
            return false;
        else
            return d.doubleValue() <= max;
    }
}
