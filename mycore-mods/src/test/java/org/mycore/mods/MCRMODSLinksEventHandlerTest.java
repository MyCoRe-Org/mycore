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

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.junit.Before;
import org.junit.Test;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.content.MCRURLContent;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectMetadataTest;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MCRMODSLinksEventHandlerTest extends MCRStoreTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.SUPER_USER);
    }

    /**
     * Tests if references to modsperson are correctly persisted in the database
     */
    @Test
    public void testHandleObjectCreatedModsperson() throws IOException, JDOMException, MCRAccessException {
        URL url1 = MCRObjectMetadataTest.class.getResource("/MCRMODSLinksEventHandlerTest/junit_mods_00000001.xml");
        Document doc1 = new MCRURLContent(url1).asXML();
        MCRObject obj1 = new MCRObject(doc1);

        MCRMetadataManager.create(obj1);

        MCRLinkTableManager linkTableManager = MCRLinkTableManager.getInstance();
        int countLinks = linkTableManager.countReferenceLinkTo("junit_modsperson_00000001");
        assertEquals(1, countLinks);

        List<String> sources = (List) linkTableManager.getSourceOf("junit_modsperson_00000001");
        assertEquals(1, sources.size());
        assertEquals("junit_mods_00000001", sources.getFirst());
    }


}
