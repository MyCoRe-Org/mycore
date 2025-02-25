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
package org.mycore.datamodel.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRTestCase;
import org.mycore.common.content.MCRURLContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRISO8601Date;

/**
 * @author Robert Stephan
 *
 */
public class MCRObjectServiceTest extends MCRTestCase {
    private static final String TEST_OBJECT_RESOURCE_NAME = "/mcr_test_01.xml";

    private MCRObjectService testService;

    /* (non-Javadoc)
     * @see org.mycore.common.MCRTestCase#setUp()
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        Document testObjectDocument = loadResourceDocument(TEST_OBJECT_RESOURCE_NAME);
        testService = new MCRObjectService();
        testService.setFromDOM(testObjectDocument.getRootElement().getChild(MCRObjectService.XML_NAME));
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#size()}.
     */
    @Test
    public void size() {
        assertEquals("Expected two servdate entries", 2, testService.getDateSize());
        assertEquals("Expected two servflag entries", 2, testService.getFlagSize());
        assertNotNull("Expected one servstate entry", testService.getState());
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#getMetadataElement(int)}.
     */
    @Test
    public void getServFlags() {
        assertEquals("Servflag of type 'createdby' does not contain 'editorA'",
            "editorA", testService.getFlags(MCRObjectService.FLAG_TYPE_CREATEDBY).get(0));
        assertEquals("Servflag of type 'modifiedby' does not contain 'editorB'",
            "editorB", testService.getFlags(MCRObjectService.FLAG_TYPE_MODIFIEDBY).get(0));
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#getHeritableMetadata()}.
     */
    @Test
    public void getServDates() {
        MCRISO8601Date createDate = new MCRISO8601Date("2024-09-24T12:00:00.123Z");
        assertEquals("Servdate of type 'createdate' does not match input",
            createDate.getDate(), testService.getDate(MCRObjectService.DATE_TYPE_CREATEDATE));
        MCRISO8601Date modifyDate = new MCRISO8601Date("2024-09-24T17:00:00.123Z");
        assertEquals("Servdate of type 'modifydate' does not match input",
            modifyDate.getDate(), testService.getDate(MCRObjectService.DATE_TYPE_MODIFYDATE));
    }

    @Test
    public void getServState() {
        testService.getState();
        MCRCategoryID categ = new MCRCategoryID("state", "published");
        assertEquals("Servsate does not match 'state:published'",
            categ, testService.getState());
    }

    private static Document loadResourceDocument(String resource) throws MCRException, IOException, JDOMException {
        URL mcrTestUrl = MCRObjectServiceTest.class.getResource(resource);
        return MCRXMLParserFactory.getValidatingParser().parseXML(new MCRURLContent(mcrTestUrl));
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.DefaultLang", "de");
        return testProperties;
    }
}
