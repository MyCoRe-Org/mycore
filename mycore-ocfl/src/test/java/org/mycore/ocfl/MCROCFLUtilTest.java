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

package org.mycore.ocfl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.OffsetDateTime;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mycore.common.config.MCRConfiguration2;

import edu.wisc.library.ocfl.api.OcflOption;
import edu.wisc.library.ocfl.api.model.ObjectVersionId;
import edu.wisc.library.ocfl.api.model.VersionInfo;

/**
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCROCFLUtilTest extends MCROCFLTestUtil {

    public static MCROcflUtil ocflUtil;

    private static String objectId = MCRConfiguration2.getStringOrThrow("MCR.OCFL.TestObject.Id");

    private ObjectVersionId versionId = ObjectVersionId.head(objectId);

    private VersionInfo versionInfo = new VersionInfo().setUser("JUnit", null).setMessage("OcflUtil Test");

    private static Path xml;

    @BeforeClass
    public static void setUp() throws Exception {
        MCROCFLUtilTest.ocflUtil = new MCROcflUtil();
        assertNotNull("Main Repository is undefined", ocflUtil.getMainRepository());
        assertNotNull("Adapt Repository is undefined", ocflUtil.getAdaptRepository());
        MCROCFLUtilTest.xml = Path.of(MCROCFLUtilTest.class.getResource("/" + objectId + ".xml").toURI());
    }

    @Before
    public void putSampleObject() {
        versionInfo.setCreated(OffsetDateTime.now());
        ocflUtil.getMainRepository().putObject(versionId, xml, versionInfo, OcflOption.OVERWRITE);
        assertTrue("Object was not Inserted correctly into Repository",
            ocflUtil.getMainRepository().containsObject(objectId));
    }

    @After
    public void cleanup() throws IOException {
        ocflUtil.resetMainRepo();
        MCROcflUtil.delDir(ocflUtil.mainClass.getRepositoryRoot());
        MCROcflUtil.delDir(ocflUtil.adaptClass.getRepositoryRoot());
        ocflUtil.reloadRepository();
    }

    @Test
    public void create() throws URISyntaxException, IOException {
        ObjectVersionId versionId = ObjectVersionId.head(objectId);
        VersionInfo versionInfo = new VersionInfo().setUser("JUnit", null).setMessage("OcflUtil Test")
            .setCreated(OffsetDateTime.now());
        ocflUtil.getMainRepository().putObject(versionId, xml, versionInfo, OcflOption.OVERWRITE);
        assertTrue("Object was not Inserted correctly into Main Repository",
            ocflUtil.getMainRepository().containsObject(objectId));
        assertNotNull("Object was Inserted empty into Main Repository",
            ocflUtil.getMainRepository().validateObject(objectId, true));
        ocflUtil.getAdaptRepository().putObject(versionId, xml, versionInfo, OcflOption.OVERWRITE);
        assertTrue("Object was not Inserted correctly into Adapt Repository",
            ocflUtil.getAdaptRepository().containsObject(objectId));
        assertNotNull("Object was Inserted empty into Adapt Repository",
            ocflUtil.getAdaptRepository().validateObject(objectId, true));
        ocflUtil.reloadRepository();
        assertTrue("Object was not Inserted correctly into Main Repository",
            ocflUtil.getMainRepository().containsObject(objectId));
        assertNotNull("Object was Inserted empty into Main Repository",
            ocflUtil.getMainRepository().validateObject(objectId, true));
        assertTrue("Object was not Inserted correctly into Adapt Repository",
            ocflUtil.getAdaptRepository().containsObject(objectId));
        assertNotNull("Object was Inserted empty into Adapt Repository",
            ocflUtil.getAdaptRepository().validateObject(objectId, true));
    }

    @Test
    public void exportAndImport() throws IOException {
        ocflUtil.getMainRepository().purgeObject(objectId);
        assertFalse("Object Delete Failed", ocflUtil.getMainRepository().containsObject(objectId));
        ObjectVersionId versionIdMCR = ObjectVersionId.head(MCROCFLXMLMetadataManager.MCR_OBJECT_ID_PREFIX + objectId);
        ocflUtil.getMainRepository().putObject(versionIdMCR, xml, versionInfo, OcflOption.OVERWRITE);
        assertTrue("Object was not inserted correctly",
            ocflUtil.getMainRepository().containsObject(MCROCFLXMLMetadataManager.MCR_OBJECT_ID_PREFIX + objectId));
        ocflUtil.exportObject(objectId)
            .importAdapt();
        assertTrue("Object did not get Imported to Adapt",
            ocflUtil.getAdaptRepository().containsObject(MCROCFLXMLMetadataManager.MCR_OBJECT_ID_PREFIX + objectId));
        ocflUtil.updateRoot()
            .exportRepository()
            .setRepositoryKey("Adapt")
            .updateMainRepo()
            .importRepository();
        assertTrue("Object did not get Imported to Main",
            ocflUtil.getAdaptRepository().containsObject(MCROCFLXMLMetadataManager.MCR_OBJECT_ID_PREFIX + objectId));
    }

    @Test
    public void delete() {
        ocflUtil.getMainRepository().purgeObject(objectId);
        assertFalse("Object was not correctly deleted", ocflUtil.getMainRepository().containsObject(objectId));
    }

    @Test
    public void testBackup() throws IOException {
        ocflUtil.updateRoot();
        ocflUtil.reloadRepository();
        assertFalse("Repository did not get switched or had preexisting data",
            ocflUtil.getMainRepository().containsObject(objectId));
        ocflUtil.restoreRoot();
        assertTrue("Backup was not restored on Main Repository", ocflUtil.getMainRepository().containsObject(objectId));
        ocflUtil.updateRoot();
        ocflUtil.setRepositoryKey("Adapt")
            .updateMainRepo()
            .restoreRoot();
        assertTrue("Backup was not restored on non Main Repository",
            ocflUtil.getAdaptRepository().containsObject(objectId));
    }

}
