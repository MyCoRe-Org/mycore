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

package org.mycore.frontend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRURLTest extends MCRTestCase {

    @Test
    public void getParameter() throws Exception {
        MCRURL url = new MCRURL("http://localhost:8080/test?a=hallo&b=mycore&c=münchen");
        assertEquals("hallo", url.getParameter("a"));
        assertEquals("mycore", url.getParameter("b"));
        assertEquals("münchen", url.getParameter("c"));
        MCRURL url2 = new MCRURL("http://localhost:8080/test?a=m%C3%BCnchen");
        assertEquals("m%C3%BCnchen", url2.getParameter("a"));
    }

    @Test
    public void getParameterMap() throws Exception {
        MCRURL url = new MCRURL("http://localhost:8080/test?a=hallo&b=mycore&a=");
        Map<String, List<String>> p = url.getParameterMap();
        assertEquals(2, p.size());
        List<String> a = p.get("a");
        List<String> b = p.get("b");
        assertEquals(2, a.size());
        assertEquals(1, b.size());
        assertEquals("mycore", b.get(0));
        assertTrue(a.contains("hallo"));
        assertTrue(a.contains(""));
    }

    @Test
    public void addParameter() throws Exception {
        MCRURL url = new MCRURL("http://localhost:8080/test?a=hallo&b=mycore");
        url.addParameter("c", "alleswirdgut");
        assertEquals("alleswirdgut", url.getParameter("c"));
        url.addParameter("a", "repository");
        List<String> aValues = url.getParameterValues("a");
        assertTrue(aValues.contains("hallo"));
        assertTrue(aValues.contains("repository"));
        MCRURL url2 = new MCRURL("http://localhost:8080/test?a=hinz%20%26%20kunz");
        url2.addParameter("b", "b%C3%A4r");
        assertEquals("http://localhost:8080/test?a=hinz%20%26%20kunz&b=b%C3%A4r", url2.getURL().toString());
    }

    @Test
    public void removeParameter() throws Exception {
        MCRURL url = new MCRURL("http://localhost:8080/test?a=hallo&b=mycore&a=alleswirdgut");
        url.removeParameter("a");
        assertNull(url.getParameter("a"));
        assertEquals("mycore", url.getParameter("b"));
        url.removeParameter("b");
        assertNull(url.getParameter("b"));
        MCRURL url2 = new MCRURL("http://localhost:8080/test?a=hinz%20%26%20kunz&b=removeme");
        url2.removeParameter("b");
        assertEquals("http://localhost:8080/test?a=hinz%20%26%20kunz", url2.getURL().toString());
    }

    @Test
    public void removeParameterValue() throws Exception {
        MCRURL url = new MCRURL("http://localhost:8080/test?a=hallo&b=mycore&a=alleswirdgut");
        url.removeParameterValue("a", "alleswirdgut");
        assertEquals("hallo", url.getParameter("a"));
        url.removeParameterValue("b", "mycore");
        assertNull(url.getParameter("b"));
    }

}
