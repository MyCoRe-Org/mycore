package org.mycore.frontend.editor.validation;

public abstract class MCRComparingValidator extends MCRPairValidatorBase {

    @Override
    public boolean hasRequiredProperties() {
        return hasProperty("operator");
    }

    @Override
    protected boolean isValidPairOrDie(String valueA, String valueB) {
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

    protected abstract int compare(String valueA, String valueB);
}