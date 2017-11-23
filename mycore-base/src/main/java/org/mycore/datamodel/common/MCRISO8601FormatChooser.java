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

package org.mycore.datamodel.common;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

import java.time.ZoneOffset;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * is a helper class for MCRMetaISO8601Date. Please be aware that this class is not supported. It may disappear some day
 * or methods get removed.
 *
 * @author Thomas Scheffler (yagee)
 * @version $Revision: 18729 $ $Date: 2010-09-21 12:33:45 +0200 (Di, 21. Sep 2010) $
 * @since 1.3
 */
public final class MCRISO8601FormatChooser {

    public static final DateTimeFormatter YEAR_FORMAT = ISODateTimeFormat.year();

    public static final DateTimeFormatter YEAR_MONTH_FORMAT = ISODateTimeFormat.yearMonth();

    public static final DateTimeFormatter COMPLETE_FORMAT = ISODateTimeFormat.date();

    public static final DateTimeFormatter COMPLETE_HH_MM_FORMAT = ISODateTimeFormat.dateHourMinute();

    public static final DateTimeFormatter COMPLETE_HH_MM_SS_FORMAT = ISODateTimeFormat.dateTimeNoMillis();

    public static final DateTimeFormatter COMPLETE_HH_MM_SS_SSS_FORMAT = ISODateTimeFormat.dateTime();

    private static final Pattern MILLI_CHECK_PATTERN = Pattern.compile("\\.\\d{4,}\\+");

    private static final boolean USE_UTC = true;

    /**
     * returns a DateTimeFormatter for the given isoString or format. This method prefers the format parameter. So if
     * it's not null or not zero length this method will interpret the format string. You can also get a formatter for e
     * specific iso String. In either case if the underlying algorithm can not determine an exact matching formatter it
     * will allway fall back to a default. So this method will never return null.
     *
     * @param isoString
     *            an ISO 8601 formatted time String, or null
     * @param isoFormat
     *            a valid format String, or null
     * @return returns a specific DateTimeFormatter
     */
    public static DateTimeFormatter getFormatter(String isoString, MCRISO8601Format isoFormat) {
        DateTimeFormatter df;
        if (isoFormat != null) {
            df = getFormatterForFormat(isoFormat);
        } else if (isoString != null && isoString.length() != 0) {
            df = getFormatterForDuration(isoString);
        } else {
            df = COMPLETE_HH_MM_SS_SSS_FORMAT;
        }
        if (USE_UTC) {
            df = df.withZone(ZoneOffset.UTC);
        }
        return df;
    }

    private static DateTimeFormatter getFormatterForFormat(MCRISO8601Format isoFormat) {
        switch (isoFormat) {
            case YEAR:
                return YEAR_FORMAT;
            case YEAR_MONTH:
                return YEAR_MONTH_FORMAT;
            case COMPLETE:
                return COMPLETE_FORMAT;
            case COMPLETE_HH_MM:
                return COMPLETE_HH_MM_FORMAT;
            case COMPLETE_HH_MM_SS:
                return COMPLETE_HH_MM_SS_FORMAT;
            case COMPLETE_HH_MM_SS_SSS:
                return COMPLETE_HH_MM_SS_SSS_FORMAT;
            case YEAR_ERA:
                return YEAR_FORMAT;
            case YEAR_MONTH_ERA:
                return YEAR_MONTH_FORMAT;
            case COMPLETE_ERA:
                return COMPLETE_FORMAT;
            case COMPLETE_HH_MM_ERA:
                return COMPLETE_HH_MM_FORMAT;
            case COMPLETE_HH_MM_SS_ERA:
                return COMPLETE_HH_MM_SS_FORMAT;
            case COMPLETE_HH_MM_SS_SSS_ERA:
                return COMPLETE_HH_MM_SS_SSS_FORMAT;
            default:
                return COMPLETE_HH_MM_SS_SSS_FORMAT;
        }
    }

