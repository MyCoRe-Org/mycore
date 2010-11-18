package org.mycore.frontend.editor.validation.pair;

import org.mycore.frontend.editor.validation.MCRValidatorBase;

public abstract class MCRComparingValidator extends MCRValidatorBase {

    @Override
    public boolean hasRequiredProperties() {
        return hasProperty("operator");
    }

    @Override
    protected boolean isValidOrDie(Object... input) throws Exception {
        String valueA = (String) (input[0]);
        String valueB = (String) (input[1]);

        if (isEmpty(valueA) || isEmpty(valueB))
            return true;

        String operator = getOperator();
        int result = compare(valueA, valueB);

        if (result < 0)
            return operator.contains("<");
        else if (result > 0)
            return operator.contains(">");
        else
            return operator.contains("=");
    }

    private String getOperator() {
        String operator = getProperty("operator");
        if ("!=".equals(operator))
            operator = "<>";
        return operator;
    }

    protected boolean isEmpty(String value) {
        return (value == null) || value.trim().isEmpty();
    }

    protected abstract int compare(String valueA, String valueB) throws Exception;
}