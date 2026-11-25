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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.MCRTransactionManager;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRExpandedObject;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.MCROCFLTestCaseHelper;
import org.mycore.ocfl.metadata.MCROCFLXMLMetadataManager;
import org.mycore.ocfl.niofs.MCROCFLFileSystemProvider;
import org.mycore.ocfl.niofs.MCROCFLFileSystemTransaction;
import org.mycore.ocfl.niofs.MCROCFLVirtualObjectProvider;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.test.MCROCFLSetupExtension;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.ocfl.test.PermutedParam;
import org.mycore.ocfl.util.MCROCFLObjectIDPrefixHelper;
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
            "an older delete version should not exist");
        assertThrows(FileSystemException.class,
            () -> MCROCFLCommands.restoreDerivateFromOCFL(JUNIT_DERIVATE_1, deleteVersion));
        assertTrue(virtualObjectProvider.isDeleted(JUNIT_DERIVATE_1), "derivate should still be deleted");
        assertEquals(deleteVersion, MCROCFLFileSystemProvider.get().getRepository().describeObject(ocflObjectId)
            .getHeadVersionNum().toString(), "no version should be written");
    }

    @TestTemplate
    public void createDeletedObjectFails() throws Exception {
        MCRObject object = MCROCFLTestCaseHelper.createObject(JUNIT_OBJECT_1);
        MCRMetadataManager.create(object);
        deleteObject(object);
        MCROCFLXMLMetadataManager metadataManager = (MCROCFLXMLMetadataManager) MCRXMLMetadataManager.obtainInstance();
        assertFalse(MCRMetadataManager.exists(object.getId()), "object should not exist");
        assertTrue(metadataManager.isDeleted(object.getId()), "object should be deleted");

        assertCreateRejected(() -> MCRMetadataManager.create(MCROCFLTestCaseHelper.createObject(JUNIT_OBJECT_1)));
        assertTrue(metadataManager.isDeleted(object.getId()), "object should still be deleted");
    }

    @TestTemplate
    public void createDeletedDerivateFails() throws Exception {
        MCRDerivate derivate = MCROCFLTestCaseHelper.loadObjectAndDerivate(JUNIT_OBJECT_1, JUNIT_DERIVATE_1);
        deleteDerivate(derivate);
        String ocflObjectId = MCROCFLObjectIDPrefixHelper.toDerivateObjectId(JUNIT_DERIVATE_1);
        int versions = repository.describeObject(ocflObjectId).getVersionMap().size();

        assertCreateRejected(
            () -> MCRMetadataManager.create(MCROCFLTestCaseHelper.createDerivate(JUNIT_OBJECT_1, JUNIT_DERIVATE_1)));
        assertEquals(versions, repository.describeObject(ocflObjectId).getVersionMap().size(),
            "the failed create should not write a version");
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
        assertEquals("Updated", restored.getMessage());
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

    /**
     * The event manager wraps the exception of the metadata store, so the cause chain is checked.
     */
    private static void assertCreateRejected(Executable create) {
        Throwable thrown = assertThrows(MCRException.class, create, "a deleted id should not be created again");
        while (thrown != null && !(thrown instanceof MCRPersistenceException)) {
            thrown = thrown.getCause();
        }
        assertNotNull(thrown, "the metadata store should reject the create");
        assertTrue(thrown.getMessage().contains("because it was deleted"), thrown.getMessage());
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
