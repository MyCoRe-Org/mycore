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

package org.mycore.ocfl.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRUsageException;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.MCRTransactionManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRExpandedObject;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.MCROCFLTestCaseHelper;
import org.mycore.ocfl.niofs.MCROCFLFileSystemProvider;
import org.mycore.ocfl.niofs.MCROCFLFileSystemTransaction;
import org.mycore.ocfl.niofs.MCROCFLVirtualObject;
import org.mycore.ocfl.niofs.MCROCFLVirtualObjectProvider;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.test.MCROCFLSetupExtension;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.ocfl.test.PermutedParam;
import org.mycore.ocfl.util.MCROCFLObjectIDPrefixHelper;
import org.mycore.ocfl.util.MCROCFLVersionHelper;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.VersionInfo;

@MyCoReTest
@ExtendWith({ MCRJPAExtension.class, MCRPermutationExtension.class, MCROCFLSetupExtension.class })
@MCROCFLSetupExtension.LoadDefaultDerivate(false)
@MCRTestConfiguration(
    properties = {
        // set jpa mappings, only use mycore-base
        @MCRTestProperty(key = "MCR.JPA.MappingFileNames", string = "META-INF/mycore-base-mappings.xml"),
        // use ocfl metadata
        @MCRTestProperty(key = "MCR.Metadata.Type.object", string = "true"),
        @MCRTestProperty(key = "MCR.Metadata.Type.derivate", string = "true"),
        // set free access
        @MCRTestProperty(key = "MCR.Access.Class", string = "org.mycore.access.MCRAccessBaseImpl"),
        @MCRTestProperty(key = "MCR.Access.Strategy.Class", string = "org.mycore.ocfl.test.AlwaysTrueStrategy")
    })
public class MCROCFLCommandsTest {

    public static final String JUNIT_DERIVATE_1 = "junit_derivate_00000001";
    public static final String JUNIT_OBJECT_1 = "junit_object_00000001";

    protected MCROCFLRepository repository;

    @PermutedParam
    private boolean remote;

    private final boolean purge = false;

    @TestTemplate
    public void restoreDerivate() throws Exception {
        // prepare
        MCRDerivate derivate = MCROCFLTestCaseHelper.loadObjectAndDerivate(JUNIT_OBJECT_1, JUNIT_DERIVATE_1);
        deleteDerivate(derivate);
        MCRObjectID derivateId = MCRObjectID.getInstance(JUNIT_DERIVATE_1);
        assertFalse(MCRMetadataManager.exists(derivateId), "derivate should not exist");

        // restore
        MCROCFLCommands.restoreDerivateFromOCFL(JUNIT_DERIVATE_1, "v2");

        // test
        assertTrue(MCRMetadataManager.exists(derivateId), "derivate should exist");
        try(Stream<Path> directoryStream = Files.list(MCRVersionedPath.head(JUNIT_DERIVATE_1, "/"))) {
            assertEquals(3, directoryStream.toList().size());
        }
        MCRExpandedObject mcrObject = MCRMetadataManager.retrieveMCRExpandedObject(derivate.getOwnerID());
        assertTrue(mcrObject.getStructure().containsDerivate(derivateId));
    }

    @TestTemplate
    public void restoreDeleteVersionFails() throws Exception {
        // prepare
        MCRDerivate derivate = MCROCFLTestCaseHelper.loadObjectAndDerivate(JUNIT_OBJECT_1, JUNIT_DERIVATE_1);
        deleteDerivate(derivate);
        MCROCFLVirtualObjectProvider virtualObjectProvider = MCROCFLFileSystemProvider.get().virtualObjectProvider();
        String ocflObjectId = MCROCFLObjectIDPrefixHelper.toDerivateObjectId(JUNIT_DERIVATE_1);
        String deleteVersion = MCROCFLFileSystemProvider.get().getRepository().describeObject(ocflObjectId)
            .getHeadVersionNum().toString();

        // test
        assertFalse(virtualObjectProvider.exists(JUNIT_DERIVATE_1, deleteVersion),
            "a delete version should not exist");
        virtualObjectProvider.get(JUNIT_DERIVATE_1, deleteVersion);
        assertFalse(virtualObjectProvider.exists(JUNIT_DERIVATE_1, deleteVersion),
            "an already read delete version should not exist either");
        assertThrows(FileSystemException.class,
            () -> MCROCFLCommands.restoreDerivateFromOCFL(JUNIT_DERIVATE_1, deleteVersion));
        assertTrue(virtualObjectProvider.isDeleted(JUNIT_DERIVATE_1), "derivate should still be deleted");
        assertEquals(deleteVersion, MCROCFLFileSystemProvider.get().getRepository().describeObject(ocflObjectId)
            .getHeadVersionNum().toString(), "no version should be written");
    }

