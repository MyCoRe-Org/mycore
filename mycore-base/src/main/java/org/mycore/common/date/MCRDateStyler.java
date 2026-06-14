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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

import de.thetaphi.forbiddenapis.SuppressForbidden;

/**
 * A {@link MCRISO8601DateFormatter} is a {@link MCRDateFormatter} that uses
 * {@link DateTimeFormatter#format(TemporalAccessor)}, instantiated with
 * {@link DateTimeFormatter#ofLocalizedDateTime(FormatStyle)}, for format a date.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRDateStyler#DATE_STYLE_KEY} can be used to
 * specify the date style.
 * <li> The property suffix {@link MCRDateStyler#LOCALE_KEY} can be used to
 * specify the locale (optional, defaults to {@link Locale#getDefault()};
 * special values <code>DEFAULT</code> and <code>ROOT</code> can be used to set
 * {@link Locale#getDefault()} and {@link Locale#ROOT} respectively).
 * <li> The property suffix {@link MCRDateStyler#TIME_ZONE_KEY}can be used to
 * specify the timezone (optional, defaults to {@link ZoneId#systemDefault()};
 * special value <code>DEFAULT</code> can be used to set {@link ZoneId#systemDefault()}).
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.common.date.MCRDateTimeFormatter
 * [...].Format=yyyy-MM-dd'T'HH:mm
 * [...].Locale=de_DE
 * [...].TimeZone=Europe/Berlin
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRDateStyler.Factory.class)
public final class MCRDateStyler extends MCRInstantFormatterBase {

    public static final String DATE_STYLE_KEY = "DateStyle";

    public static final String LOCALE_KEY = "Locale";

    public static final String TIME_ZONE_KEY = "TimeZone";

    private final DateTimeFormatter formatter;

    public MCRDateStyler(FormatStyle dateStyle) {
        this(dateStyle, Locale.getDefault(), ZoneId.systemDefault());
    }

    public MCRDateStyler(FormatStyle dateStyle, Locale locale) {
        this(dateStyle, locale, ZoneId.systemDefault());
    }

    @SuppressForbidden
    public MCRDateStyler(FormatStyle dateStyle, Locale locale, ZoneId zoneId) {
        Objects.requireNonNull(dateStyle, "Date style must not be null");
        Objects.requireNonNull(locale, "Locale must not be null");
        Objects.requireNonNull(zoneId, "Zone ID must not be null");
        this.formatter = DateTimeFormatter.ofLocalizedDate(dateStyle).localizedBy(locale).withZone(zoneId);
    }

    @Override
    public String format(Instant instant) {

        return formatter.format(instant);
    }

    public static final class Factory implements Supplier<MCRDateStyler> {

        @MCRProperty(name = DATE_STYLE_KEY)
        public String dateStyle;

        @MCRProperty(name = LOCALE_KEY, required = false)
        public String locale;

        @MCRProperty(name = TIME_ZONE_KEY, required = false)
        public String timeZone;

        @Override
        public MCRDateStyler get() {
            return new MCRDateStyler(FormatStyle.valueOf(dateStyle), getLocale(locale), getTimeZone(timeZone));
        }

    }

}
