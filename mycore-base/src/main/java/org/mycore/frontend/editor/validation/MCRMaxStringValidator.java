package org.mycore.frontend.editor.validation;

public class MCRMaxStringValidator extends MCRValidator {

    @Override
    public boolean hasRequiredPropertiesForValidation() {
        return "string".equals(getProperty("type")) && hasProperty("max");
    }

    @Override
    public boolean isValid(String input) {
        String max = getProperty("max");
        return (input.compareTo(max) <= 0);
    }
}
