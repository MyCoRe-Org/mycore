package org.mycore.datamodel.metadata;

import java.io.IOException;
import java.util.Date;

import junit.framework.TestCase;

import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class MCRMetaISO8601DateTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
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
        //wrong date format for the following string should null the internal date.
        timeString = "1997-07-16T19:20:30+01:00";
        System.out.println(timeString);
        ts.setDate(timeString);
        assertNull("Date is not null", ts.getDate());
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
        MCRMetaISO8601Date ts=new MCRMetaISO8601Date();
        assertNotNull("Format used is Null", ts.getFormat());
        ts.setFormat(MCRMetaISO8601Date.COMPLETE);
        assertEquals("Set format differs from get format",MCRMetaISO8601Date.COMPLETE,ts.getFormat());
    }

    public void testgetDate() {
        MCRMetaISO8601Date ts=new MCRMetaISO8601Date();
        assertNull("Date is not Null", ts.getDate());
        Date dt=new Date();
        ts.setDate(dt);
        assertNotNull("Date is Null", ts.getDate());
        assertEquals("Set date differs from get date",dt,ts.getDate());
    }

    public void testgetISOString() {
        MCRMetaISO8601Date ts=new MCRMetaISO8601Date();
        assertNull("Date is not Null", ts.getISOString());
        Date dt=new Date();
        ts.setDate(dt);
        assertNotNull("Date is Null", ts.getISOString());
    }

}
