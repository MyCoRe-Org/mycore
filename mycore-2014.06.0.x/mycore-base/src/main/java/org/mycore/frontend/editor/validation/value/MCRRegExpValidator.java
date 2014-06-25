package org.mycore.frontend.editor.validation.value;

public class MCRRegExpValidator extends MCRSingleValueValidator {

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
