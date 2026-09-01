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

package org.mycore.common.date;

import java.time.ZoneId;
import java.util.Locale;

/**
 * Utility class providing functions commonly used by implementations of {@link MCRDateFormatter}.
 */
public final class MCRDateFormatterUtils {

    public static final String ROOT = "ROOT";

    public static final String DEFAULT = "DEFAULT";

    private MCRDateFormatterUtils() {
    }

    /**
     * Utility function to parse a string representation of {@link Locale}
     * using {@link Locale#forLanguageTag(String)} and supporting
     * special values {@link MCRDateFormatterUtils#ROOT} for {@link Locale#ROOT}
     * as well as {@link MCRDateFormatterUtils#DEFAULT} for {@link Locale#getDefault()}.
     * <p>
     * Supports the BCP 47 format (<code>de-DE</code>) and tries to support the
     * POSIX / IETF format (<code>de_DE</code>) by converting all occurrences
     * of <code>_</code> into <code>-</code>.
     */
    public static Locale getLocale(String locale) {
        return switch (locale) {
            case null -> Locale.getDefault();
            case ROOT -> Locale.ROOT;
            case DEFAULT -> Locale.getDefault();
            case String languageTag -> Locale.forLanguageTag(languageTag.replace("_", "-"));
        };
    }

    /**
     * Utility function to parse a string representation of {@link ZoneId}
     * using {@link ZoneId#of(String)} and supporting the special value
     * {@link MCRDateFormatterUtils#DEFAULT} for {@link ZoneId#systemDefault()}.
     * <p>
     * Supports the IANA Time Zone Database format (<code>Europe/Berlin</code>).
     */
    public static ZoneId getTimeZone(String timeZone) {
        return switch (timeZone) {
            case null -> ZoneId.systemDefault();
            case DEFAULT -> ZoneId.systemDefault();
            case String zoneId -> ZoneId.of(zoneId);
        };
    }

}
