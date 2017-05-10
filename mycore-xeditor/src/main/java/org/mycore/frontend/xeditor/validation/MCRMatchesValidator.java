package org.mycore.frontend.xeditor.validation;

public class MCRMatchesValidator extends MCRValidator {

    private String pattern;

    @Override
    public boolean hasRequiredAttributes() {
        return hasAttributeValue("matches");
    }

    @Override
    public void configure() {
        this.pattern = getAttributeValue("matches");
    }

    @Override
    protected boolean isValid(String value) {
        return value.matches(pattern);
    }
}