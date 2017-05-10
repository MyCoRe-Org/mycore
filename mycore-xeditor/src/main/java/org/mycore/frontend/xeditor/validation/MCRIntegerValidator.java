package org.mycore.frontend.xeditor.validation;

public class MCRIntegerValidator extends MCRValidator {

    @Override
    public boolean hasRequiredAttributes() {
        return "integer".equals(getAttributeValue("type"));
    }

    @Override
    protected boolean isValid(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}