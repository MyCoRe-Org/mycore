package org.mycore.frontend.editor.validation;

public class MCRMaxIntegerValidator extends MCRIntegerValidator {

    @Override
    public boolean hasRequiredProperties() {
        return super.hasRequiredProperties() && hasProperty("max");
    }

    @Override
    protected boolean isValidOrDie(String input) {
        int max = Integer.parseInt(getProperty("max"));
        int value = Integer.parseInt(input);
        return (value <= max);
    }
}