    private static DateTimeFormatter getFormatterForDuration(String isoString) {
        boolean test = false;
        switch (isoString.length()) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                return YEAR_FORMAT;
            case 6:
            case 7:
            case 8:
                return YEAR_MONTH_FORMAT;
            case 10:
            case 11:
                return COMPLETE_FORMAT;
            case 17: // YYYY-MM-DDThh:mm'Z'
                test = true;
            case 22:
                if (test || !isoString.endsWith("Z")) {
                    // YYYY-MM-DDThh:mm[+-]hh:mm
                    return COMPLETE_HH_MM_FORMAT;
                }
                // YYYY-MM-DDThh:mm:ss.s'Z'
                return COMPLETE_HH_MM_SS_SSS_FORMAT;
            case 20: // YYYY-MM-DDThh:mm:ss'Z'
            case 25: // YYYY-MM-DDThh:mm:ss[+-]hh:mm
                return COMPLETE_HH_MM_SS_FORMAT;
            case 23: // YYYY-MM-DDThh:mm:ss.ss'Z'
            case 24: // YYYY-MM-DDThh:mm:ss.sss'Z'
            case 27: // YYYY-MM-DDThh:mm:ss.s[+-]hh:mm
            case 28: // YYYY-MM-DDThh:mm:ss.ss[+-]hh:mm
            case 29: // YYYY-MM-DDThh:mm:ss.ss[+-]hh:mm
                return COMPLETE_HH_MM_SS_SSS_FORMAT;
            default:
                return COMPLETE_HH_MM_SS_SSS_FORMAT;
        }
    }

    /**
     * returns a String that has not more than 3 digits representing "fractions of a second". If isoString has no or not
     * more than 3 digits this method simply returns isoString.
     *
     * @param isoString
     *            an ISO 8601 formatted time String
     * @return an ISO 8601 formatted time String with at max 3 digits for fractions of a second
     */
    public static String cropSecondFractions(String isoString) {
        Matcher matcher = MILLI_CHECK_PATTERN.matcher(isoString);
        boolean result = matcher.find();
        if (result) {
            return matcher.replaceFirst(isoString.substring(matcher.start(), matcher.start() + 4) + "+");
        }
        return isoString;
    }

    private static class ISODateTimeFormat {

        public static DateTimeFormatter year() {
            return DateTimeFormatter.ofPattern("uuuu", Locale.ROOT);
        }

        public static DateTimeFormatter yearMonth() {
            return DateTimeFormatter.ofPattern("uuuu-MM", Locale.ROOT);
        }

        public static DateTimeFormatter date() {
            return DateTimeFormatter.ISO_LOCAL_DATE;
        }

        public static DateTimeFormatter dateHourMinute() {
            return new DateTimeFormatterBuilder().parseCaseInsensitive()
                .appendValue(YEAR, 4)
                .appendLiteral('-')
                .appendValue(MONTH_OF_YEAR, 2)
                .appendLiteral('-')
                .appendValue(DAY_OF_MONTH, 2)
                .appendLiteral('T')
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .appendOffset("+HH:MM", "Z")
                .toFormatter(Locale.ROOT)
                .withChronology(IsoChronology.INSTANCE)
                .withResolverStyle(ResolverStyle.STRICT);
        }

        public static DateTimeFormatter dateTimeNoMillis() {
            return new DateTimeFormatterBuilder().parseCaseInsensitive()
                .appendValue(YEAR, 4)
                .appendLiteral('-')
                .appendValue(MONTH_OF_YEAR, 2)
                .appendLiteral('-')
                .appendValue(DAY_OF_MONTH, 2)
                .appendLiteral('T')
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                .appendOffset("+HH:MM", "Z")
                .toFormatter(Locale.ROOT)
                .withChronology(IsoChronology.INSTANCE)
                .withResolverStyle(ResolverStyle.STRICT);
        }

        public static DateTimeFormatter dateTime() {
            return new DateTimeFormatterBuilder().parseCaseInsensitive()
                .appendValue(YEAR, 4)
                .appendLiteral('-')
                .appendValue(MONTH_OF_YEAR, 2)
                .appendLiteral('-')
                .appendValue(DAY_OF_MONTH, 2)
                .appendLiteral('T')
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                .appendFraction(ChronoField.NANO_OF_SECOND, 3, 3, true)
                .appendOffset("+HH:MM", "Z")
                .toFormatter(Locale.ROOT)
                .withChronology(IsoChronology.INSTANCE)
                .withResolverStyle(ResolverStyle.STRICT);
        }
    }

}
