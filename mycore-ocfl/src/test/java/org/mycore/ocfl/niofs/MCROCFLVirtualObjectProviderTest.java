package org.mycore.ocfl.niofs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.util.Date;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTransactionManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.MCROCFLTestCaseHelper;
import org.mycore.ocfl.metadata.MCROCFLXMLMetadataManager;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.test.PermutedParam;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.ocfl.test.MCROCFLSetupExtension;
import org.mycore.test.MyCoReTest;

import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.VersionInfo;

@MyCoReTest
@ExtendWith({ MCRPermutationExtension.class, MCROCFLSetupExtension.class })
public class MCROCFLVirtualObjectProviderTest {

    protected MCROCFLRepository repository;

    @PermutedParam
    private boolean remote;

    @PermutedParam
    private boolean purge;

    @TestTemplate
    public void exists() throws FileSystemException {
        MCROCFLVirtualObjectProvider virtualObjectProvider = MCROCFLFileSystemProvider.get().virtualObjectProvider();
        MCROCFLFileSystem fs = MCROCFLFileSystemProvider.getMCROCFLFileSystem();

        // check default
        assertTrue(virtualObjectProvider.exists(MCROCFLTestCaseHelper.DERIVATE_1));
        assertFalse(virtualObjectProvider.exists(MCROCFLTestCaseHelper.DERIVATE_2));

        // check after read map is filled
        assertFalse(Files.exists(MCRPath.getPath(MCROCFLTestCaseHelper.DERIVATE_2, "/")));
        assertFalse(virtualObjectProvider.exists(MCROCFLTestCaseHelper.DERIVATE_2));

        // check after write map is filled
        MCRTransactionManager.beginTransactions();
        assertFalse(Files.exists(MCRPath.getPath(MCROCFLTestCaseHelper.DERIVATE_2, "/")));
        assertFalse(virtualObjectProvider.exists(MCROCFLTestCaseHelper.DERIVATE_2));
        fs.createRoot(MCROCFLTestCaseHelper.DERIVATE_2);
        assertTrue(virtualObjectProvider.exists(MCROCFLTestCaseHelper.DERIVATE_2));
        MCRTransactionManager.commitTransactions();
        assertTrue(virtualObjectProvider.exists(MCROCFLTestCaseHelper.DERIVATE_2));

        // check after rm
        MCRTransactionManager.beginTransactions();
        fs.removeRoot(MCROCFLTestCaseHelper.DERIVATE_2);
        assertTrue(virtualObjectProvider.exists(MCROCFLTestCaseHelper.DERIVATE_2));
        MCRTransactionManager.commitTransactions();

        if (purge) {
            assertFalse(virtualObjectProvider.exists(MCROCFLTestCaseHelper.DERIVATE_2));
        } else {
            assertTrue(virtualObjectProvider.exists(MCROCFLTestCaseHelper.DERIVATE_2));
        }
    }

    @TestTemplate
    public void isDeleted() throws FileSystemException {
        MCROCFLVirtualObjectProvider virtualObjectProvider = MCROCFLFileSystemProvider.get().virtualObjectProvider();
        MCROCFLFileSystem fs = MCROCFLFileSystemProvider.getMCROCFLFileSystem();

        // check default
        assertFalse(virtualObjectProvider.isDeleted(MCROCFLTestCaseHelper.DERIVATE_1));
        assertFalse(virtualObjectProvider.isDeleted(MCROCFLTestCaseHelper.DERIVATE_2));

        // delete
        MCRTransactionManager.beginTransactions();
        fs.removeRoot(MCROCFLTestCaseHelper.DERIVATE_1);
        assertFalse(virtualObjectProvider.isDeleted(MCROCFLTestCaseHelper.DERIVATE_1));
        MCRTransactionManager.commitTransactions();

        // check after commit
        if (purge) {
            assertFalse(virtualObjectProvider.isDeleted(MCROCFLTestCaseHelper.DERIVATE_1));
        } else {
            assertTrue(virtualObjectProvider.isDeleted(MCROCFLTestCaseHelper.DERIVATE_1));
        }
    }

    /**
     * After a purge the id can be used again. The new object must not be read from cached versions of the purged
     * one.
     */
    @TestTemplate
    public void purgeInvalidatesCachedVersions() throws Exception {
        String derivate = MCROCFLTestCaseHelper.DERIVATE_2;
        String ocflObjectId = MCROCFLTestCaseHelper.DERIVATE_2_OBJECT_ID;
        createObject(ocflObjectId, "files/old.txt");
        // fill the read cache with v1
        assertTrue(Files.exists(MCRVersionedPath.getPath(derivate, "v1", "/old.txt")));

        if (purge) {
            // purge through the file system
            MCRTransactionManager.beginTransactions();
            MCROCFLFileSystemProvider.getMCROCFLFileSystem().removeRoot(derivate);
            MCRTransactionManager.commitTransactions();
        } else {
            // purge through the metadata manager
            MCROCFLXMLMetadataManager metadataManager = new MCROCFLXMLMetadataManager();
            metadataManager.setRepositoryKey(repository.getId());
            metadataManager.purge(MCRObjectID.getInstance(derivate), new Date(), null);
        }
        assertFalse(repository.containsObject(ocflObjectId), "the object should be purged");

        // use the id again
        createObject(ocflObjectId, "files/new.txt");
        assertTrue(Files.exists(MCRVersionedPath.getPath(derivate, "v1", "/new.txt")), "v1 should be the new object");
        assertFalse(Files.exists(MCRVersionedPath.getPath(derivate, "v1", "/old.txt")),
            "v1 should not be read from the cache of the purged object");
    }

    private void createObject(String ocflObjectId, String path) {
        repository.updateObject(ObjectVersionId.head(ocflObjectId), new VersionInfo().setMessage("Created"),
            updater -> updater.writeFile(new ByteArrayInputStream(new byte[] { 1 }), path));
    }

}
