/**
 * 
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
