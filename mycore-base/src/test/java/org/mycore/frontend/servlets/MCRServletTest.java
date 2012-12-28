/**
 * 
 */
package org.mycore.frontend.servlets;

import static org.junit.Assert.*;

import java.net.URISyntaxException;

import org.junit.Test;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRServletTest {

    /**
     * Test method for {@link org.mycore.frontend.servlets.MCRServlet#encodeURL(String)}.
     */
    @Test
    public void testencodeURL() {
        String test=null, encoded;
        try {
            test="http://server.mycore.de/search?query=Java&maxResult=100";
            encoded = MCRServlet.encodeURL(test);
            System.out.println(test + "->" + encoded);
            test = "Test{}.txt";
            encoded = MCRServlet.encodeURL(test);
            System.out.println(test + "->" + encoded);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Could not encode URI " + test, e);
        }
    }
}
