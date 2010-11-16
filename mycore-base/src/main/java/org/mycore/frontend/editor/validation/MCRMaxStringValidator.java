package org.mycore.frontend.editor.validation;

public class MCRMaxStringValidator extends MCRValidatorBase {

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
