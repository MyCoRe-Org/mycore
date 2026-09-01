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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneId;
import java.util.Locale;

import org.junit.jupiter.api.Test;

public class MCRDateFormatterUtilsTest {

    @Test
    public void nullLocale() {

        Locale locale = MCRDateFormatterUtils.getLocale(null);
        assertEquals(Locale.getDefault(), locale);

    }

    @Test
    public void defaultLocale() {

        Locale locale = MCRDateFormatterUtils.getLocale(MCRDateFormatterUtils.DEFAULT);
        assertEquals(Locale.getDefault(), locale);

    }

    @Test
    public void rootLocale() {

        Locale locale = MCRDateFormatterUtils.getLocale(MCRDateFormatterUtils.ROOT);
        assertEquals(Locale.ROOT, locale);

    }

    @Test
    public void posixLocale() {

        Locale locale = MCRDateFormatterUtils.getLocale("de_DE");
        assertEquals(Locale.of("de", "DE"), locale);

    }

    @Test
    public void nullTimeZone() {

        ZoneId zoneId = MCRDateFormatterUtils.getTimeZone(null);
        assertEquals(ZoneId.systemDefault(), zoneId);

    }

    @Test
    public void defaultTimeZone() {

        ZoneId zoneId = MCRDateFormatterUtils.getTimeZone(MCRDateFormatterUtils.DEFAULT);
        assertEquals(ZoneId.systemDefault(), zoneId);

    }

    @Test
    public void ianaTimeZone() {

        ZoneId zoneId = MCRDateFormatterUtils.getTimeZone("Europe/Berlin");
        assertEquals(ZoneId.of("Europe/Berlin"), zoneId);

    }

}
