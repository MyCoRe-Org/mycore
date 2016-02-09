/*
 * $Id$ 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */
package org.mycore.datamodel.common;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.mycore.common.MCRTestCase;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class MCRDataURLTest extends MCRTestCase {

    private final static String HELLO_STRING = "Hello <MyCoRe>!";
    private final static String MINIMAL_URLENCODED_DATA = "data:,Hello%20%3CMyCoRe%3E%21";
    private final static String URLENCODED_DATA = "data:text/plain;charset=UTF-8,Hello%20%3CMyCoRe%3E%21";
    private final static String B64ENCODED_DATA = "data:text/plain;charset=UTF-8;base64,SGVsbG8gPE15Q29SZT4h";

    @Test
    public void testMinimalURLEncodedCompose() throws MalformedURLException {
        MCRDataURL dataURL = new MCRDataURL(HELLO_STRING.getBytes(StandardCharsets.US_ASCII));

        assertEquals(MINIMAL_URLENCODED_DATA, dataURL.compose());
    }

    @Test
    public void testURLEncodedCompose() throws MalformedURLException {
        MCRDataURL dataURL = new MCRDataURL(HELLO_STRING.getBytes(Charset.forName("UTF-8")), "text/plain",
                Charset.forName("UTF-8"), MCRDataURLEncoding.URL);

        assertEquals(URLENCODED_DATA, dataURL.compose());
    }

    @Test
    public void testBase64EncodedCompose() throws MalformedURLException {
        MCRDataURL dataURL = new MCRDataURL(HELLO_STRING.getBytes(Charset.forName("UTF-8")), "text/plain",
                Charset.forName("UTF-8"), MCRDataURLEncoding.BASE64);

        assertEquals(B64ENCODED_DATA, dataURL.compose());
    }

    @Test
    public void testMinimalURLEncodedParse() throws MalformedURLException {
        MCRDataURL dataURL = MCRDataURL.parse(MINIMAL_URLENCODED_DATA);

        assertEquals(HELLO_STRING, new String(dataURL.getData()));
    }

    @Test
    public void testURLEncodedParse() throws MalformedURLException {
        MCRDataURL dataURL = MCRDataURL.parse(URLENCODED_DATA);

        assertEquals(HELLO_STRING, new String(dataURL.getData()));
    }

    @Test
    public void testBase64EncodedParse() throws MalformedURLException {
        MCRDataURL dataURL = MCRDataURL.parse(B64ENCODED_DATA);

        assertEquals(HELLO_STRING, new String(dataURL.getData()));
    }

}
