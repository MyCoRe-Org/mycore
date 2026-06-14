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

import static org.mycore.common.date.MCRDateFormatterUtils.getLocale;
import static org.mycore.common.date.MCRDateFormatterUtils.getTimeZone;

import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.datamodel.common.MCRISO8601Date;

/**
 * A {@link MCRISO8601DateFormatter} is a {@link MCRDateFormatter} that uses
 * {@link MCRISO8601Date#format(String, Locale)} to format a date.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRSimpleDateFormatter#FORMAT_KEY} can be used to
 * specify the format.
 * <li> The property suffix {@link MCRSimpleDateFormatter#LOCALE_KEY} can be used to
 * specify the locale (optional, defaults to {@link Locale#getDefault()};
 * special values <code>DEFAULT</code> and <code>ROOT</code> can be used to set
 * {@link Locale#getDefault()} and {@link Locale#ROOT} respectively).
 * <li> The property suffix {@link MCRSimpleDateFormatter#TIME_ZONE_KEY}can be used to
 * specify the timezone (optional, defaults to {@link ZoneId#systemDefault()};
 * special value <code>DEFAULT</code> can be used to set {@link ZoneId#systemDefault()}).
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.common.date.MCRISO8601DateFormatter
 * [...].Format=yyyy-MM-dd'T'HH:mm
 * [...].Locale=de_DE
 * [...].TimeZone=Europe/Berlin
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRISO8601DateFormatter.Factory.class)
public final class MCRISO8601DateFormatter extends MCRDateFormatterBase {

    private final String format;

    private final Locale locale;

    private final String timeZone;

    public MCRISO8601DateFormatter(String format) {
        this(format, Locale.ROOT, ZoneId.systemDefault());
    }

    public MCRISO8601DateFormatter(String format, Locale locale) {
        this(format, locale, ZoneId.systemDefault());
    }

    public MCRISO8601DateFormatter(String format, Locale locale, ZoneId zoneId) {
        this.format = Objects.requireNonNull(format, "Format must not be null");
        this.locale = Objects.requireNonNull(locale, "Locale must not be null");
        this.timeZone = Objects.requireNonNull(zoneId, "Zone ID must not be null").getId();
    }

    @Override
    public String format(Date date) {

        MCRISO8601Date isoDate = new MCRISO8601Date();
        isoDate.setDate(date);

        return isoDate.format(format, locale, timeZone);

    }

    public static final class Factory implements Supplier<MCRISO8601DateFormatter> {

        @MCRProperty(name = "Format")
        public String format;

        @MCRProperty(name = "Locale", required = false)
        public String locale;

        @MCRProperty(name = "TimeZone", required = false)
        public String timeZone;

        @Override
        public MCRISO8601DateFormatter get() {
            return new MCRISO8601DateFormatter(format, getLocale(locale), getTimeZone(timeZone));
        }

    }

}
