/*
 * $Revision: 30923 $ $Date: 2014-10-22 10:54:47 +0200 (Mi, 22 Okt 2014) $
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.mycore.datamodel.common.MCRISO8601Date;

/**
 * Different supported MODS date formats.
 * @author Thomas Scheffler
 * @since 2015.03
 *
 */
public enum MCRMODSDateFormat {
    iso8601("iso8601", null), w3cdtf_10("w3cdtf-10", "yyyy-MM-dd"), w3cdtf_19("w3cdtf-19",
        "yyyy-MM-dd'T'HH:mm:ss"), marc_4("marc-4", "yyyy"), iso8601_4("iso8601-4", "yyyy"), iso8601_7("iso8601-7",
            "yyyy-MM"), iso8601_8("iso8601-8", "yyyyMMdd"), iso8601_15("iso8601-15", "yyyyMMdd'T'HHmmss"),

    // Try to guess encoding from date length 
    unknown_4("unknown-4", "yyyy"), unknown_8("unknown-8", "yyyyMMdd"), unknown_10("unknown-10",
        "yyyy-MM-dd"), unknown_19("unknown-19", "yyyy-MM-dd'T'HH:mm:ss"), unknown_15("unknown-15", "yyyyMMdd'T'HHmmss");
    private static volatile Map<String, MCRMODSDateFormat> encodingToFormatMap;

    private String encoding, attributeValue;

    private String dateFormat;

    boolean dateOnly;

    static final TimeZone MODS_TIMEZONE = TimeZone.getTimeZone("UTC");

    static final Locale DATE_LOCALE = Locale.ROOT;

    private MCRMODSDateFormat(String encoding, String dateFormat) {
        this.encoding = encoding;
        this.attributeValue = encoding == "iso8601" ? encoding : encoding.split("-")[0];
        this.dateFormat = dateFormat;
        this.dateOnly = dateFormat != null && !dateFormat.endsWith("ss"); //see above
    }

    public static MCRMODSDateFormat getFormat(String encoding) {
        if (encodingToFormatMap == null) {
            initMap();
        }
        return encodingToFormatMap.get(encoding);
    }

    private static void initMap() {
        Map<String, MCRMODSDateFormat> encodingToFormatMap = new HashMap<>();
        for (MCRMODSDateFormat f : values()) {
            encodingToFormatMap.put(f.encoding, f);
        }
        MCRMODSDateFormat.encodingToFormatMap = encodingToFormatMap;
    }

    public String getEncoding() {
        return encoding;
    }

    public boolean isDateOnly() {
        return dateOnly;
    }

    public String asEncodingAttributeValue() {
        return attributeValue;
    }

    public Date parseDate(String text) throws ParseException {
        if (this == iso8601) {
            MCRISO8601Date isoDate = new MCRISO8601Date(text);
            return isoDate.getDate();
        }
        return getDateFormat().parse(text);
    }

    public String formatDate(Date date) {
        if (this == iso8601) {
            MCRISO8601Date isoDate = new MCRISO8601Date();
            isoDate.setDate(date);
            return isoDate.getISOString();
        }
        return getDateFormat().format(date);
    }

    public SimpleDateFormat getDateFormat() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(this.dateFormat, MCRMODSDateFormat.DATE_LOCALE);
        dateFormat.setTimeZone(MCRMODSDateFormat.MODS_TIMEZONE);
        return dateFormat;
    }
}
