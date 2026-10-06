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

package org.mycore.common.xsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import javax.xml.transform.TransformerException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.util.MCRTestCaseXSLTUtil;
import org.mycore.common.xsl.uriresolver.MCRMockResolver;
import org.mycore.common.xsl.uriresolver.MCRURIResolver;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.URIResolver.ModuleResolver.mcrobject.Class", classNameOf = MCRMockResolver.class)
})
public class MCRObjectFunctionsTests {

    private static final String OBJECT_ID = "mcr_test_00000001";

    @BeforeEach
    public void setUp() {
        MCRMockResolver.clearCalls();
        MCRMockResolver.setResultSource(null);
        MCRURIResolver.obtainInstance().reinitialize();
    }

    @Test
    @DisplayName("mcrobject:get returns the object")
    public void testGet() throws TransformerException {
        MCRMockResolver.setResultSource(new JDOMSource(new Document(new Element("mycoreobject"))));

        Element result = get(OBJECT_ID);

        assertEquals("mycoreobject", result.getChildren().getFirst().getName());
        assertEquals("mcrobject:" + OBJECT_ID, MCRMockResolver.getCalls().getFirst().getHref());
    }

    @Test
    @DisplayName("mcrobject:get returns the expanded object")
    public void testGetExpanded() throws TransformerException {
        MCRMockResolver.setResultSource(new JDOMSource(new Document(new Element("mycoreobject"))));

        Element result = get(OBJECT_ID, true);

        assertEquals("mycoreobject", result.getChildren().getFirst().getName());
        assertEquals("mcrobject:" + OBJECT_ID, MCRMockResolver.getCalls().getFirst().getHref());
    }

    @Test
    @DisplayName("mcrobject:get returns the non-expanded object")
    public void testGetNotExpanded() throws TransformerException {
        MCRMockResolver.setResultSource(new JDOMSource(new Document(new Element("mycoreobject"))));

        Element result = get(OBJECT_ID, false);

        assertEquals("mycoreobject", result.getChildren().getFirst().getName());
        assertEquals("mcrobject:" + OBJECT_ID + "?expanded=false", MCRMockResolver.getCalls().getFirst().getHref());
    }

    @Test
    @DisplayName("mcrobject:get returns nothing for a missing object")
    public void testGetMissing() throws TransformerException {
        assertTrue(get(OBJECT_ID).getChildren().isEmpty());
    }

    private static Element get(String id) throws TransformerException {
        return MCRTestCaseXSLTUtil.transform("/xslt/functions/object-test.xsl", Map.of("id", id)).getRootElement();
    }

    private static Element get(String id, boolean expanded) throws TransformerException {
        return MCRTestCaseXSLTUtil.transform("/xslt/functions/object-test.xsl",
            Map.of("id", id, "expanded", String.valueOf(expanded))).getRootElement();
    }
}