    @TestTemplate
    public void createDeletedObject() throws Exception {
        MCRObject object = MCROCFLTestCaseHelper.createObject(JUNIT_OBJECT_1);
        MCRMetadataManager.create(object);
        deleteObject(object);
        assertFalse(MCRMetadataManager.exists(object.getId()), "object should not exist");

        // like the old SVN store, a deleted id can be created again and the history is kept
        MCRMetadataManager.create(MCROCFLTestCaseHelper.createObject(JUNIT_OBJECT_1));

        assertTrue(MCRMetadataManager.exists(object.getId()), "object should exist again");
        String ocflObjectId = MCROCFLObjectIDPrefixHelper.MCROBJECT + JUNIT_OBJECT_1;
        assertEquals(3, repository.describeObject(ocflObjectId).getVersionMap().size(),
            "the history should be kept");
    }

    @TestTemplate
    public void createDeletedDerivate() throws Exception {
        MCRDerivate derivate = MCROCFLTestCaseHelper.loadObjectAndDerivate(JUNIT_OBJECT_1, JUNIT_DERIVATE_1);
        deleteDerivate(derivate);
        MCRObjectID derivateId = derivate.getId();
        String ocflObjectId = MCROCFLObjectIDPrefixHelper.toDerivateObjectId(JUNIT_DERIVATE_1);
        int versions = repository.describeObject(ocflObjectId).getVersionMap().size();
        // reading the deleted derivate must not prevent creating it again
        assertFalse(Files.exists(MCRVersionedPath.head(JUNIT_DERIVATE_1, "/")), "root should not exist");

        // like the old SVN store, a deleted id can be created again and the history is kept
        MCRTransactionManager.requireTransactions(MCROCFLFileSystemTransaction.class);
        MCRMetadataManager.create(MCROCFLTestCaseHelper.createDerivate(JUNIT_OBJECT_1, JUNIT_DERIVATE_1));
        MCROCFLTestCaseHelper.loadDerivate(JUNIT_DERIVATE_1);
        MCRTransactionManager.commitTransactions(MCROCFLFileSystemTransaction.class);

        assertTrue(MCRMetadataManager.exists(derivateId), "derivate should exist again");
        try (Stream<Path> directoryStream = Files.list(MCRVersionedPath.head(JUNIT_DERIVATE_1, "/"))) {
            assertEquals(3, directoryStream.toList().size(), "the files should be created again");
        }
        assertTrue(repository.describeObject(ocflObjectId).getVersionMap().size() > versions,
            "the history should be kept");
    }

    @TestTemplate
    public void createDeletedDerivateWithLeftoverKeepFile() throws Exception {
        MCRDerivate derivate = MCROCFLTestCaseHelper.loadObjectAndDerivate(JUNIT_OBJECT_1, JUNIT_DERIVATE_1);
        deleteDerivate(derivate);
        // older releases left the .keep files of empty directories in the delete version
        String ocflObjectId = MCROCFLObjectIDPrefixHelper.toDerivateObjectId(JUNIT_DERIVATE_1);
        String keepFile = MCROCFLVirtualObject.FILES_DIRECTORY + "leftover/.keep";
        repository.updateObject(ObjectVersionId.head(ocflObjectId),
            new VersionInfo().setMessage(MCROCFLVersionHelper.MESSAGE_DELETED),
            updater -> updater.writeFile(new ByteArrayInputStream(new byte[0]), keepFile));
        MCROCFLFileSystemProvider.get().virtualObjectProvider().invalidate(JUNIT_DERIVATE_1, null);
        assertNotNull(repository.getObject(ObjectVersionId.head(ocflObjectId)).getFile(keepFile),
            "the delete version should contain the leftover .keep file");

        // the metadata is created before the root, both on top of the delete version
        MCRTransactionManager.requireTransactions(MCROCFLFileSystemTransaction.class);
        MCRMetadataManager.create(MCROCFLTestCaseHelper.createDerivate(JUNIT_OBJECT_1, JUNIT_DERIVATE_1));
        MCROCFLTestCaseHelper.loadDerivate(JUNIT_DERIVATE_1);
        MCRTransactionManager.commitTransactions(MCROCFLFileSystemTransaction.class);

        assertTrue(MCRMetadataManager.exists(derivate.getId()), "derivate should exist again");
        assertNull(repository.getObject(ObjectVersionId.head(ocflObjectId)).getFile(keepFile),
            "the leftover .keep file should be removed by the create");
    }

