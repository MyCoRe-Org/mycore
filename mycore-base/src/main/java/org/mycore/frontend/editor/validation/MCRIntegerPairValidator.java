package org.mycore.frontend.editor.validation;

public class MCRIntegerPairValidator extends MCRComparingValidator {

    @Override
    public boolean hasRequiredProperties() {
        return super.hasRequiredProperties() && "integer".equals(getProperty("type"));
    }

    protected int compare(String valueA, String valueB) {
        int a = Integer.parseInt(valueA.trim());
        int b = Integer.parseInt(valueB.trim());
        return (a - b);
    }
}
