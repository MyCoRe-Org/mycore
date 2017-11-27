/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.frontend.xeditor.validation;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * Helper for decimal validators to convert string input into a decimal value for a given locale.
 * 
 * @author Frank L\u00FCtzenkirchen 
 */
public class MCRDecimalConverter {

    private Locale locale = Locale.getDefault();

    public MCRDecimalConverter(String localeID) {
        this.locale = new Locale(localeID);
    }

    /**
     * Converts a given text string to a decimal number, using the given locale.
     * 
     * @param value the text strin
     * @return null, if the text contains illegal chars or can not be parsed
     */
    public Double string2double(String value) {
        if (hasIllegalCharacters(value)) {
            return null;
        }

        NumberFormat nf = NumberFormat.getNumberInstance(locale);
        if ((nf instanceof DecimalFormat) && hasMultipleDecimalSeparators(value, (DecimalFormat) nf)) {
            return null;
        }

        try {
            return nf.parse(value).doubleValue();
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