    @TestTemplate
    public void restoreMetadataOfDeletedDerivateFails() throws Exception {
        MCRDerivate derivate = MCROCFLTestCaseHelper.loadObjectAndDerivate(JUNIT_OBJECT_1, JUNIT_DERIVATE_1);
        deleteDerivate(derivate);
        String ocflObjectId = MCROCFLObjectIDPrefixHelper.toDerivateObjectId(JUNIT_DERIVATE_1);
        int versions = repository.describeObject(ocflObjectId).getVersionMap().size();

        assertThrows(MCRUsageException.class, () -> MCROCFLCommands.restoreObjFromOCFLVersioned(JUNIT_DERIVATE_1,
            "v1"), "the metadata of a deleted derivate should not be restored without its files");
        assertFalse(MCRMetadataManager.exists(derivate.getId()), "derivate should still not exist");
        assertEquals(versions, repository.describeObject(ocflObjectId).getVersionMap().size(),
            "no version should be written");
    }

    @TestTemplate
    public void restoreDeletedObject() throws Exception {
        MCRObject object = MCROCFLTestCaseHelper.createObject(JUNIT_OBJECT_1);
        MCRMetadataManager.create(object);
        deleteObject(object);
        assertFalse(MCRMetadataManager.exists(object.getId()), "object should not exist");

        MCROCFLCommands.restoreObjFromOCFLVersioned(JUNIT_OBJECT_1, "v1");

        assertTrue(MCRMetadataManager.exists(object.getId()), "object should exist again");
        String ocflObjectId = MCROCFLObjectIDPrefixHelper.MCROBJECT + JUNIT_OBJECT_1;
        assertEquals(3, repository.describeObject(ocflObjectId).getVersionMap().size(),
            "restore should add a version and keep the history");
        VersionInfo restored = repository.getObject(ObjectVersionId.head(ocflObjectId)).getVersionInfo();
        assertEquals(MCROCFLVersionHelper.MESSAGE_CREATED, restored.getMessage(),
            "the restore of a deleted object should create it again");
        OffsetDateTime deleted = repository.getObject(ObjectVersionId.version(ocflObjectId, "v2"))
            .getVersionInfo().getCreated();
        assertFalse(restored.getCreated().isBefore(deleted),
            "the restore version should not be older than the delete version, but was " + restored.getCreated());
    }

    @TestTemplate
    public void describeObject() throws MCRAccessException, URISyntaxException, IOException {
        MCRDerivate derivate = MCROCFLTestCaseHelper.loadObjectAndDerivate(JUNIT_OBJECT_1, JUNIT_DERIVATE_1);
        MCROCFLCommands.describeObject(MCROCFLObjectIDPrefixHelper.toDerivateObjectId(derivate.getId().toString()),
            repository.getId());
    }

    private static void deleteObject(MCRObject object) throws Exception {
        MCRTransactionManager.requireTransactions(MCROCFLFileSystemTransaction.class);
        MCRMetadataManager.delete(object);
        MCRTransactionManager.commitTransactions(MCROCFLFileSystemTransaction.class);
    }

    private static void deleteDerivate(MCRDerivate derivate) throws MCRAccessException {
        MCRTransactionManager.requireTransactions(MCROCFLFileSystemTransaction.class);
        MCRMetadataManager.delete(derivate);
        MCRTransactionManager.commitTransactions(MCROCFLFileSystemTransaction.class);
    }

}
