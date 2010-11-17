package org.mycore.frontend.editor.validation.value;

public class MCRIntegerValidator extends MCRValidatorBase {

    @Override
    public boolean hasRequiredProperties() {
        return "integer".equals(getProperty("type"));
    }

    @Override
    protected boolean isValidOrDie(String input) throws Exception {
        Integer.parseInt(input);
        return true;
    }
}
