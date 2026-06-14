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

    private MCRDateFormatterUtils() {
    }

    public static Locale getLocale(String locale) {
        return switch (locale) {
            case null -> Locale.getDefault();
            case "ROOT" -> Locale.ROOT;
            case "DEFAULT" -> Locale.getDefault();
            case String languageTag -> Locale.forLanguageTag(languageTag);
        };
    }

    public static ZoneId getTimeZone(String timeZone) {
        return switch (timeZone) {
            case null -> ZoneId.systemDefault();
            case "DEFAULT" -> ZoneId.systemDefault();
            case String zoneId -> ZoneId.of(zoneId);
        };
    }

}
