package org.mycore.frontend.editor.validation.value;

public class MCRMaxLengthValidator extends MCRSingleValueValidator {

    @Override
    public boolean hasRequiredProperties() {
        return hasProperty("maxLength");
    }

    @Override
    protected boolean isValidOrDie(String input) {
        int maxLength = Integer.parseInt(getProperty("maxLength"));
        return (input.length() <= maxLength);
    }
}
