package org.mycore.datamodel.common;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;

import org.junit.Test;

public class MCRISO8601FormatChooserTest {

    @Test
    public void formatChooser() {
        // test year
        LocalDate localDate = LocalDate.of(2001, 5, 23);
        ZonedDateTime zonedDateTime = LocalDateTime.of(localDate, LocalTime.of(20, 30, 15)).atZone(ZoneId.of("UTC"));

        String duration = "-16";

        assertEquals(duration + " test failed", MCRISO8601FormatChooser.YEAR_FORMAT.format(localDate),
            getFormat(localDate, duration));
        duration = "2006";
        assertEquals(duration + " test failed", MCRISO8601FormatChooser.YEAR_FORMAT.format(localDate),
            getFormat(localDate, duration));
        // test year-month
        duration = "2006-01";
        assertEquals(duration + " test failed", MCRISO8601FormatChooser.YEAR_MONTH_FORMAT.format(localDate),
            getFormat(localDate, duration));
        // test complete
        duration = "2006-01-18";
        assertEquals(duration + " test failed", MCRISO8601FormatChooser.COMPLETE_FORMAT.format(localDate),
            getFormat(localDate, duration));
        // test complete with hour and minutes
        duration = "2006-01-18T11:08Z";
        assertEquals(duration + " test failed", MCRISO8601FormatChooser.COMPLETE_HH_MM_FORMAT.format(zonedDateTime),
            getFormat(zonedDateTime, duration));
        duration = "2006-01-18T11:08+02:00";
        assertEquals(duration + " test failed", MCRISO8601FormatChooser.COMPLETE_HH_MM_FORMAT.format(zonedDateTime),
            getFormat(zonedDateTime, duration));
        // test complete with hour, minutes and seconds
        duration = "2006-01-18T11:08:20Z";
        assertEquals(duration + " test failed", MCRISO8601FormatChooser.COMPLETE_HH_MM_SS_FORMAT.format(zonedDateTime),
            getFormat(zonedDateTime, duration));
        duration = "2006-01-18T11:08:20+02:00";
        assertEquals(duration + " test failed", MCRISO8601FormatChooser.COMPLETE_HH_MM_SS_FORMAT.format(zonedDateTime),
            getFormat(zonedDateTime, duration));
        // test complete with hour, minutes, seconds and fractions of a second
        duration = "2006-01-18T11:08:20.1Z";
        assertEquals(duration + " test failed",
            MCRISO8601FormatChooser.COMPLETE_HH_MM_SS_SSS_FORMAT.format(zonedDateTime),
            getFormat(zonedDateTime, duration));
        duration = "2006-01-18T11:08:20.12Z";
        assertEquals(duration + " test failed",
            MCRISO8601FormatChooser.COMPLETE_HH_MM_SS_SSS_FORMAT.format(zonedDateTime),
            getFormat(zonedDateTime, duration));
        duration = "2006-01-18T11:08:20.123Z";
        assertEquals(duration + " test failed",
            MCRISO8601FormatChooser.COMPLETE_HH_MM_SS_SSS_FORMAT.format(zonedDateTime),
            getFormat(zonedDateTime, duration));
        duration = "2006-01-18T11:08:20.1+02:00";
        assertEquals(duration + " test failed",
            MCRISO8601FormatChooser.COMPLETE_HH_MM_SS_SSS_FORMAT.format(zonedDateTime),
            getFormat(zonedDateTime, duration));
        duration = "2006-01-18T11:08:20.12+02:00";
        assertEquals(duration + " test failed",
            MCRISO8601FormatChooser.COMPLETE_HH_MM_SS_SSS_FORMAT.format(zonedDateTime),
            getFormat(zonedDateTime, duration));
        duration = "2006-01-18T11:08:20.123+02:00";
        assertEquals(duration + " test failed",
            MCRISO8601FormatChooser.COMPLETE_HH_MM_SS_SSS_FORMAT.format(zonedDateTime),
            getFormat(zonedDateTime, duration));
    }

    private String getFormat(TemporalAccessor compareDate, String duration) {
        return MCRISO8601FormatChooser.getFormatter(duration, null).format(compareDate);
    }

}
