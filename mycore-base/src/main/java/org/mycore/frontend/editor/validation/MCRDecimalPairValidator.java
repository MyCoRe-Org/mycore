package org.mycore.frontend.editor.validation;

public class MCRDecimalPairValidator extends MCRComparingValidator {

    @Override
    public boolean hasRequiredProperties() {
        return super.hasRequiredProperties() && "decimal".equals(getProperty("type"));
    }

    protected int compare(String valueA, String valueB) throws Exception {
        String format = getProperty("format");
        MCRDecimalConverter converter = new MCRDecimalConverter(format);
        double a = converter.string2double(valueA);
        double b = converter.string2double(valueB);
        return (int) (Math.signum(a - b));
    }
}
