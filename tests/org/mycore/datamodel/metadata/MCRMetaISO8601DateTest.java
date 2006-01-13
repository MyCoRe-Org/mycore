package org.mycore.datamodel.metadata;

import java.text.DateFormat;

import org.mycore.common.ISO8601DateFormat;

import junit.framework.TestCase;

public class MCRMetaISO8601DateTest extends TestCase {
    
    final static DateFormat df=new ISO8601DateFormat();

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaTimestamp.getSecond()'
     */
    public void testsetDate() {
        MCRMetaISO8601Date ts=new MCRMetaISO8601Date();
        String timeString="1997-07-16T19:20:30.452300+01:00";
        System.out.println(timeString);
        ts.setDate(timeString);
        assertNotNull("Date is null",ts.getDate());
        //this can be a different String, but point in time should be the same
        System.out.println(df.format(ts.getDate()));
    }

}
