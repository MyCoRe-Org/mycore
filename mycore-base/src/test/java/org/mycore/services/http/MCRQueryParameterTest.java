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

package org.mycore.services.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class MCRQueryParameterTest {
    @Test
    public void testOfEncodedString() {
        MCRQueryParameter parameter = MCRQueryParameter.ofEncodedString("name=value");
        assertEquals("name", parameter.name());
        assertEquals("value", parameter.value());
        parameter = MCRQueryParameter.ofEncodedString("name");
        assertEquals("name", parameter.name());
        assertEquals("", parameter.value());
        parameter = MCRQueryParameter.ofEncodedString("=value");
        assertEquals("", parameter.name());
        assertEquals("value", parameter.value());
    }

    @Test
    public void testToEncodedString() {
        assertEquals("name=value", new MCRQueryParameter("name", "value").toEncodedString());
        assertEquals("name=%3D%26", new MCRQueryParameter("name", "=&").toEncodedString());
        //UTF-8 encoding test:
        assertEquals("name=%C3%BC", new MCRQueryParameter("name", "ü").toEncodedString());
        assertEquals("name", new MCRQueryParameter("name", null).toEncodedString());
        assertEquals("name", new MCRQueryParameter("name", "").toEncodedString());
        assertEquals("=value", new MCRQueryParameter(null, "value").toEncodedString());
        assertEquals("=value", new MCRQueryParameter("", "value").toEncodedString());
    }

    @Test
    public void testParse() {
        String encodedString = "?name1=value1&name2=value2";
        List<MCRQueryParameter> parameters = MCRQueryParameter.parse(encodedString);
        assertEquals(2, parameters.size());

        MCRQueryParameter parameter1 = parameters.getFirst();
        assertEquals("name1", parameter1.name());
        assertEquals("value1", parameter1.value());

        MCRQueryParameter parameter2 = parameters.get(1);
        assertEquals("name2", parameter2.name());
        assertEquals("value2", parameter2.value());

        assertEquals(MCRQueryParameter.parse(encodedString),
            MCRQueryParameter.parse(encodedString.substring(1)));
    }

    @Test
    public void testToQueryString() {
        List<MCRQueryParameter> parameters = List.of(
            new MCRQueryParameter("name1", "value1"),
            new MCRQueryParameter("name2", "value2"));

        String expected = "?name1=value1&name2=value2";
        assertEquals(expected, MCRQueryParameter.toQueryString(parameters));
    }

}
