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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.URL;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.content.MCRURLContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.test.MyCoReTest;

/**
 * @author Robert Stephan
 *
 */
@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.DefaultLang", string = "de")
})
public class MCRObjectServiceTest {
    private static final String TEST_OBJECT_RESOURCE_NAME = "/mcr_test_01.xml";

    private MCRObjectService testService;

    @BeforeEach
    public void setUp() throws Exception {
        Document testObjectDocument = loadResourceDocument(TEST_OBJECT_RESOURCE_NAME);
        testService = new MCRObjectService();
        testService.setFromDOM(testObjectDocument.getRootElement().getChild(MCRObjectService.XML_NAME));
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#size()}.
     */
    @Test
    public void size() {
        assertEquals(2, testService.getDateSize(), "Expected two servdate entries");
        assertEquals(2, testService.getFlagSize(), "Expected two servflag entries");
        assertNotNull(testService.getState(), "Expected one servstate entry");
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#getMetadataElement(int)}.
     */
    @Test
    public void getServFlags() {
        assertEquals("editorA", testService.getFlags(MCRObjectService.FLAG_TYPE_CREATEDBY).get(0),
            "Servflag of type 'createdby' does not contain 'editorA'");
        assertEquals("editorB", testService.getFlags(MCRObjectService.FLAG_TYPE_MODIFIEDBY).get(0),
            "Servflag of type 'modifiedby' does not contain 'editorB'");
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#getHeritableMetadata()}.
     */
    @Test
    public void getServDates() {
        MCRISO8601Date createDate = new MCRISO8601Date("2024-09-24T12:00:00.123Z");
        assertEquals(createDate.getDate(), testService.getDate(MCRObjectService.DATE_TYPE_CREATEDATE),
            "Servdate of type 'createdate' does not match input");
        MCRISO8601Date modifyDate = new MCRISO8601Date("2024-09-24T17:00:00.123Z");
        assertEquals(modifyDate.getDate(), testService.getDate(MCRObjectService.DATE_TYPE_MODIFYDATE),
            "Servdate of type 'modifydate' does not match input");
    }

    @Test
    public void getServState() {
        testService.getState();
        MCRCategoryID categ = new MCRCategoryID("state", "published");
        assertEquals(categ, testService.getState(), "Servsate does not match 'state:published'");
    }

    private static Document loadResourceDocument(String resource) throws MCRException, IOException, JDOMException {
        URL mcrTestUrl = MCRObjectServiceTest.class.getResource(resource);
        return MCRXMLParserFactory.getValidatingParser().parseXML(new MCRURLContent(mcrTestUrl));
    }

}
