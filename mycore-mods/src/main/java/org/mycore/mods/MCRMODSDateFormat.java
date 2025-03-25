/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import org.mycore.datamodel.common.MCRISO8601Date;

/**
 * Different supported MODS date formats.
 * @author Thomas Scheffler
 * @since 2015.03
 *
 */
public enum MCRMODSDateFormat {
    ISO_8601("iso8601", null),
    W3C_DTF_10("w3cdtf-10", "yyyy-MM-dd"),
    W3C_DTF_19("w3cdtf-19",
        "yyyy-MM-dd'T'HH:mm:ss"),
    MARC_4("marc-4", "yyyy"),
    ISO_8601_4("iso8601-4", "yyyy"),
    ISO_8601_7("iso8601-7",
        "yyyy-MM"),
    ISO_8601_8("iso8601-8", "yyyyMMdd"),
    ISO_8601_15("iso8601-15", "yyyyMMdd'T'HHmmss"),

    // Try to guess encoding from date length 
    UNKNOWN_4("unknown-4", "yyyy"),
    UNKNOWN_8("unknown-8", "yyyyMMdd"),
    UNKNOWN_10("unknown-10",
        "yyyy-MM-dd"),
    UNKNOWN_19("unknown-19", "yyyy-MM-dd'T'HH:mm:ss"),
    UNKNOWN_15("unknown-15", "yyyyMMdd'T'HHmmss");

    private static volatile Map<String, MCRMODSDateFormat> encodingToFormatMap;

    private final String encoding;

    private final String attributeValue;

    private final String dateFormat;

    final boolean dateOnly;

    static final TimeZone MODS_TIMEZONE = TimeZone.getTimeZone("UTC");

    static final Locale DATE_LOCALE = Locale.ROOT;

    MCRMODSDateFormat(String encoding, String dateFormat) {
        this.encoding = encoding;
        this.attributeValue = Objects.equals(encoding, "iso8601") ? encoding : encoding.split("-")[0];
        this.dateFormat = dateFormat;
        this.dateOnly = dateFormat != null && !dateFormat.endsWith("ss"); //see above
    }

    @Deprecated
    public static MCRMODSDateFormat getFormat(String encoding) {
        return obtainInstance(encoding);
    }

    public static MCRMODSDateFormat obtainInstance(String encoding) {
        if (encodingToFormatMap == null) {
            synchronized (MCRMODSDateFormat.class) {
                if (encodingToFormatMap == null) {
                    initMap();
                }
            }
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
        if (this == ISO_8601) {
            MCRISO8601Date isoDate = new MCRISO8601Date(text);
            return isoDate.getDate();
        }
        return getDateFormat().parse(text);
    }

    public String formatDate(Date date) {
        if (this == ISO_8601) {
            MCRISO8601Date isoDate = new MCRISO8601Date();
            isoDate.setDate(date);
            return isoDate.getISOString();
        }
        return getDateFormat().format(date);
    }

    public SimpleDateFormat getDateFormat() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(this.dateFormat, DATE_LOCALE);
        dateFormat.setTimeZone(MODS_TIMEZONE);
        return dateFormat;
    }
}
