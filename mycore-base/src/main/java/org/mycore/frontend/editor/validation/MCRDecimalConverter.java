package org.mycore.frontend.editor.validation;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import org.mycore.common.MCRException;

public class MCRDecimalConverter {

    private Locale locale = Locale.getDefault();

    public MCRDecimalConverter(MCRValidator validator) {
        String format = validator.getProperty("format");
        if (format != null)
            locale = new Locale(format);
    }

    public double string2double(String value) throws Exception {
        NumberFormat nf = NumberFormat.getNumberInstance(locale);

        checkForIllegalCharacters(value);

        if (nf instanceof DecimalFormat)
            checkForMultipleDecimalSeparators(value, (DecimalFormat) nf);

        return nf.parse(value).doubleValue();
    }

    private void checkForMultipleDecimalSeparators(String string, DecimalFormat df) {
        DecimalFormatSymbols dfs = df.getDecimalFormatSymbols();
        String patternNonDecimalSeparators = "[^" + dfs.getDecimalSeparator() + "]";
        String decimalSeparatorsLeftOver = string.replaceAll(patternNonDecimalSeparators, "");

        if (decimalSeparatorsLeftOver.length() > 1) {
            throw new MCRException("Number contains repeated decimal separator: " + string);
        }

    }

    private void checkForIllegalCharacters(String string) {
        if (!string.matches("[0-9,.]+"))
            throw new MCRException("Number contains illegal characters: " + string);
    }
}
