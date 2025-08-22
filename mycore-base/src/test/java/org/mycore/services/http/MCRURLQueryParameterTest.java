/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

public class MCRURLQueryParameterTest {
    @Test
    public void testOfEncodedString() {
        MCRURLQueryParameter parameter = MCRURLQueryParameter.ofEncodedString("name=value");
        assertEquals("name", parameter.name());
        assertEquals("value", parameter.value());
        parameter = MCRURLQueryParameter.ofEncodedString("name");
        assertEquals("name", parameter.name());
        assertEquals("", parameter.value());
        assertEquals(parameter, MCRURLQueryParameter.ofEncodedString("name="));
        parameter = MCRURLQueryParameter.ofEncodedString("=value");
        assertEquals("", parameter.name());
        assertEquals("value", parameter.value());
    }

    @Test
    public void testToEncodedString() {
        assertEquals("name=value", new MCRURLQueryParameter("name", "value").toEncodedString());
        assertEquals("name=%3D%26", new MCRURLQueryParameter("name", "=&").toEncodedString());
        //UTF-8 encoding test:
        assertEquals("name=%C3%BC", new MCRURLQueryParameter("name", "ü").toEncodedString());
        assertEquals("name", new MCRURLQueryParameter("name", null).toEncodedString());
        assertEquals("name", new MCRURLQueryParameter("name", "").toEncodedString());
        assertEquals("=value", new MCRURLQueryParameter(null, "value").toEncodedString());
        assertEquals("=value", new MCRURLQueryParameter("", "value").toEncodedString());
    }

    @Test
    public void testParse() {
        String encodedString = "?name1=value1&name2=value2";
        List<MCRURLQueryParameter> parameters = MCRURLQueryParameter.parse(encodedString);
        assertEquals(2, parameters.size());

        MCRURLQueryParameter parameter1 = parameters.getFirst();
        assertEquals("name1", parameter1.name());
        assertEquals("value1", parameter1.value());

        MCRURLQueryParameter parameter2 = parameters.get(1);
        assertEquals("name2", parameter2.name());
        assertEquals("value2", parameter2.value());

        assertEquals(MCRURLQueryParameter.parse(encodedString),
            MCRURLQueryParameter.parse(encodedString.substring(1)));
    }

    @Test
    public void testToQueryString() {
        List<MCRURLQueryParameter> parameters = List.of(
            new MCRURLQueryParameter("name", "value"),
            new MCRURLQueryParameter("name", "value²"));

        String expected = "name=value&name=value%C2%B2";
        assertEquals(expected, MCRURLQueryParameter.toQueryString(parameters));
    }

}
