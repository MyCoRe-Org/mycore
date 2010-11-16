package org.mycore.frontend.editor.validation;

public class MCRRegExpValidator extends MCRValidatorBase {

    @Override
    public boolean hasRequiredProperties() {
        return hasProperty("regexp");
    }

    @Override
    protected boolean isValidOrDie(String input) {
        String pattern = getProperty("regexp");
        return input.matches(pattern);
    }

}
