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

package org.mycore.datamodel.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.StringWriter;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.datamodel.common.MCRISO8601Format;

/**
 * This class is a JUnit test case for org.mycore.datamodel.metadata.MCRMeta8601Date.
 * 
 * @author Thomas Scheffler
 * @version $Revision$ $Date$
 *
 */
public class MCRMetaISO8601DateTest extends MCRTestCase {
    private static Logger LOGGER;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();//org.mycore.datamodel.metadata.MCRMetaISO8601Date
        if (LOGGER == null) {
            LOGGER = LogManager.getLogger(MCRMetaISO8601DateTest.class);
        }
    }

    /*
     * Test method for
     * 'org.mycore.datamodel.metadata.MCRMetaTimestamp.getSecond()'
     */
    @Test
    public void setDate() {
        MCRMetaISO8601Date ts = new MCRMetaISO8601Date();
        String timeString = "1997-07-16T19:20:30.452300+01:00";
        LOGGER.debug(timeString);
        ts.setDate(timeString);
        assertNotNull("Date is null", ts.getDate());
        // this can be a different String, but point in time should be the same
        LOGGER.debug(ts.getISOString());
        ts.setFormat(MCRISO8601Format.COMPLETE_HH_MM.toString());
        LOGGER.debug(ts.getISOString());
        // wrong date format for the following string should null the internal
        // date.
        timeString = "1997-07-16T19:20:30+01:00";
        LOGGER.debug(timeString);
        ts.setDate(timeString);
        assertNull("Date is not null", ts.getDate());
        ts.setFormat(null); // check auto format determination
        ts.setDate(timeString);
        assertNotNull("Date is null", ts.getDate());
        // check if shorter format declarations fail if String is longer
        ts.setFormat(MCRISO8601Format.YEAR.toString());
        timeString = "1997-07";
        ts.setDate(timeString);
        assertNull("Date is not null", ts.getDate());
        LOGGER.debug(ts.getISOString());
        timeString = "01.12.1986";
        ts.setFormat(null);
        ts.setDate(timeString);
        assertNull("Date is not null", ts.getDate());
        MCRConfiguration.instance().set("MCR.Metadata.SimpleDateFormat.StrictParsing", "false");
        MCRConfiguration.instance().set("MCR.Metadata.SimpleDateFormat.Locales", "de_DE,en_US");
        ts.setFormat(null);
        ts.setDate(timeString);
        LOGGER.debug(ts.getISOString());
        timeString = "12/01/1986";
        ts.setDate(timeString);
        LOGGER.debug(ts.getISOString());

        ts.setDate("2001");
        assertEquals(2001, ts.getMCRISO8601Date().getDt().get(ChronoField.YEAR));

        // test b.c. 
        ts.setDate("-0312");
        assertEquals(-312, ts.getMCRISO8601Date().getDt().get(ChronoField.YEAR));
        ts.setDate("-0315-05");
        assertEquals(-315, ts.getMCRISO8601Date().getDt().get(ChronoField.YEAR));
        assertEquals(5, ts.getMCRISO8601Date().getDt().get(ChronoField.MONTH_OF_YEAR));
        ts.setDate("-0318-08-12");
        assertEquals(-318, ts.getMCRISO8601Date().getDt().get(ChronoField.YEAR));
        assertEquals(8, ts.getMCRISO8601Date().getDt().get(ChronoField.MONTH_OF_YEAR));
        assertEquals(12, ts.getMCRISO8601Date().getDt().get(ChronoField.DAY_OF_MONTH));
    }

    /*
     * Test method for
     * 'org.mycore.datamodel.metadata.MCRMetaTimestamp.getSecond()'
     */
    @Test
    public void createXML() {
        MCRMetaISO8601Date ts = new MCRMetaISO8601Date("servdate", "createdate", 0);
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

    @Test
    public void getFormat() {
        MCRMetaISO8601Date ts = new MCRMetaISO8601Date();
        assertNull("Format used is not Null", ts.getFormat());
        ts.setFormat(MCRISO8601Format.COMPLETE.toString());
        assertEquals("Set format differs from get format", MCRISO8601Format.COMPLETE.toString(), ts.getFormat());
    }

    @Test
    public void getDate() {
        MCRMetaISO8601Date ts = new MCRMetaISO8601Date();
        assertNull("Date is not Null", ts.getDate());
        Date dt = new Date();
        ts.setDate(dt);
        assertNotNull("Date is Null", ts.getDate());
        assertEquals("Set date differs from get date", dt, ts.getDate());
    }

    @Test
    public void getISOString() {
        MCRMetaISO8601Date ts = new MCRMetaISO8601Date();
        assertNull("Date is not Null", ts.getISOString());
        Date dt = new Date();
        ts.setDate(dt);
        assertNotNull("Date is Null", ts.getISOString());
    }

    @Test
    public void setFromDOM() {
        MCRMetaISO8601Date ts = new MCRMetaISO8601Date();
        Element datum = new Element("datum");
        datum.setAttribute("inherited", "0").setText("2006-01-23");
        ts.setFromDOM(datum);
        assertEquals("Dates not equal", "2006-01-23", ts.getISOString());
        datum.setAttribute("format", MCRISO8601Format.COMPLETE_HH_MM.toString());
        ts.setFromDOM(datum);
        assertNull("Date should be null", ts.getDate());
        assertEquals("Format should be set by jdom", MCRISO8601Format.COMPLETE_HH_MM.toString(), ts.getFormat());
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put(MCRISO8601Date.PROPERTY_STRICT_PARSING, "true");
        testProperties.put("log4j.logger.org.mycore.datamodel.metadata.MCRMetaISO8601Date", "INFO");

        return testProperties;
    }

}
