package org.mycore.ocfl.niofs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.FileSystemException;
import java.nio.file.Files;

import org.junit.Test;
import org.mycore.common.MCRTransactionManager;
import org.mycore.datamodel.niofs.MCRPath;

public class MCROCFLVirtualObjectProviderTest extends MCROCFLNioTestCase {

    public MCROCFLVirtualObjectProviderTest(boolean remote, boolean purge) {
        super(remote, purge);
    }

    @Test
    public void exists() throws FileSystemException {
        MCROCFLVirtualObjectProvider virtualObjectProvider = MCROCFLFileSystemProvider.get().virtualObjectProvider();
        MCROCFLFileSystem fs = MCROCFLFileSystemProvider.getMCROCFLFileSystem();

        // check default
        assertTrue(virtualObjectProvider.exists(DERIVATE_1));
        assertFalse(virtualObjectProvider.exists(DERIVATE_2));

        // check after read map is filled
        assertFalse(Files.exists(MCRPath.getPath(DERIVATE_2, "/")));
        assertFalse(virtualObjectProvider.exists(DERIVATE_2));

        // check after write map is filled
        MCRTransactionManager.beginTransactions();
        assertFalse(Files.exists(MCRPath.getPath(DERIVATE_2, "/")));
        assertFalse(virtualObjectProvider.exists(DERIVATE_2));
        fs.createRoot(DERIVATE_2);
        assertTrue(virtualObjectProvider.exists(DERIVATE_2));
        MCRTransactionManager.commitTransactions();
        assertTrue(virtualObjectProvider.exists(DERIVATE_2));

        // check after rm
        MCRTransactionManager.beginTransactions();
        fs.removeRoot(DERIVATE_2);
        assertTrue(virtualObjectProvider.exists(DERIVATE_2));
        MCRTransactionManager.commitTransactions();

        if (isPurge()) {
            assertFalse(virtualObjectProvider.exists(DERIVATE_2));
        } else {
            assertTrue(virtualObjectProvider.exists(DERIVATE_2));
        }
    }

    @Test
    public void isDeleted() throws FileSystemException {
        MCROCFLVirtualObjectProvider virtualObjectProvider = MCROCFLFileSystemProvider.get().virtualObjectProvider();
        MCROCFLFileSystem fs = MCROCFLFileSystemProvider.getMCROCFLFileSystem();

        // check default
        assertFalse(virtualObjectProvider.isDeleted(DERIVATE_1));
        assertFalse(virtualObjectProvider.isDeleted(DERIVATE_2));

        // delete
        MCRTransactionManager.beginTransactions();
        fs.removeRoot(DERIVATE_1);
        assertFalse(virtualObjectProvider.isDeleted(DERIVATE_1));
        MCRTransactionManager.commitTransactions();

        // check after commit
        if (isPurge()) {
            assertFalse(virtualObjectProvider.isDeleted(DERIVATE_1));
        } else {
            assertTrue(virtualObjectProvider.isDeleted(DERIVATE_1));
        }
    }

}
