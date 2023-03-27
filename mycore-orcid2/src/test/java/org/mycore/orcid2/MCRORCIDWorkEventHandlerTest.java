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

package org.mycore.orcid2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRURLContent;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImpl;
import org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImplTest;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.xml.sax.SAXException;

public class MCRORCIDWorkEventHandlerTest extends MCRJPATestCase {

    static final MCRCategoryDAO DAO = new MCRCategoryDAOImpl();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        final MCRCategory state
            = MCRCategoryDAOImplTest.loadClassificationResource("/mycore-classifications/state.xml");
        DAO.addCategory(null, state);
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());
        return testProperties;
    }

    @Test
    public void testDefaultFilter() throws Exception {
        final MCRObject input = loadDefaultObject();
        setState(input, "published");
        final MCRObject output = MCRORCIDWorkEventHandler.filterObject(input);
        final MCRMODSWrapper wrapper = new MCRMODSWrapper(output);
        final Element titleElement = wrapper.getElement("mods:title");
        assertNotNull(titleElement);
        assertEquals("Test", wrapper.getElement("mods:title").getText());
        assertTrue(MCRORCIDWorkEventHandler.checkPublish(output));
    }

    @Test
    public void testSkipObject() throws Exception {
        final MCRObject input = loadDefaultObject();
        setState(input, "submitted");
        final MCRObject output = MCRORCIDWorkEventHandler.filterObject(input);
        assertFalse(MCRORCIDWorkEventHandler.checkPublish(output));
    }

    private static MCRObject loadDefaultObject() throws JDOMException, IOException, SAXException {
        final MCRContent inputContent
            = new MCRURLContent(MCRORCIDWorkEventHandlerTest.class.getResource("/mods_example.xml"));
        return new MCRObject(inputContent.asXML());
    }

    private static void setState(MCRObject object, String state) {
        object.getService().setState(state);
    }
} 
