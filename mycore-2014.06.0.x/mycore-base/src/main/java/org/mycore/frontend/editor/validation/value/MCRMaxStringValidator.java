package org.mycore.frontend.editor.validation.value;

public class MCRMaxStringValidator extends MCRSingleValueValidator {

    @Override
    public boolean hasRequiredProperties() {
        return "string".equals(getProperty("type")) && hasProperty("max");
    }

    @Override
    protected boolean isValidOrDie(String input) {
        String max = getProperty("max");
        return (input.compareTo(max) <= 0);
    }
}
