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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.xpath.XPath;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRTestCase;
import org.mycore.common.content.MCRVFSContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.metadata.MCRObject;
import org.xml.sax.SAXParseException;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRMODSWrapperTest extends MCRTestCase {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        setProperty("MCR.Metadata.Type.mods", "true", false);
    }

    /**
     * Test method for {@link org.mycore.mods.MCRMODSWrapper#wrapMODSDocument(org.jdom2.Element, java.lang.String)}.
     * @throws URISyntaxException 
     * @throws SAXParseException 
     * @throws JDOMException 
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
        XPath xpathCheck = XPath.newInstance("//mods:mods");
        xpathCheck.addNamespace(MCRConstants.MODS_NAMESPACE);
        assertEquals("Did not find mods data", 1, xpathCheck.selectNodes(mcrObjXml).size());
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
        XPath xpathCheck = XPath.newInstance("//mods:mods");
        xpathCheck.addNamespace(MCRConstants.MODS_NAMESPACE);
        assertEquals("Did not find mods data", 1, xpathCheck.selectNodes(mcrObjXml).size());
    }

    @Test
    public void testServiceFlags() {
        MCRMODSWrapper wrapper = new MCRMODSWrapper();
        assertNull(wrapper.getServiceFlag("name"));
        wrapper.setServiceFlag("name", "value");
        assertEquals("value", wrapper.getServiceFlag("name"));
    }
}
