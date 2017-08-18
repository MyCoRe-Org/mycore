/*
 * $Revision: 5697 $ $Date: 07.04.2011 $
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

package org.mycore.mods;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.junit.Test;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRTestCase;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRVFSContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.xml.sax.SAXParseException;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRMODSWrapperTest extends MCRTestCase {

    /**
     * Test method for {@link org.mycore.mods.MCRMODSWrapper#wrapMODSDocument(org.jdom2.Element, java.lang.String)}.
     */
    @Test
    public void testWrapMODSDocument() throws SAXParseException, URISyntaxException, JDOMException, IOException {
        Document modsDoc = loadMODSDocument();
        MCRObject mcrObj = MCRMODSWrapper.wrapMODSDocument(modsDoc.getRootElement(), "JUnit");
        assertTrue("Generated MCRObject is not valid.", mcrObj.isValid());
        Document mcrObjXml = mcrObj.createXML();
        //check load from XML throws no exception
        MCRObject mcrObj2 = new MCRObject(mcrObjXml);
        mcrObjXml = mcrObj2.createXML();
        XPathExpression<Element> xpathCheck = XPathFactory.instance().compile("//mods:mods", Filters.element(), null,
            MCRConstants.MODS_NAMESPACE);
        assertEquals("Did not find mods data", 1, xpathCheck.evaluate(mcrObjXml).size());
    }

    private Document loadMODSDocument() throws SAXParseException, IOException {
        URL worldClassUrl = this.getClass().getResource("/mods80700998.xml");
        Document xml = MCRXMLParserFactory.getParser().parseXML(new MCRVFSContent(worldClassUrl));
        return xml;
    }

    @Test
    public void testSetMODS() throws SAXParseException, IOException, JDOMException {
        Element mods = loadMODSDocument().detachRootElement();
        MCRMODSWrapper wrapper = new MCRMODSWrapper();
        wrapper.setID("JUnit", 4711);
        wrapper.setMODS(mods);
        Document mcrObjXml = wrapper.getMCRObject().createXML();
        XPathExpression<Element> xpathCheck = XPathFactory.instance().compile("//mods:mods", Filters.element(), null,
            MCRConstants.MODS_NAMESPACE);
        assertEquals("Did not find mods data", 1, xpathCheck.evaluate(mcrObjXml).size());
    }

    @Test
    public void testServiceFlags() {
        MCRMODSWrapper wrapper = new MCRMODSWrapper();
        assertNull(wrapper.getServiceFlag("name"));
        wrapper.setServiceFlag("name", "value");
        assertEquals("value", wrapper.getServiceFlag("name"));
    }

    @Test
    public void setElement() throws SAXParseException, IOException {
        Element mods = loadMODSDocument().detachRootElement();
        MCRMODSWrapper wrapper = new MCRMODSWrapper();
        wrapper.setID("JUnit", 4711);
        wrapper.setMODS(mods);

        Map<String, String> attrMap = new HashMap<>();
        attrMap.put("authorityURI",
            "http://mycore.de/classifications/mir_filetype.xml");
        attrMap.put("displayLabel", "mir_filetype");
        attrMap.put("valueURI",
            "http://mycore.de/classifications/mir_filetype.xml#excel");

        wrapper.setElement("classification", "", attrMap);
        Document mcrObjXml = wrapper.getMCRObject().createXML();

        String checkXpathString = "//mods:mods/mods:classification["
            + "@authorityURI='http://mycore.de/classifications/mir_filetype.xml' and "
            + "@displayLabel='mir_filetype' and "
            + "@valueURI='http://mycore.de/classifications/mir_filetype.xml#excel'"
            + "]";

        XPathExpression<Element> xpathCheck = XPathFactory.instance().compile(checkXpathString, Filters.element(),
            null, MCRConstants.MODS_NAMESPACE);

        assertTrue(xpathCheck.evaluate(mcrObjXml).size() > 0);
    }

    @Test
    public void testIsSupported() {
        MCRConfiguration.instance().set("MCR.Metadata.Type.sthelse", true);
        MCRObjectID mycoreMods = MCRObjectID.getInstance("mycore_mods_00000011");
        MCRObjectID mycoreSthelse = MCRObjectID.getInstance("mycore_sthelse_00000011");

        assertTrue("Mods type should be supported.", MCRMODSWrapper.isSupported(mycoreMods));
        assertFalse("sthesle type should not be supported.", MCRMODSWrapper.isSupported(mycoreSthelse));
    }

    public void testGetLinkedRelatedItems() throws SAXParseException, IOException {
        Element mods = loadMODSDocument().detachRootElement();
        MCRMODSWrapper wrapper = new MCRMODSWrapper();

        Element relatedItem = new Element("relatedItem", MCRConstants.MODS_NAMESPACE);
        relatedItem.setAttribute("href", "mir_test_00000001", MCRConstants.XLINK_NAMESPACE);
        relatedItem.setAttribute("type", "series");
        mods.addContent(relatedItem);

        wrapper.setID("JUnit", 4711);
        wrapper.setMODS(mods);

        assertEquals("There should be one related item!", wrapper.getLinkedRelatedItems().size(), 1);
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.mods", "true");
        return testProperties;
    }
}
