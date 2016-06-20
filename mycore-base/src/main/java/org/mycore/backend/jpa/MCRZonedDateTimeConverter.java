/**
 * 
 */
package org.mycore.backend.jpa;

import java.time.Instant;
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
        Instant instant = Instant.from(date);
        return Date.from(instant);
    }

    @Override
    public ZonedDateTime convertToEntityAttribute(Date dbData) {
        Instant instant = dbData.toInstant();
        return ZonedDateTime.from(instant);
    }

}
