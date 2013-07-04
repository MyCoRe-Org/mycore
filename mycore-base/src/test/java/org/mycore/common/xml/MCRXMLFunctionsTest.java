package org.mycore.common.xml;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.text.ParseException;

import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRXMLFunctionsTest extends MCRTestCase {

    /*
     * Test method for 'org.mycore.common.xml.MCRXMLFunctions.formatISODate(String, String, String, String)'
     */
    @Test
    public void formatISODate() throws ParseException {
        assertEquals("24.02.1964", MCRXMLFunctions.formatISODate("1964-02-24", "dd.MM.yyyy", "de"));
        assertEquals("1571", MCRXMLFunctions.formatISODate("1571", "yyyy", "de"));
    }

    /*
     * Test method for 'org.mycore.common.xml.MCRXMLFunctions.getISODate(String, String)'
     */
    @Test
    public void getISODate() throws ParseException {
        assertEquals("1964-02-24", MCRXMLFunctions.getISODate("24.02.1964", "dd.MM.yyyy", "YYYY-MM-DD"));
        assertEquals("Timezone was not correctly detected", "1964-02-23T22:00:00Z",
            MCRXMLFunctions.getISODate("24.02.1964 00:00:00 +0200", "dd.MM.yyyy HH:mm:ss Z", "YYYY-MM-DDThh:mm:ssTZD"));
    }

    @Test
    public void normalizeAbsoluteURL() throws MalformedURLException, URISyntaxException {
        String source = "http://www.mycore.de/Space Character.test";
        String result = "http://www.mycore.de/Space%20Character.test";
        assertEquals("Result URL is not correct", result, MCRXMLFunctions.normalizeAbsoluteURL(source));
        assertEquals("URL differs,  but was already RFC 2396 conform.", result, MCRXMLFunctions.normalizeAbsoluteURL(result));
        source = "http://www.mycore.de/Hühnerstall.pdf";
        result = "http://www.mycore.de/H%C3%BChnerstall.pdf";
        assertEquals("Result URL is not correct", result, MCRXMLFunctions.normalizeAbsoluteURL(source));
        assertEquals("URL differs,  but was already RFC 2396 conform.", result, MCRXMLFunctions.normalizeAbsoluteURL(result));
    }

    @Test
    public void endodeURIPath() throws URISyntaxException {
        String source = "Space Character.test";
        String result = "Space%20Character.test";
        assertEquals("Result URI path is not correct", result, MCRXMLFunctions.encodeURIPath(source));
        source = "Hühnerstall.pdf";
        result = "H%C3%BChnerstall.pdf";
        assertEquals("Result URI path is not correct", source, MCRXMLFunctions.encodeURIPath(source));
        assertEquals("Result URI path is not correct", result, MCRXMLFunctions.encodeURIPath(source, true));
    }

    @Test
    public void shortenText() {
        String test = "Foo bar";
        String result = "Foo...";
        assertEquals("Shortened text did not match", result, MCRXMLFunctions.shortenText(test, 3));
        assertEquals("Shortened text did not match", result, MCRXMLFunctions.shortenText(test, 0));
        assertEquals("Shortened text did not match", test, MCRXMLFunctions.shortenText(test, test.length()));
    }

}
