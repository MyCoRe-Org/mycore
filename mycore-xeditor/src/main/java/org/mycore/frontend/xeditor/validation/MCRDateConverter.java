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

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.mycore.common.MCRException;

/**
 * Helper for date validators to convert string input into a date value.
 * 
 * @author Frank L\u00FCtzenkirchen 
 */
public class MCRDateConverter {

    private static final Date CHECK_DATE = new Date(0L);

    private List<SimpleDateFormat> formats = new ArrayList<>();

    /**
     * @param patterns a list of allowed SimpleDateFormat patterns separated by ";" 
     */
    public MCRDateConverter(String patterns) {
        for (String pattern : patterns.split(";")) {
            formats.add(getDateFormat(pattern.trim()));
        }
    }

    protected SimpleDateFormat getDateFormat(String pattern) {
        SimpleDateFormat df = new SimpleDateFormat(pattern, Locale.ROOT);
        df.setLenient(false);
        return df;
    }

    /**
     * 
     * @param input the text string
     * @return the parsed Date matching one of the allowed date patterns, or null if the text can not be parsed
     */
    public Date string2date(String input) throws MCRException {
        for (SimpleDateFormat format : formats) {
            if (format.format(CHECK_DATE).length() != input.length()) {
                continue;
            }
            try {
                ParsePosition pp = new ParsePosition(0);
                Date value = format.parse(input, pp);
                if (pp.getIndex() == input.length()) {
                    return value;
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }
}
