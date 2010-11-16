package org.mycore.frontend.editor.validation;

public class MCRMaxLengthValidator extends MCRValidatorBase {

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
