package org.mycore.frontend.editor.validation.value;

public class MCRMinStringValidator extends MCRSingleValueValidator {

    @Override
    public boolean hasRequiredProperties() {
        return "string".equals(getProperty("type")) && hasProperty("min");
    }

    @Override
    protected boolean isValidOrDie(String input) {
        String min = getProperty("min");
        return (min.compareTo(input) <= 0);
    }

}
