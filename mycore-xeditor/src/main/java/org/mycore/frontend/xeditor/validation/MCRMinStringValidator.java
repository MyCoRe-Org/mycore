package org.mycore.frontend.xeditor.validation;

public class MCRMinStringValidator extends MCRValidator {

    private String min;

    @Override
    public boolean hasRequiredAttributes() {
        return "string".equals(getAttributeValue("type")) && hasAttributeValue("min");
    }

    @Override
    public void configure() {
        min = getAttributeValue("min");
    }

    @Override
    protected boolean isValid(String value) {
        return min.compareTo(value) <= 0;
    }
}
