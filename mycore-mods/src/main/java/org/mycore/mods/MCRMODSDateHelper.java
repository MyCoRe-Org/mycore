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

package org.mycore.mods;

import static org.mycore.mods.MCRMODSDateFormat.DATE_LOCALE;
import static org.mycore.mods.MCRMODSDateFormat.MODS_TIMEZONE;

import java.text.ParseException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Objects;

import org.jdom2.Element;
import org.mycore.common.MCRException;

/**
 * Helper class to parse and build MODS date elements, see
 * http://www.loc.gov/standards/mods/userguide/generalapp.html#encoding
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRMODSDateHelper {

    public static Date getDate(Element element) {
        if (element == null) {
            return null;
        }

        String text = element.getTextTrim();
        if ((text == null) || text.isEmpty()) {
            return null;
        }

        String encoding = element.getAttributeValue("encoding", "unknown").toLowerCase(DATE_LOCALE);
        String key = encoding + "-" + text.length();
        MCRMODSDateFormat format = firstNonNull(MCRMODSDateFormat.getFormat(key),
            MCRMODSDateFormat.getFormat(encoding));
        if (format == null) {
            throw reportParseException(encoding, text, null);
        }
        try {
            return format.parseDate(text);
        } catch (ParseException ex) {
            throw reportParseException(encoding, text, ex);
        }
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... o) {
        for (T test : Objects.requireNonNull(o)) {
            if (test != null) {
                return test;
            }
        }
        throw new NullPointerException();
    }

    public static GregorianCalendar getCalendar(Element element) {
        Date date = getDate(element);
        if (date == null) {
            return null;
        }
        GregorianCalendar cal = new GregorianCalendar(MODS_TIMEZONE, DATE_LOCALE);
        cal.setTime(date);
        return cal;
    }

    public static void setDate(Element element, Date date, MCRMODSDateFormat encoding) {
        Objects.requireNonNull(encoding, "encoding is required: " + encoding);
        element.setText(encoding.formatDate(date));
        element.setAttribute("encoding", encoding.asEncodingAttributeValue());
    }

    public static void setDate(Element element, GregorianCalendar cal, MCRMODSDateFormat encoding) {
        setDate(element, encoding.isDateOnly() ? adjustTimeOffset(cal) : cal.getTime(), encoding);
    }

    private static Date adjustTimeOffset(GregorianCalendar cal) {
        GregorianCalendar clone = new GregorianCalendar(MODS_TIMEZONE, DATE_LOCALE);
        clone.set(cal.get(GregorianCalendar.YEAR),
            cal.get(GregorianCalendar.MONTH),
            cal.get(GregorianCalendar.DAY_OF_MONTH));
        return clone.getTime();
    }

    private static MCRException reportParseException(String encoding, String text, ParseException ex) {
        String msg = "Unable to parse MODS date encoded " + encoding + " " + text;
        return new MCRException(msg, ex);
    }
}
