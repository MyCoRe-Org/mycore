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

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * Converts a {@link ZonedDateTime} to {@link Date} for JPA 2.1.
 * @author Thomas Scheffler (yagee)
 */
@Converter
public class MCRZonedDateTimeConverter implements AttributeConverter<ZonedDateTime, Date> {

    @Override
    public Date convertToDatabaseColumn(ZonedDateTime date) {
        if (date == null) {
            return null;
        }
        Instant instant = Instant.from(date);
        return Date.from(instant);
    }

    @Override
    public ZonedDateTime convertToEntityAttribute(Date dbData) {
        if (dbData == null) {
            return null;
        }
        Instant instant = dbData.toInstant();
        try {
            return ZonedDateTime.from(instant);
        } catch (DateTimeException exc) {
            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
            return ZonedDateTime.of(localDateTime, ZoneId.of("UTC"));
        }
    }

}
