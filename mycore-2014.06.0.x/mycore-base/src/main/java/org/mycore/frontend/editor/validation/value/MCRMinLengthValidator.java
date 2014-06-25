package org.mycore.frontend.editor.validation.value;

public class MCRMinLengthValidator extends MCRSingleValueValidator {

    @Override
    public boolean hasRequiredProperties() {
        return hasProperty("minLength");
    }

    @Override
    protected boolean isValidOrDie(String input) {
        int minLength = Integer.parseInt(getProperty("minLength"));
        return (input.length() >= minLength);
    }
}
