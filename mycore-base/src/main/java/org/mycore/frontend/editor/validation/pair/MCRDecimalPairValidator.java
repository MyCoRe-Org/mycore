package org.mycore.frontend.editor.validation.pair;

import org.mycore.frontend.editor.validation.MCRDecimalConverter;

public class MCRDecimalPairValidator extends MCRComparingValidator {

    @Override
    public boolean hasRequiredProperties() {
        return super.hasRequiredProperties() && "decimal".equals(getProperty("type"));
    }

    protected int compare(String valueA, String valueB) throws Exception {
        MCRDecimalConverter converter = new MCRDecimalConverter(this);
        double a = converter.string2double(valueA);
        double b = converter.string2double(valueB);
        return (int) (Math.signum(a - b));
    }
}
