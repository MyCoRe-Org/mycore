package org.mycore.frontend.editor.validation;

public class MCRDecimalValidator extends MCRValidatorBase {

    @Override
    public boolean hasRequiredProperties() {
        return "decimal".equals(getProperty("type"));
    }

    @Override
    protected boolean isValidOrDie(String input) throws Exception {
        getConverter().string2double(input);
        return true;
    }

    protected MCRDecimalConverter getConverter() {
        String format = getProperty("format");
        return new MCRDecimalConverter(format);
    }

}
