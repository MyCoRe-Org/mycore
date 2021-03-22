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

package org.mycore.mods;

import java.io.IOException;
import java.net.URISyntaxException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs2.MCRStoreManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Tests for MCR-910 (Link MODS documents to other MODS documents)
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRMODSLinkedMetadataTest extends MCRJPATestCase {

    MCRObjectID seriesID, bookID;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        seriesID = MCRObjectID.getInstance("junit_mods_00000001");
        bookID = MCRObjectID.getInstance("junit_mods_00000002");
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.getSuperUserInstance());
        MCRObject series = new MCRObject(getResourceAsURL(seriesID + ".xml").toURI());
        MCRObject book = new MCRObject(getResourceAsURL(bookID + ".xml").toURI());
        MCRMetadataManager.create(series);
        MCRMetadataManager.create(book);
    }

    @After
    public void tearDown() throws Exception {
        MCRXMLMetadataManager mm = MCRXMLMetadataManager.instance();
        String seriesBase = seriesID.getBase();
        while (!mm.listIDsForBase(seriesBase).isEmpty()) {
            for (String storeID : mm.listIDsForBase(seriesBase)) {
                final MCRObjectID currentId = MCRObjectID.getInstance(storeID);
                System.err.println("Delete " + currentId);
                try {
                    MCRMetadataManager.deleteMCRObject(currentId);
                } catch (MCRActiveLinkException | MCRAccessException e) {
                    System.err.println("Cannot delete " + currentId + " at this moment.");
                }
            }
        }
        MCRStoreManager.removeStore(seriesID.getBase());
        super.tearDown();
    }

    @Test
    public void testLinks() {
        Assert.assertEquals("There should be a reference link from +" + bookID + " to " + seriesID + ".", 1,
            MCRLinkTableManager.instance().countReferenceLinkTo(seriesID));
    }

    @Test
    public void testUpdate() throws IOException, URISyntaxException, MCRPersistenceException,
        MCRActiveLinkException, JDOMException, SAXException, MCRAccessException {
        MCRObject seriesNew = new MCRObject(getResourceAsURL(seriesID + "-updated.xml").toURI());
        MCRMetadataManager.update(seriesNew);
        Document bookNew = MCRXMLMetadataManager.instance().retrieveXML(bookID);
        XPathBuilder<Element> builder = new XPathBuilder<>(
            "/mycoreobject/metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem/mods:titleInfo/mods:title",
            Filters.element());
        builder.setNamespace(MCRConstants.MODS_NAMESPACE);
        XPathExpression<Element> seriesTitlePath = builder.compileWith(XPathFactory.instance());
        Element titleElement = seriesTitlePath.evaluateFirst(bookNew);
        Assert.assertNotNull(
            "No title element in related item: " + new XMLOutputter(Format.getPrettyFormat()).outputString(bookNew),
            titleElement);
        Assert.assertEquals("Title update from series was not promoted to book of series.",
            "Updated series title", titleElement.getText());
    }

    @Test(expected = MCRPersistenceException.class)
    public void testHierarchyDirect() throws URISyntaxException, SAXParseException, IOException, MCRAccessException {
        MCRObjectID book2ID = MCRObjectID.getInstance("junit_mods_00000003");
        MCRObject book2 = new MCRObject(getResourceAsURL(book2ID + ".xml").toURI());
        MCRMetadataManager.create(book2);
        book2 = new MCRObject(getResourceAsURL(book2ID + "-updated.xml").toURI());
        MCRMetadataManager.update(book2);
    }

    @Test(expected = MCRPersistenceException.class)
    public void testHierarchyIndirect() throws URISyntaxException, SAXParseException, IOException, MCRAccessException {
        MCRObjectID book2ID = MCRObjectID.getInstance("junit_mods_00000003");
        MCRObject book2 = new MCRObject(getResourceAsURL(book2ID + ".xml").toURI());
        MCRMetadataManager.create(book2);
        MCRObject series = new MCRObject(getResourceAsURL(seriesID + "-updated2.xml").toURI());
        MCRMetadataManager.update(series);
    }
}
