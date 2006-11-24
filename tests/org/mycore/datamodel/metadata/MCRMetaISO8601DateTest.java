package org.mycore.datamodel.metadata;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.joda.time.format.DateTimeFormatter;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRTestCase;

public class MCRMetaISO8601DateTest extends MCRTestCase {
    private static Logger LOGGER;

    protected void setUp() throws Exception {
        super.setUp();//org.mycore.datamodel.metadata.MCRMetaISO8601Date
        if (setProperty("MCR.log4j.logger.org.mycore.datamodel.metadata.MCRMetaISO8601Date","INFO", false)){
            //DEBUG will print a Stacktrace if we test for errors, but that's O.K.
            MCRConfiguration.instance().configureLogging();
        }
        if (LOGGER == null) {
            LOGGER = Logger.getLogger(MCRMetaISO8601DateTest.class);
        }
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
        LOGGER.debug(timeString);
        ts.setDate(timeString);
        assertNotNull("Date is null", ts.getDate());
        // this can be a different String, but point in time should be the same
        LOGGER.debug(ts.getISOString());
        ts.setFormat(MCRMetaISO8601Date.IsoFormat.COMPLETE_HH_MM);
        LOGGER.debug(ts.getISOString());
        // wrong date format for the following string should null the internal
        // date.
        timeString = "1997-07-16T19:20:30+01:00";
        LOGGER.debug(timeString);
        ts.setDate(timeString);
        assertNull("Date is not null", ts.getDate());
        ts.setFormat((String)null); // check auto format determination
        ts.setDate(timeString);
        assertNotNull("Date is null", ts.getDate());
        // check if shorter format declarations fail if String is longer
        ts.setFormat(MCRMetaISO8601Date.IsoFormat.YEAR);
        timeString = "1997-07";
        ts.setDate(timeString);
        assertNull("Date is not null", ts.getDate());
        LOGGER.debug(ts.getISOString());
        timeString = "01.12.1986";
        ts.setFormat((String)null);
        ts.setDate(timeString);
        assertNull("Date is not null", ts.getDate());
        setProperty("MCR.SimpleDateFormat.strictParsing","false",true);
        setProperty("MCR.SimpleDateFormat.locales","de_DE,en_US",true);
        ts.setFormat((String)null);
        ts.setDate(timeString);
        LOGGER.debug(ts.getISOString());
        timeString = "12/01/1986";
        ts.setDate(timeString);
        LOGGER.debug(ts.getISOString());
        //assertNotNull("Date is null", ts.getDate());
        setProperty("MCR.SimpleDateFormat.strictParsing","true",true);
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
        if (LOGGER.isDebugEnabled()) {
            XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
            StringWriter sw = new StringWriter();
            try {
                xout.output(export, sw);
                LOGGER.debug(sw.toString());
            } catch (IOException e) {
                LOGGER.warn("Failure printing xml result", e);
            }
        }
    }

    public void testgetFormat() {
        MCRMetaISO8601Date ts = new MCRMetaISO8601Date();
        assertNull("Format used is not Null", ts.getFormat());
        ts.setFormat(MCRMetaISO8601Date.IsoFormat.COMPLETE);
        assertEquals("Set format differs from get format", MCRMetaISO8601Date.IsoFormat.COMPLETE, ts.getFormat());
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

    public void testsetFromDOM() {
        MCRMetaISO8601Date ts = new MCRMetaISO8601Date();
        Element datum = new Element("datum");
        datum.setAttribute("inherited", "0").setText("2006-01-23");
        ts.setFromDOM(datum);
        assertEquals("Dates not equal", "2006-01-23", ts.getISOString());
        datum.setAttribute("format", MCRMetaISO8601Date.IsoFormat.COMPLETE_HH_MM.toString());
        ts.setFromDOM(datum);
        assertNull("Date should be null", ts.getDate());
        assertEquals("Format should be set by jdom", MCRMetaISO8601Date.IsoFormat.COMPLETE_HH_MM, ts.getFormat());
    }
    
    private String getFormat(DateTimeFormatter df) {
        if ((df == null) || (df == MCRMetaISO8601Date.FormatChooser.COMPLETE_HH_MM_SS_SSS_FORMAT)
                || (df == MCRMetaISO8601Date.FormatChooser.UTC_COMPLETE_HH_MM_SS_SSS_FORMAT)) {
            return MCRMetaISO8601Date.IsoFormat.COMPLETE_HH_MM_SS_SSS.toString();
        } else if (df == MCRMetaISO8601Date.FormatChooser.COMPLETE_HH_MM_SS_FORMAT || (df == MCRMetaISO8601Date.FormatChooser.UTC_COMPLETE_HH_MM_SS_FORMAT)) {
            return MCRMetaISO8601Date.IsoFormat.COMPLETE_HH_MM_SS.toString();
        } else if (df == MCRMetaISO8601Date.FormatChooser.COMPLETE_HH_MM_FORMAT || (df == MCRMetaISO8601Date.FormatChooser.UTC_COMPLETE_HH_MM_FORMAT)) {
            return MCRMetaISO8601Date.IsoFormat.COMPLETE_HH_MM.toString();
        } else if (df == MCRMetaISO8601Date.FormatChooser.COMPLETE_FORMAT || (df == MCRMetaISO8601Date.FormatChooser.UTC_COMPLETE_FORMAT)) {
            return MCRMetaISO8601Date.IsoFormat.COMPLETE.toString();
        } else if (df == MCRMetaISO8601Date.FormatChooser.YEAR_MONTH_FORMAT || (df == MCRMetaISO8601Date.FormatChooser.UTC_YEAR_MONTH_FORMAT)) {
            return MCRMetaISO8601Date.IsoFormat.YEAR_MONTH.toString();
        } else if (df == MCRMetaISO8601Date.FormatChooser.YEAR_FORMAT || (df == MCRMetaISO8601Date.FormatChooser.UTC_YEAR_FORMAT)) {
            return MCRMetaISO8601Date.IsoFormat.YEAR.toString();
        } else {
            return MCRMetaISO8601Date.IsoFormat.COMPLETE_HH_MM_SS_SSS.toString();
        }
    }

}