package org.mycore.frontend.editor.validation;

public class MCRMinLengthValidator extends MCRValidatorBase {

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
