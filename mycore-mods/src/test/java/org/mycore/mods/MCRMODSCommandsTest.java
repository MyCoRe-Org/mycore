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
import static org.junit.Assert.assertNull;

import java.util.List;

import org.jdom2.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.datamodel.ifs2.MCRStoreManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRMODSCommandsTest extends MCRStoreTestCase {

    private MCRObjectID objectID;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectID = MCRObjectID.getInstance("junit_mods_00000001");
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.getSuperUserInstance());
        MCRMetadataManager.create(new MCRObject(getResourceAsURL(objectID + ".xml").toURI()));
    }

    @After
    public void tearDown() throws Exception {
        MCRMetadataManager.deleteMCRObject(objectID);
        MCRStoreManager.removeStore(objectID.getBase());
        super.tearDown();
    }

    @Test
    public void testRepairPagesOfAllObjects() {
        assertEquals(List.of("repair pages of mods object " + objectID), MCRMODSCommands.repairPagesOfAllObjects());
    }

    @Test
    public void testRepairPages() throws Exception {
        MCRMODSCommands.repairPages(objectID.toString());

        MCRMODSWrapper wrapper = new MCRMODSWrapper(MCRMetadataManager.retrieveMCRObject(objectID));

        Element host = wrapper.getElement("mods:relatedItem[@type='host']/mods:part/mods:extent[@unit='pages']");
        assertEquals(2, host.getChildren().size());
        assertEquals("275", host.getChildText("start", MCRConstants.MODS_NAMESPACE));
        assertEquals("281", host.getChildText("end", MCRConstants.MODS_NAMESPACE));
        assertNull(host.getChild("list", MCRConstants.MODS_NAMESPACE));

        Element series = wrapper.getElement("mods:relatedItem[@type='series']/mods:part/mods:extent[@unit='pages']");
        assertEquals(1, series.getChildren().size());
        assertEquals("3-4 und 7-8", series.getChildText("list", MCRConstants.MODS_NAMESPACE));
    }
}
