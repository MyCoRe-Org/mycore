package org.mycore.datamodel.metadata;

import java.io.IOException;
import java.util.Date;

import junit.framework.TestCase;

import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.joda.time.format.DateTimeFormatter;

public class MCRMetaISO8601DateTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testFormatChooser() {
        // test year
        String duration = "2006";
        assertEquals(duration + " test failed", getFormat(MCRMetaISO8601Date.FormatChooser.YEAR_FORMAT), getFormat(MCRMetaISO8601Date.FormatChooser
                .getFormatter(duration, null)));
        // test year-month
        duration = "2006-01";
        assertEquals(duration + " test failed", getFormat(MCRMetaISO8601Date.FormatChooser.YEAR_MONTH_FORMAT), getFormat(MCRMetaISO8601Date.FormatChooser
                .getFormatter(duration, null)));
        // test complete
        duration = "2006-01-18";
        assertEquals(duration + " test failed", getFormat(MCRMetaISO8601Date.FormatChooser.COMPLETE_FORMAT), getFormat(MCRMetaISO8601Date.FormatChooser
                .getFormatter(duration, null)));
        // test complete with hour and minutes
        duration = "2006-01-18T11:08Z";
        assertEquals(duration + " test failed", getFormat(MCRMetaISO8601Date.FormatChooser.COMPLETE_HH_MM_FORMAT), getFormat(MCRMetaISO8601Date.FormatChooser
                .getFormatter(duration, null)));
        duration = "2006-01-18T11:08+02:00";
        assertEquals(duration + " test failed", getFormat(MCRMetaISO8601Date.FormatChooser.COMPLETE_HH_MM_FORMAT), getFormat(MCRMetaISO8601Date.FormatChooser
                .getFormatter(duration, null)));
        // test complete with hour, minutes and seconds
        duration = "2006-01-18T11:08:20Z";
        assertEquals(duration + " test failed", getFormat(MCRMetaISO8601Date.FormatChooser.COMPLETE_HH_MM_SS_FORMAT),
                getFormat(MCRMetaISO8601Date.FormatChooser.getFormatter(duration, null)));
        duration = "2006-01-18T11:08:20+02:00";
        assertEquals(duration + " test failed", getFormat(MCRMetaISO8601Date.FormatChooser.COMPLETE_HH_MM_SS_FORMAT),
                getFormat(MCRMetaISO8601Date.FormatChooser.getFormatter(duration, null)));
        // test complete with hour, minutes, seconds and fractions of a second
        duration = "2006-01-18T11:08:20.1Z";
        assertEquals(duration + " test failed", getFormat(MCRMetaISO8601Date.FormatChooser.COMPLETE_HH_MM_SS_SSS_FORMAT),
                getFormat(MCRMetaISO8601Date.FormatChooser.getFormatter(duration, null)));
        duration = "2006-01-18T11:08:20.12Z";
        assertEquals(duration + " test failed", getFormat(MCRMetaISO8601Date.FormatChooser.COMPLETE_HH_MM_SS_SSS_FORMAT),
                getFormat(MCRMetaISO8601Date.FormatChooser.getFormatter(duration, null)));
        duration = "2006-01-18T11:08:20.123Z";
        assertEquals(duration + " test failed", getFormat(MCRMetaISO8601Date.FormatChooser.COMPLETE_HH_MM_SS_SSS_FORMAT),
                getFormat(MCRMetaISO8601Date.FormatChooser.getFormatter(duration, null)));
        duration = "2006-01-18T11:08:20.1+02:00";
        assertEquals(duration + " test failed", getFormat(MCRMetaISO8601Date.FormatChooser.COMPLETE_HH_MM_SS_SSS_FORMAT),
                getFormat(MCRMetaISO8601Date.FormatChooser.getFormatter(duration, null)));
        duration = "2006-01-18T11:08:20.12+02:00";
        assertEquals(duration + " test failed", getFormat(MCRMetaISO8601Date.FormatChooser.COMPLETE_HH_MM_SS_SSS_FORMAT),
                getFormat(MCRMetaISO8601Date.FormatChooser.getFormatter(duration, null)));
        duration = "2006-01-18T11:08:20.123+02:00";
        assertEquals(duration + " test failed", getFormat(MCRMetaISO8601Date.FormatChooser.COMPLETE_HH_MM_SS_SSS_FORMAT),
                getFormat(MCRMetaISO8601Date.FormatChooser.getFormatter(duration, null)));
    }

    /*
     * Test method for
     * 'org.mycore.datamodel.metadata.MCRMetaTimestamp.getSecond()'
     */
    public void testsetDate() {
        MCRMetaISO8601Date ts = new MCRMetaISO8601Date();
        String timeString = "1997-07-16T19:20:30.452300+01:00";
        System.out.println(timeString);
        ts.setDate(timeString);
        assertNotNull("Date is null", ts.getDate());
        // this can be a different String, but point in time should be the same
        System.out.println(ts.getISOString());
        ts.setFormat(MCRMetaISO8601Date.COMPLETE_HH_MM);
        System.out.println(ts.getISOString());
        // wrong date format for the following string should null the internal
        // date.
        timeString = "1997-07-16T19:20:30+01:00";
        System.out.println(timeString);
        ts.setDate(timeString);
        assertNull("Date is not null", ts.getDate());
        ts.setFormat(null); // check auto format determination
        ts.setDate(timeString);
        assertNotNull("Date is null", ts.getDate());
        System.out.println(ts.getISOString());
    }

    /*
     * Test method for
     * 'org.mycore.datamodel.metadata.MCRMetaTimestamp.getSecond()'
     */
    public void testcreateXML() {
        MCRMetaISO8601Date ts = new MCRMetaISO8601Date("service", "servdate", "createdate", 0);
        String timeString = "1997-07-16T19:20:30.452300+01:00";
        ts.setDate(timeString);
        assertNotNull("Date is null", ts.getDate());
        Element export = ts.createXML();
        XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
        try {
            xout.output(export, System.out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testgetFormat() {
        MCRMetaISO8601Date ts = new MCRMetaISO8601Date();
        assertNull("Format used is not Null", ts.getFormat());
        ts.setFormat(MCRMetaISO8601Date.COMPLETE);
        assertEquals("Set format differs from get format", MCRMetaISO8601Date.COMPLETE, ts.getFormat());
    }

    public void testgetDate() {
        MCRMetaISO8601Date ts = new MCRMetaISO8601Date();
        assertNull("Date is not Null", ts.getDate());
        Date dt = new Date();
        ts.setDate(dt);
        assertNotNull("Date is Null", ts.getDate());
        assertEquals("Set date differs from get date", dt, ts.getDate());
    }

    public void testgetISOString() {
        MCRMetaISO8601Date ts = new MCRMetaISO8601Date();
        assertNull("Date is not Null", ts.getISOString());
        Date dt = new Date();
        ts.setDate(dt);
        assertNotNull("Date is Null", ts.getISOString());
    }

    private String getFormat(DateTimeFormatter df) {
        if ((df == null) || (df == MCRMetaISO8601Date.FormatChooser.COMPLETE_HH_MM_SS_SSS_FORMAT)
                || (df == MCRMetaISO8601Date.FormatChooser.UTC_COMPLETE_HH_MM_SS_SSS_FORMAT)) {
            return MCRMetaISO8601Date.COMPLETE_HH_MM_SS_SSS;
        } else if (df == MCRMetaISO8601Date.FormatChooser.COMPLETE_HH_MM_SS_FORMAT || (df == MCRMetaISO8601Date.FormatChooser.UTC_COMPLETE_HH_MM_SS_FORMAT)) {
            return MCRMetaISO8601Date.COMPLETE_HH_MM_SS;
        } else if (df == MCRMetaISO8601Date.FormatChooser.COMPLETE_HH_MM_FORMAT || (df == MCRMetaISO8601Date.FormatChooser.UTC_COMPLETE_HH_MM_FORMAT)) {
            return MCRMetaISO8601Date.COMPLETE_HH_MM;
        } else if (df == MCRMetaISO8601Date.FormatChooser.COMPLETE_FORMAT || (df == MCRMetaISO8601Date.FormatChooser.UTC_COMPLETE_FORMAT)) {
            return MCRMetaISO8601Date.COMPLETE;
        } else if (df == MCRMetaISO8601Date.FormatChooser.YEAR_MONTH_FORMAT || (df == MCRMetaISO8601Date.FormatChooser.UTC_YEAR_MONTH_FORMAT)) {
            return MCRMetaISO8601Date.YEAR_MONTH;
        } else if (df == MCRMetaISO8601Date.FormatChooser.YEAR_FORMAT || (df == MCRMetaISO8601Date.FormatChooser.UTC_YEAR_FORMAT)) {
            return MCRMetaISO8601Date.YEAR;
        } else {
            return MCRMetaISO8601Date.COMPLETE_HH_MM_SS_SSS;
        }
    }

}
