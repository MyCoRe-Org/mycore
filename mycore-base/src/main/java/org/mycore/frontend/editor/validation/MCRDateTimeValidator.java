package org.mycore.frontend.editor.validation;

public class MCRDateTimeValidator extends MCRValidatorBase {

    @Override
    public boolean hasRequiredProperties() {
        return "datetime".equals(getProperty("type")) && hasProperty("format");
    }

    @Override
    protected boolean isValidOrDie(String input) throws Exception {
        getConverter().string2date(input);
        return true;
    }

    protected MCRDateTimeConverter getConverter() {
        String patterns = getProperty("format");
        return new MCRDateTimeConverter(patterns);
    }
}
