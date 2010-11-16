package org.mycore.frontend.editor.validation;

import org.mycore.common.MCRConfigurationException;

public abstract class MCRComparingValidator extends MCRPairValidatorBase {

    @Override
    public boolean hasRequiredProperties() {
        return hasProperty("operator");
    }

    @Override
    protected boolean isValidPairOrDie(String valueA, String valueB) throws Exception {
        valueA = (valueA == null ? "" : valueA.trim());
        valueB = (valueB == null ? "" : valueB.trim());
        if (valueA.isEmpty() || valueB.isEmpty())
            return true;

        String operator = getProperty("operator");
        if ("!=".equals(operator))
            operator = "<>";

        int result = compare(valueA, valueB);

        if (result < 0)
            return operator.contains("<");
        else if (result > 0)
            return operator.contains(">");
        else
            return operator.contains("=");
    }

    protected abstract int compare(String valueA, String valueB);
}