package org.mycore.datamodel.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MCRISO8601DateTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void formatChooser() {
        // test year
        String duration = "-16";
        assertEquals(duration + " test failed", getFormat(MCRISO8601FormatChooser.YEAR_FORMAT),
            getFormat(MCRISO8601FormatChooser.getFormatter(duration, null)));
        duration = "2006";
        assertEquals(duration + " test failed", getFormat(MCRISO8601FormatChooser.YEAR_FORMAT),
            getFormat(MCRISO8601FormatChooser.getFormatter(duration, null)));
        // test year-month
        duration = "2006-01";
        assertEquals(duration + " test failed", getFormat(MCRISO8601FormatChooser.YEAR_MONTH_FORMAT),
            getFormat(MCRISO8601FormatChooser.getFormatter(duration, null)));
        // test complete
        duration = "2006-01-18";
        assertEquals(duration + " test failed", getFormat(MCRISO8601FormatChooser.COMPLETE_FORMAT),
            getFormat(MCRISO8601FormatChooser.getFormatter(duration, null)));
        // test complete with hour and minutes
        duration = "2006-01-18T11:08Z";
        assertEquals(duration + " test failed", getFormat(MCRISO8601FormatChooser.COMPLETE_HH_MM_FORMAT),
            getFormat(MCRISO8601FormatChooser.getFormatter(duration, null)));
        duration = "2006-01-18T11:08+02:00";
        assertEquals(duration + " test failed", getFormat(MCRISO8601FormatChooser.COMPLETE_HH_MM_FORMAT),
            getFormat(MCRISO8601FormatChooser.getFormatter(duration, null)));
        // test complete with hour, minutes and seconds
        duration = "2006-01-18T11:08:20Z";
        assertEquals(duration + " test failed", getFormat(MCRISO8601FormatChooser.COMPLETE_HH_MM_SS_FORMAT),
            getFormat(MCRISO8601FormatChooser.getFormatter(duration, null)));
        duration = "2006-01-18T11:08:20+02:00";
        assertEquals(duration + " test failed", getFormat(MCRISO8601FormatChooser.COMPLETE_HH_MM_SS_FORMAT),
            getFormat(MCRISO8601FormatChooser.getFormatter(duration, null)));
        // test complete with hour, minutes, seconds and fractions of a second
        duration = "2006-01-18T11:08:20.1Z";
        assertEquals(duration + " test failed", getFormat(MCRISO8601FormatChooser.COMPLETE_HH_MM_SS_SSS_FORMAT),
            getFormat(MCRISO8601FormatChooser.getFormatter(duration, null)));
        duration = "2006-01-18T11:08:20.12Z";
        assertEquals(duration + " test failed", getFormat(MCRISO8601FormatChooser.COMPLETE_HH_MM_SS_SSS_FORMAT),
            getFormat(MCRISO8601FormatChooser.getFormatter(duration, null)));
        duration = "2006-01-18T11:08:20.123Z";
        assertEquals(duration + " test failed", getFormat(MCRISO8601FormatChooser.COMPLETE_HH_MM_SS_SSS_FORMAT),
            getFormat(MCRISO8601FormatChooser.getFormatter(duration, null)));
        duration = "2006-01-18T11:08:20.1+02:00";
        assertEquals(duration + " test failed", getFormat(MCRISO8601FormatChooser.COMPLETE_HH_MM_SS_SSS_FORMAT),
            getFormat(MCRISO8601FormatChooser.getFormatter(duration, null)));
        duration = "2006-01-18T11:08:20.12+02:00";
        assertEquals(duration + " test failed", getFormat(MCRISO8601FormatChooser.COMPLETE_HH_MM_SS_SSS_FORMAT),
            getFormat(MCRISO8601FormatChooser.getFormatter(duration, null)));
        duration = "2006-01-18T11:08:20.123+02:00";
        assertEquals(duration + " test failed", getFormat(MCRISO8601FormatChooser.COMPLETE_HH_MM_SS_SSS_FORMAT),
            getFormat(MCRISO8601FormatChooser.getFormatter(duration, null)));
    }

    @Test
    public void getDate() {
        MCRISO8601Date ts = new MCRISO8601Date();
        assertNull("Date is not Null", ts.getDate());
        Date dt = new Date();
        ts.setDate(dt);
        assertNotNull("Date is Null", ts.getDate());
        assertEquals("Set date differs from get date", dt, ts.getDate());
    }

    @Test
    public void getFormat() {
        MCRISO8601Date ts = new MCRISO8601Date();
        assertNull("Format used is not Null", ts.getIsoFormat());
        ts.setFormat(MCRISO8601Format.COMPLETE);
        assertEquals("Set format differs from get format", MCRISO8601Format.COMPLETE, ts.getIsoFormat());
    }

    private String getFormat(DateTimeFormatter df) {
        if (df == null || df == MCRISO8601FormatChooser.COMPLETE_HH_MM_SS_SSS_FORMAT
            || df == MCRISO8601FormatChooser.UTC_COMPLETE_HH_MM_SS_SSS_FORMAT) {
            return MCRISO8601Format.COMPLETE_HH_MM_SS_SSS.toString();
        } else if (df == MCRISO8601FormatChooser.COMPLETE_HH_MM_SS_FORMAT
            || df == MCRISO8601FormatChooser.UTC_COMPLETE_HH_MM_SS_FORMAT) {
            return MCRISO8601Format.COMPLETE_HH_MM_SS.toString();
        } else if (df == MCRISO8601FormatChooser.COMPLETE_HH_MM_FORMAT
            || df == MCRISO8601FormatChooser.UTC_COMPLETE_HH_MM_FORMAT) {
            return MCRISO8601Format.COMPLETE_HH_MM.toString();
        } else if (df == MCRISO8601FormatChooser.COMPLETE_FORMAT || df == MCRISO8601FormatChooser.UTC_COMPLETE_FORMAT) {
            return MCRISO8601Format.COMPLETE.toString();
        } else if (df == MCRISO8601FormatChooser.YEAR_MONTH_FORMAT
            || df == MCRISO8601FormatChooser.UTC_YEAR_MONTH_FORMAT) {
            return MCRISO8601Format.YEAR_MONTH.toString();
        } else if (df == MCRISO8601FormatChooser.YEAR_FORMAT || df == MCRISO8601FormatChooser.UTC_YEAR_FORMAT) {
            return MCRISO8601Format.YEAR.toString();
        } else {
            return MCRISO8601Format.COMPLETE_HH_MM_SS_SSS.toString();
        }
    }

    @Test
    public void getISOString() {
        MCRISO8601Date ts = new MCRISO8601Date();
        assertNull("Date is not Null", ts.getISOString());
        Date dt = new Date();
        ts.setDate(dt);
        assertNotNull("Date is Null", ts.getISOString());
    }
    
    @Test
    public void testFormat(){
        String year="2015";
        String simpleFormat="yyyy";
        String language="de";
        //simulate MCRXMLFunctions.format();
        Locale locale = new Locale(language);
        MCRISO8601Date mcrdate = new MCRISO8601Date();
        mcrdate.setFormat((String) null);
        mcrdate.setDate(year);
        String formatted = mcrdate.format(simpleFormat, locale, TimeZone.getDefault().getID());
        assertEquals(year, formatted);
    }

    /*
     * Test method for
     * 'org.mycore.datamodel.metadata.MCRMetaTimestamp.getSecond()'
     */
    @Test
    public void setDate() {
        MCRISO8601Date ts = new MCRISO8601Date();
        String timeString = "1997-07-16T19:20:30.452300+01:00";
        System.out.println(timeString);
        ts.setDate(timeString);
        assertNotNull("Date is null", ts.getDate());
        // this can be a different String, but point in time should be the same
        System.out.println(ts.getISOString());
        ts.setFormat(MCRISO8601Format.COMPLETE_HH_MM);
        System.out.println(ts.getISOString());
        // wrong date format for the following string should null the internal
        // date.
        timeString = "1997-07-16T19:20:30+01:00";
        System.out.println(timeString);
        ts.setDate(timeString);
        assertNull("Date is not null", ts.getDate());
        ts.setFormat((String) null); // check auto format determination
        ts.setDate(timeString);
        assertNotNull("Date is null", ts.getDate());
        // check if shorter format declarations fail if String is longer
        ts.setFormat(MCRISO8601Format.YEAR);
        timeString = "1997-07";
        ts.setDate(timeString);
        assertNull("Date is not null", ts.getDate());
        System.out.println(ts.getISOString());
        timeString = "01.12.1986";
        ts.setFormat((String) null);
        ts.setDate(timeString);
        assertNull("Date is not null", ts.getDate());

    }
}