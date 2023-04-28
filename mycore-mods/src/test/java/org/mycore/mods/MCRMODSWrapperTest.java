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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
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
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRURLContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRMODSWrapperTest extends MCRTestCase {

    /**
     * Test method for {@link org.mycore.mods.MCRMODSWrapper#wrapMODSDocument(org.jdom2.Element, java.lang.String)}.
     */
    @Test
    public void testWrapMODSDocument() throws Exception {
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

    private Document loadMODSDocument() throws IOException, JDOMException {
        URL worldClassUrl = this.getClass().getResource("/mods80700998.xml");
        return MCRXMLParserFactory.getParser().parseXML(new MCRURLContent(worldClassUrl));
    }

    @Test
    public void testSetMODS() throws Exception {
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
    public void setElement() throws Exception {
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
        MCRConfiguration2.set("MCR.Metadata.Type.sthelse", String.valueOf(true));
        MCRObjectID mycoreMods = MCRObjectID.getInstance("mycore_mods_00000011");
        MCRObjectID mycoreSthelse = MCRObjectID.getInstance("mycore_sthelse_00000011");

        assertTrue("Mods type should be supported.", MCRMODSWrapper.isSupported(mycoreMods));
        assertFalse("sthesle type should not be supported.", MCRMODSWrapper.isSupported(mycoreSthelse));
    }

    @Test
    public void testGetLinkedRelatedItems() throws IOException, JDOMException {
        Element mods = loadMODSDocument().detachRootElement();
        MCRMODSWrapper wrapper = new MCRMODSWrapper();

        Element relatedItem = new Element("relatedItem", MCRConstants.MODS_NAMESPACE);
        relatedItem.setAttribute("href", "mir_test_00000001", MCRConstants.XLINK_NAMESPACE);
        mods.addContent(relatedItem);
        wrapper.setID("JUnit", 4711);
        wrapper.setMODS(mods);
        assertEquals("There should be no related item!", 0, wrapper.getLinkedRelatedItems().size());

        relatedItem.setAttribute("type", "");
        assertEquals("There should be no related item!", 0, wrapper.getLinkedRelatedItems().size());

        relatedItem.setAttribute("type", "series");
        assertEquals("There should be one related item!", 1, wrapper.getLinkedRelatedItems().size());
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.mods", "true");
        return testProperties;
    }
}
