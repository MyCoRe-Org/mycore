package org.mycore.frontend.editor.validation;

public class MCRStringPairValidator extends MCRComparingValidator {

    @Override
    public boolean hasRequiredProperties() {
        return super.hasRequiredProperties() && "string".equals(getProperty("type"));
    }

    protected int compare(String valueA, String valueB) {
        return valueA.compareTo(valueB);
    }
}
