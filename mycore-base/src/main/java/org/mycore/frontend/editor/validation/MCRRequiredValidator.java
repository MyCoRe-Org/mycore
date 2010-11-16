package org.mycore.frontend.editor.validation;

public class MCRRequiredValidator extends MCRValidatorBase {

    @Override
    public boolean hasRequiredProperties() {
        return hasProperty("required");
    }

    @Override
    protected boolean isValidOrDie(String input) throws Exception {
        boolean required = Boolean.valueOf(getProperty("required"));

        if (!required)
            return true;
        if (input == null)
            return false;

        return !input.trim().isEmpty();
    }
}
