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
