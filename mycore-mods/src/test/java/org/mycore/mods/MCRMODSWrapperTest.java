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

package org.mycore.mods;

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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.MCRXlink;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRURLContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.test.MyCoReTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Scheffler (yagee)
 */
@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true")
})
public class MCRMODSWrapperTest {

    /**
     * Test method for {@link org.mycore.mods.MCRMODSWrapper#wrapMODSDocument(org.jdom2.Element, java.lang.String)}.
     */
    @Test
    public void testWrapMODSDocument() throws Exception {
        Document modsDoc = loadMODSDocument();
        MCRObject mcrObj = MCRMODSWrapper.wrapMODSDocument(modsDoc.getRootElement(), "JUnit");
        assertTrue(mcrObj.isValid(), "Generated MCRObject is not valid.");
        Document mcrObjXml = mcrObj.createXML();
        //check load from XML throws no exception
        MCRObject mcrObj2 = new MCRObject(mcrObjXml);
        mcrObjXml = mcrObj2.createXML();
        XPathExpression<Element> xpathCheck = XPathFactory.instance().compile("//mods:mods", Filters.element(), null,
            MCRConstants.MODS_NAMESPACE);
        assertEquals(1, xpathCheck.evaluate(mcrObjXml).size(), "Did not find mods data");
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
        assertEquals(1, xpathCheck.evaluate(mcrObjXml).size(), "Did not find mods data");
    }

    @Test
    public void testServiceFlags() {
        MCRMODSWrapper wrapper = new MCRMODSWrapper();
        Assertions.assertNull(wrapper.getServiceFlag("name"));
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

        assertFalse(xpathCheck.evaluate(mcrObjXml).isEmpty());
    }

    @Test
    public void testIsSupported() {
        MCRConfiguration2.set("MCR.Metadata.Type.sthelse", String.valueOf(true));
        MCRObjectID mycoreMods = MCRObjectID.getInstance("mycore_mods_00000011");
        MCRObjectID mycoreSthelse = MCRObjectID.getInstance("mycore_sthelse_00000011");

        assertTrue(MCRMODSWrapper.isSupported(mycoreMods), "Mods type should be supported.");
        assertFalse(MCRMODSWrapper.isSupported(mycoreSthelse), "sthesle type should not be supported.");
    }

    @Test
    public void testGetLinkedRelatedItems() throws IOException, JDOMException {
        Element mods = loadMODSDocument().detachRootElement();
        MCRMODSWrapper wrapper = new MCRMODSWrapper();

        Element relatedItem = new Element("relatedItem", MCRConstants.MODS_NAMESPACE);
        relatedItem.setAttribute(MCRXlink.HREF, "mir_test_00000001", MCRConstants.XLINK_NAMESPACE);
        mods.addContent(relatedItem);
        wrapper.setID("JUnit", 4711);
        wrapper.setMODS(mods);
        assertEquals(0, wrapper.getLinkedRelatedItems().size(), "There should be no related item!");

        relatedItem.setAttribute("type", "");
        assertEquals(0, wrapper.getLinkedRelatedItems().size(), "There should be no related item!");

        relatedItem.setAttribute("type", "series");
        assertEquals(1, wrapper.getLinkedRelatedItems().size(), "There should be one related item!");

        relatedItem.removeAttribute(MCRXlink.HREF, MCRConstants.XLINK_NAMESPACE);
        assertEquals(0, wrapper.getLinkedRelatedItems().size(), "There should be no related item!");
    }

}
