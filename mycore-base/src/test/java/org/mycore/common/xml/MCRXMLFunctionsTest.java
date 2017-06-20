package org.mycore.common.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.text.ParseException;

import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRXMLFunctionsTest extends MCRTestCase {

    private final static String[] HTML_STRINGS = new String[] { "<h1>Hello World!</h1>",
        "<h1>Hell<i>o</i> World!<br /></h1>", "<h1>Hell<i>o</i> World!<br></h1>",
        "<h1>Hell<i>o</i> World!&lt;br&gt;</h1>", "<h1>Hell<i>&ouml;</i> World!&lt;br&gt;</h1>",
        "<h1>Hello</h1> <h2>World!</h2><br/>", "Hello <a href=\"http://www.mycore.de\">MyCoRe</a>!",
        "Hello <a href='http://www.mycore.de'>MyCoRe</a>!",
        "Gläser und Glaskeramiken im MgO-Al<sub>2</sub>O<sub>3</sub>-SiO<sub>2</sub>-System mit hoher Mikrohärte und hohem Elastizitätsmodul" };

    private final static String[] NON_HTML_STRINGS = new String[] { "Hello MyCoRe!", "a < b > c" };

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
        assertEquals("Timezone was not correctly detected", "1964-02-23T22:00:00Z", MCRXMLFunctions
            .getISODate("24.02.1964 00:00:00 +0200", "dd.MM.yyyy HH:mm:ss Z", "YYYY-MM-DDThh:mm:ssTZD"));
    }

    /*
     * Test method for 'org.mycore.common.xml.MCRXMLFunctions.getISODateFromMCRHistoryDate(String, String, String)'
     */
    @Test
    public void getISODateFromMCRHistoryDate() throws ParseException {
        assertEquals("1964-02-24T00:00:00.000Z",
            MCRXMLFunctions.getISODateFromMCRHistoryDate("1964-02-24", "von", "gregorian"));
        assertEquals("1964-03-08T00:00:00.000Z",
            MCRXMLFunctions.getISODateFromMCRHistoryDate("1964-02-24", "von", "julian"));
        assertEquals("1964-02-28T00:00:00.000Z",
            MCRXMLFunctions.getISODateFromMCRHistoryDate("1964-02", "bis", "gregorian"));
        assertEquals("-0100-12-31T00:00:00.000Z",
            MCRXMLFunctions.getISODateFromMCRHistoryDate("100 BC", "bis", "gregorian"));
    }

    @Test
    public void normalizeAbsoluteURL() throws MalformedURLException, URISyntaxException {
        String source = "http://www.mycore.de/Space Character.test";
        String result = "http://www.mycore.de/Space%20Character.test";
        assertEquals("Result URL is not correct", result, MCRXMLFunctions.normalizeAbsoluteURL(source));
        assertEquals("URL differs,  but was already RFC 2396 conform.", result,
            MCRXMLFunctions.normalizeAbsoluteURL(result));
        source = "http://www.mycore.de/Hühnerstall.pdf";
        result = "http://www.mycore.de/H%C3%BChnerstall.pdf";
        assertEquals("Result URL is not correct", result, MCRXMLFunctions.normalizeAbsoluteURL(source));
        assertEquals("URL differs,  but was already RFC 2396 conform.", result,
            MCRXMLFunctions.normalizeAbsoluteURL(result));
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
    public void decodeURIPath() throws URISyntaxException {
        String source = "Space%20Character.test";
        String result = "Space Character.test";
        assertEquals("Result URI path is not correct", result, MCRXMLFunctions.decodeURIPath(source));
        source = "H%C3%BChnerstall.pdf";
        result = "Hühnerstall.pdf";
        assertEquals("Result URI path is not correct", result, MCRXMLFunctions.decodeURIPath(source));
        source = "/New%20Folder";
        result = "/New Folder";
        assertEquals("Result URI path is not correct", result, MCRXMLFunctions.decodeURIPath(source));
    }

    @Test
    public void shortenText() {
        String test = "Foo bar";
        String result = "Foo...";
        assertEquals("Shortened text did not match", result, MCRXMLFunctions.shortenText(test, 3));
        assertEquals("Shortened text did not match", result, MCRXMLFunctions.shortenText(test, 0));
        assertEquals("Shortened text did not match", test, MCRXMLFunctions.shortenText(test, test.length()));
    }

    /*
     * Test method for 'org.mycore.common.xml.MCRXMLFunctions.isHtml(String)'
     */
    @Test
    public void isHtml() {
        for (final String s : HTML_STRINGS) {
            assertTrue("Should be html: " + s, MCRXMLFunctions.isHtml(s));
        }
        for (final String s : NON_HTML_STRINGS) {
            assertFalse("Should not be html: " + s, MCRXMLFunctions.isHtml(s));
        }
    }

    /*
     * Test method for 'org.mycore.common.xml.MCRXMLFunctions.stripHtml(String)'
     */
    @Test
    public void stripHtml() {
        for (final String s : HTML_STRINGS) {
            final String stripped = MCRXMLFunctions.stripHtml(s);
            assertFalse("Should not contains html: " + stripped, MCRXMLFunctions.isHtml(stripped));
        }
    }
}
