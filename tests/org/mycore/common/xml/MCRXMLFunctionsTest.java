package org.mycore.common.xml;

import java.text.ParseException;

import org.mycore.common.MCRTestCase;

public class MCRXMLFunctionsTest extends MCRTestCase {

    /*
     * Test method for 'org.mycore.common.xml.MCRXMLFunctions.formatISODate(String, String, String, String)'
     */
    public void testFormatISODate() throws ParseException {
        assertEquals("24.02.1964",MCRXMLFunctions.formatISODate("1964-02-24","dd.MM.yyyy","de"));
        assertEquals("1571", MCRXMLFunctions.formatISODate("1571", "yyyy", "de"));
    }

    /*
     * Test method for 'org.mycore.common.xml.MCRXMLFunctions.getISODate(String, String)'
     */
    public void testGetISODate() throws ParseException {
        assertEquals("1964-02-24",MCRXMLFunctions.getISODate("24.02.1964","dd.MM.yyyy","YYYY-MM-DD"));
        assertEquals("Timezone was not correctly detected","1964-02-23T22:00:00Z",MCRXMLFunctions.getISODate("24.02.1964 00:00:00 +0200","dd.MM.yyyy HH:mm:ss Z","YYYY-MM-DDThh:mm:ssTZD"));
    }
    
}
