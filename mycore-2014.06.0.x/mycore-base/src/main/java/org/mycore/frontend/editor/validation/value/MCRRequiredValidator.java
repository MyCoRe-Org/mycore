package org.mycore.frontend.editor.validation.value;

public class MCRRequiredValidator extends MCRSingleValueValidator {

    @Override
    public boolean hasRequiredProperties() {
        return hasProperty("required");
    }

    @Override
    protected boolean isValidOrDie(String input) throws Exception {
        boolean required = Boolean.valueOf(getProperty("required"));
        boolean empty = (input == null) || (input.trim().isEmpty());
        return !(required && empty);
    }
}
