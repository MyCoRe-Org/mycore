package org.mycore.frontend.xeditor.validation;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class MCRDecimalConverter {

    private Locale locale = Locale.getDefault();

    public MCRDecimalConverter(String format) {
        locale = new Locale(format);
    }

    public Double string2double(String value) {
        if (hasIllegalCharacters(value))
            return null;

        NumberFormat nf = NumberFormat.getNumberInstance(locale);
        if ((nf instanceof DecimalFormat) && hasMultipleDecimalSeparators(value, (DecimalFormat) nf))
            return null;

        try {
            return new Double(nf.parse(value).doubleValue());
        } catch (ParseException e) {
            return null;
        }
    }

    private boolean hasMultipleDecimalSeparators(String string, DecimalFormat df) {
        DecimalFormatSymbols dfs = df.getDecimalFormatSymbols();
        String patternNonDecimalSeparators = "[^" + dfs.getDecimalSeparator() + "]";
        String decimalSeparatorsLeftOver = string.replaceAll(patternNonDecimalSeparators, "");
        return (decimalSeparatorsLeftOver.length() > 1);
    }

    private boolean hasIllegalCharacters(String string) {
        return !string.matches("[0-9,.]+");
    }
}
