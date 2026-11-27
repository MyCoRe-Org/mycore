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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URL;
import java.util.Date;
import java.util.List;

import org.jdom2.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.datamodel.ifs2.MCRStoreManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@ExtendWith(MCRMetadataExtension.class)
public class MCRMODSCommandsTest {

    private MCRObjectID objectID;

    private MCRObjectID unchangedObjectID;

    @BeforeEach
    public void setUp() throws Exception {
        objectID = MCRObjectID.getInstance("junit_mods_00000001");
        unchangedObjectID = MCRObjectID.getInstance("junit_mods_00000002");
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.SUPER_USER);
        MCRMetadataManager.create(new MCRObject(getResourceAsURL(objectID + ".xml").toURI()));
        MCRMetadataManager.create(new MCRObject(getResourceAsURL(unchangedObjectID + ".xml").toURI()));
    }

    @AfterEach
    public void tearDown() throws Exception {
        MCRMetadataManager.deleteMCRObject(objectID);
        MCRMetadataManager.deleteMCRObject(unchangedObjectID);
        MCRStoreManager.removeStore(objectID.getBase());
    }

    @Test
    public void testRepairPagesOfAllObjects() {
        assertEquals(List.of("repair pages of mods object " + objectID, "repair pages of mods object "
            + unchangedObjectID), MCRMODSCommands.repairPagesOfAllObjects());
    }

    @Test
    public void testRepairPages() throws Exception {
        MCRMODSCommands.repairPages(objectID.toString());

        MCRMODSWrapper wrapper = new MCRMODSWrapper(MCRMetadataManager.retrieveMCRObject(objectID));

        Element article = wrapper.getElement("mods:part/mods:extent[@unit='pages']");
        assertEquals(2, article.getChildren().size());
        assertEquals("12", article.getChildText("start", MCRConstants.MODS_NAMESPACE));
        assertEquals("15", article.getChildText("end", MCRConstants.MODS_NAMESPACE));

        Element host = wrapper.getElement("mods:relatedItem[@type='host']/mods:part/mods:extent[@unit='pages']");
        assertEquals(2, host.getChildren().size());
        assertEquals("275", host.getChildText("start", MCRConstants.MODS_NAMESPACE));
        assertEquals("281", host.getChildText("end", MCRConstants.MODS_NAMESPACE));
        assertNull(host.getChild("list", MCRConstants.MODS_NAMESPACE));

        Element series = wrapper.getElement("mods:relatedItem[@type='series']/mods:part/mods:extent[@unit='pages']");
        assertEquals(1, series.getChildren().size());
        assertEquals("3-4 und 7-8", series.getChildText("list", MCRConstants.MODS_NAMESPACE));
    }

    @Test
    public void testRepairPagesWithoutChanges() throws Exception {
        Date modified = getModifyDate(unchangedObjectID);
        assertNotNull(modified);

        MCRMODSCommands.repairPages(unchangedObjectID.toString());

        assertEquals(modified, getModifyDate(unchangedObjectID));
        Element extent = new MCRMODSWrapper(MCRMetadataManager.retrieveMCRObject(unchangedObjectID))
            .getElement("mods:part/mods:extent[@unit='pages']");
        assertEquals(1, extent.getChildren().size());
        assertEquals("S. 123 ff.", extent.getChildText("list", MCRConstants.MODS_NAMESPACE));

        Element host = new MCRMODSWrapper(MCRMetadataManager.retrieveMCRObject(unchangedObjectID))
            .getElement("mods:relatedItem[@type='host']/mods:part/mods:extent[@unit='pages']");
        assertEquals(1, host.getChildren().size());
        assertEquals("S. 5 \u2014 Beilage", host.getChildText("list", MCRConstants.MODS_NAMESPACE));
    }

    private URL getResourceAsURL(String fileName) {
        return getClass().getResource("/" + getClass().getSimpleName() + "/" + fileName);
    }

    private static Date getModifyDate(MCRObjectID id) {
        return MCRMetadataManager.retrieveMCRObject(id).getService().getDate("modifydate");
    }
}
