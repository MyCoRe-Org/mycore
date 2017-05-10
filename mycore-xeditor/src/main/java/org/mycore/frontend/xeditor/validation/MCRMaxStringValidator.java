package org.mycore.frontend.xeditor.validation;

public class MCRMaxStringValidator extends MCRValidator {

    private String max;

    @Override
    public boolean hasRequiredAttributes() {
        return "string".equals(getAttributeValue("type")) && hasAttributeValue("max");
    }

    @Override
    public void configure() {
        max = getAttributeValue("max");
    }

    @Override
    protected boolean isValid(String value) {
        return max.compareTo(value) >= 0;
    }
}
