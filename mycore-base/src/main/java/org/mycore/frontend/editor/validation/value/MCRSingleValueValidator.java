package org.mycore.frontend.editor.validation.value;

import org.mycore.frontend.editor.validation.MCRValidatorBase;

public abstract class MCRSingleValueValidator extends MCRValidatorBase {

    @Override
    protected boolean isValidOrDie(Object... input) throws Exception {
        String value = (String) (input[0]);
        return isValidOrDie(value);
    }

    protected abstract boolean isValidOrDie(String input) throws Exception;
}
