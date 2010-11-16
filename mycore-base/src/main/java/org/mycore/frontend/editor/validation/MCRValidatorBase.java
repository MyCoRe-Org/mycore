package org.mycore.frontend.editor.validation;

public abstract class MCRValidatorBase extends MCRConfigurableBase implements MCRValidator {

    protected abstract boolean isValidOrDie(String input) throws Exception;

    public boolean isValid(String input) {
        try {
            return isValidOrDie(input);
        } catch (Exception ex) {
            return false;
        }
    }
}
