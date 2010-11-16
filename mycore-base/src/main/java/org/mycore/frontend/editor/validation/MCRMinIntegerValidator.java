package org.mycore.frontend.editor.validation;

public class MCRMinIntegerValidator extends MCRIntegerValidator {

    @Override
    public boolean hasRequiredProperties() {
        return super.hasRequiredProperties() && hasProperty("min");
    }

    @Override
    protected boolean isValidOrDie(String input) {
        int min = Integer.parseInt(getProperty("min"));
        int value = Integer.parseInt(input);
        return (min <= value);
    }
}
