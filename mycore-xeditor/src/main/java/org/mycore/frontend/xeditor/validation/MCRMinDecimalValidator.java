package org.mycore.frontend.xeditor.validation;

public class MCRMinDecimalValidator extends MCRDecimalValidator {

    private double min;

    @Override
    public boolean hasRequiredAttributes() {
        return super.hasRequiredAttributes() && hasAttributeValue("min");
    }

    @Override
    public void configure() {
        super.configure();
        min = converter.string2double(getAttributeValue("min"));
    }

    @Override
    protected boolean isValid(String value) {
        Double d = converter.string2double(value);
        if (d == null)
            return false;
        else
            return min <= d.doubleValue();
    }
}
