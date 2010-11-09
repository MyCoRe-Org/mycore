package org.mycore.frontend.editor.validation;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class MCRDecimalValidator extends MCRValidator {

    @Override
    public boolean hasRequiredPropertiesForValidation() {
        return "decimal".equals(getProperty("type"));
    }

    @Override
    public boolean isValid(String input) throws Exception {
        string2double(input);
        return true;
    }

    protected double string2double(String string) throws ParseException {
        Locale locale = getLocale();
        NumberFormat nf = NumberFormat.getNumberInstance(locale);
        return nf.parse(string).doubleValue();
    }

    private Locale getLocale() {
        String format = getProperty("format");
        if (format == null)
            return Locale.getDefault();
        else
            return new Locale(format);
    }
}
