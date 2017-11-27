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

package org.mycore.backend.jpa;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

public class MCRZonedDateTimeConverterTest {

    @Test
    public void convertToEntityAttribute() throws Exception {
        MCRZonedDateTimeConverter c = new MCRZonedDateTimeConverter();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = format.parse("2001-01-01T14:00:00Z");
        ZonedDateTime zonedDateTime = c.convertToEntityAttribute(date);
        ZonedDateTime compareDateTime = ZonedDateTime.of(2001, 1, 1, 14, 0, 0, 0, ZoneId.of("UTC"));
        assertEquals(zonedDateTime.toInstant().getEpochSecond(), compareDateTime.toInstant().getEpochSecond());
    }

}
