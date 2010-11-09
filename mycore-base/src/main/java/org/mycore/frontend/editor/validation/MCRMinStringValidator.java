package org.mycore.frontend.editor.validation;

public class MCRMinStringValidator extends MCRValidator {

    @Override
    public boolean hasRequiredPropertiesForValidation() {
        return "string".equals(getProperty("type")) && hasProperty("min");
    }

    @Override
    public boolean isValid(String input) {
        String min = getProperty("min");
        return (min.compareTo(input) <= 0);
    }

}
