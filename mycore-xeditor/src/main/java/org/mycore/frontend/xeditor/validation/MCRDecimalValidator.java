package org.mycore.frontend.xeditor.validation;

public class MCRDecimalValidator extends MCRValidator {

    protected MCRDecimalConverter converter;
    
    @Override
    public boolean hasRequiredAttributes() {
        return "decimal".equals(getAttributeValue("type")) && hasAttributeValue("locale");
    }

    @Override
    public void configure() {
        String locale = getAttributeValue("locale");
        converter = new MCRDecimalConverter(locale);
    }

    @Override
    protected boolean isValid(String value) {
        return converter.string2double(value) != null;
    }
}