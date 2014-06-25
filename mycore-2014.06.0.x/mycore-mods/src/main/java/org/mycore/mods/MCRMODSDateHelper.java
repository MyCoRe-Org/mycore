/*
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.mods;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.datamodel.common.MCRISO8601Date;

/**
 * Helper class to parse and build MODS date elements, see
 * http://www.loc.gov/standards/mods/userguide/generalapp.html#encoding
 *  
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRMODSDateHelper {

    private static final String ISO8601 = "iso8601";

    private final static Map<String, String> formats = new HashMap<String, String>();

    static {
        formats.put(ISO8601, null);
        formats.put("w3cdtf-10", "yyyy-MM-dd");
        formats.put("w3cdtf-19", "yyyy-MM-dd'T'HH:mm:ss");
        formats.put("marc-4", "yyyy");
        formats.put("iso8601-4", "yyyy");
        formats.put("iso8601-7", "yyyy-MM");
        formats.put("iso8601-8", "yyyyMMdd");
        formats.put("iso8601-15", "yyyyMMdd'T'HHmmss");

        // Try to guess encoding from date length 
        formats.put("unknown-4", "yyyy");
        formats.put("unknown-8", "yyyyMMdd");
        formats.put("unknown-10", "yyyy-MM-dd");
        formats.put("unknown-19", "yyyy-MM-dd'T'HH:mm:ss");
        formats.put("unknown-15", "yyyyMMdd'T'HHmmss");
    }

    public static Date getDate(Element element) {
        if (element == null)
            return null;

        String text = element.getTextTrim();
        if ((text == null) || text.isEmpty())
            return null;

        String encoding = element.getAttributeValue("encoding", "unknown").toLowerCase();
        String key = encoding + "-" + text.length();

        String format = formats.get(key);
        if (format == null) {
            if (encoding.equals(ISO8601)) {
                MCRISO8601Date isoDate = new MCRISO8601Date(text);
                return isoDate.getDate();
            }
            throw reportParseException(encoding, text, null);
        }

        try {
            return new SimpleDateFormat(format).parse(text);
        } catch (ParseException ex) {
            throw reportParseException(encoding, text, ex);
        }
    }

    private static MCRException reportParseException(String encoding, String text, ParseException ex) {
        String msg = "Unable to parse MODS date encoded " + encoding + " " + text;
        return new MCRException(msg, ex);
    }

    public static GregorianCalendar getCalendar(Element element) {
        Date date = getDate(element);
        if (date == null) {
            return null;
        }

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        return cal;
    }

    public static void setDate(Element element, Date date, String encoding) {
        String format = formats.get(encoding);
        if (format != null) {
            String text = new SimpleDateFormat(format).format(date);
            element.setText(text);
            encoding = encoding.split("-")[0];
        } else {
            MCRISO8601Date isoDate = new MCRISO8601Date();
            isoDate.setDate(date);
            element.setText(isoDate.getISOString());
        }
        element.setAttribute("encoding", encoding);
    }

    public static void setDate(Element element, GregorianCalendar cal, String encoding) {
        setDate(element, cal.getTime(), encoding);
    }
}
