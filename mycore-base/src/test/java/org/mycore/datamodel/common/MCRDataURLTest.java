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
package org.mycore.datamodel.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.nio.charset.IllegalCharsetNameException;

import org.junit.Test;
import org.mycore.common.MCRTestCase;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class MCRDataURLTest extends MCRTestCase {

    private static final String[] VALID = {
        "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQAQMAAAAlPW0iAAAABlBMVEUAAAD///+l2Z/dAAAAM0lEQVR4nGP4/5/h/1+G/58ZDrAz3D/McH8yw83NDDeNGe4Ug9C9zwz3gVLMDA/A6P9/AFGGFyjOXZtQAAAAAElFTkSuQmCC",
        "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAgAAAAIBAMAAAA2IaO4AAAAFVBMVEXk5OTn5+ft7e319fX29vb5+fn///++GUmVAAAALUlEQVQIHWNICnYLZnALTgpmMGYIFWYIZTA2ZFAzTTFlSDFVMwVyQhmAwsYMAKDaBy0axX/iAAAAAElFTkSuQmCC",
        "   data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAgAAAAIBAMAAAA2IaO4AAAAFVBMVEXk5OTn5+ft7e319fX29vb5+fn///++GUmVAAAALUlEQVQIHWNICnYLZnALTgpmMGYIFWYIZTA2ZFAzTTFlSDFVMwVyQhmAwsYMAKDaBy0axX/iAAAAAElFTkSuQmCC   ",
        " data:,Hello%2C%20World!", " data:,Hello World!", " data:text/html,%3Ch1%3EHello%2C%20World!%3C%2Fh1%3E",
        "data:,A%20brief%20note", "data:text/html;charset=US-ASCII,%3Ch1%3EHello!%3C%2Fh1%3E",
        "data:text/html;charset=UTF-8,%3Ch1%3EHello!%3C%2Fh1%3E",
        "data:text/html;charset=US-ASCII;param=extra,%3Ch1%3EHello!%3C%2Fh1%3E",
        "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIGhlaWdodD0iMTAwIj48cmVjdCBmaWxsPSIjMDBCMUZGIiB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIvPjwvc3ZnPg==" };

    private static final String[] INVALID = { " data:text/plain;base64,SGVsbG8sIFdvcmxkIQ%3D%3D",
        "dataxbase64", "data:HelloWorld", "data:text/html;charset=,%3Ch1%3EHello!%3C%2Fh1%3E",
        "data:text/html;charset,%3Ch1%3EHello!%3C%2Fh1%3E",
        "data:base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQAQMAAAAlPW0iAAAABlBMVEUAAAD///+l2Z/dAAAAM0lEQVR4nGP4/5/h/1+G/58ZDrAz3D/McH8yw83NDDeNGe4Ug9C9zwz3gVLMDA/A6P9/AFGGFyjOXZtQAAAAAElFTkSuQmCC",
        "", "http://wikipedia.org", "base64",
        "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQAQMAAAAlPW0iAAAABlBMVEUAAAD///+l2Z/dAAAAM0lEQVR4nGP4/5/h/1+G/58ZDrAz3D/McH8yw83NDDeNGe4Ug9C9zwz3gVLMDA/A6P9/AFGGFyjOXZtQAAAAAElFTkSuQmCC" };

    @Test
    public void testParseValid() {
        for (final String url : VALID) {
            try {
                MCRDataURL dataURL = MCRDataURL.parse(url);

                assertNotNull(dataURL);
            } catch (IllegalCharsetNameException | MalformedURLException e) {
                fail(url + ": " + e.getMessage());
            }
        }
    }

    @Test
    public void testParseInValid() {
        for (final String url : INVALID) {
            boolean thrown = false;
            try {
                MCRDataURL dataURL = MCRDataURL.parse(url);
                assertNull(url + " is not null.", dataURL);
            } catch (IllegalCharsetNameException | MalformedURLException e) {
                thrown = true;
            }
            assertTrue(thrown);
        }
    }

    @Test
    public void testCompose() {
        for (final String url : VALID) {
            MCRDataURL du1;
            try {
                du1 = MCRDataURL.parse(url);
                assertNotNull(du1);
            } catch (IllegalCharsetNameException | MalformedURLException e) {
                fail("unserialize " + url + ": " + e.getMessage());
                return;
            }

            try {
                MCRDataURL du2 = MCRDataURL.parse(du1.toString());

                assertEquals(du1, du2);
            } catch (IllegalCharsetNameException | MalformedURLException e) {
                fail("serialize " + url + ": " + e.getMessage());
            }
        }
    }
}
