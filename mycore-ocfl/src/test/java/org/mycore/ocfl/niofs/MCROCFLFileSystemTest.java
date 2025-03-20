package org.mycore.ocfl.niofs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.StreamSupport;

import org.junit.Test;
import org.mycore.common.MCRTransactionManager;
import org.mycore.datamodel.niofs.MCRVersionedPath;

public class MCROCFLFileSystemTest extends MCROCFLNioTestCase {

    public MCROCFLFileSystemTest(boolean remote, boolean purge) {
        super(remote, purge);
    }

    @Test
    public void createRoot() throws Exception {
        MCROCFLFileSystem fs = MCROCFLFileSystemProvider.getMCROCFLFileSystem();
        // check already existing
        assertThrows("root should already exists " + DERIVATE_1,
            FileAlreadyExistsException.class, () -> fs.createRoot(DERIVATE_1));
        // check active transaction
        assertThrows("should not be possible to create root because there is no active transaction",
            MCROCFLInactiveTransactionException.class, () -> fs.createRoot(DERIVATE_2));

        // begin transaction
        MCRTransactionManager.beginTransactions();
        fs.createRoot(DERIVATE_2);
        assertTrue(DERIVATE_2 + " should exist", Files.exists(MCRVersionedPath.head(DERIVATE_2, "/")));
        assertThrows("root should already exists " + DERIVATE_2,
            FileAlreadyExistsException.class, () -> fs.createRoot(DERIVATE_2));
        MCRTransactionManager.commitTransactions();
        assertTrue(DERIVATE_2 + " should exist after commiting",
            Files.exists(MCRVersionedPath.head(DERIVATE_2, "/")));
    }

    @Test
    public void removeRoot() throws Exception {
        MCROCFLFileSystem fs = MCROCFLFileSystemProvider.getMCROCFLFileSystem();

        assertThrows("should not be possible to remove root because there is no active transaction",
            MCROCFLInactiveTransactionException.class, () -> fs.removeRoot(DERIVATE_1));
        fs.removeRoot(DERIVATE_2);

        MCRTransactionManager.beginTransactions();
        fs.removeRoot(DERIVATE_1);
        assertFalse("root path should not exist", Files.exists(MCRVersionedPath.head(DERIVATE_1, "/")));
        MCRTransactionManager.commitTransactions();
        assertFalse("root path should not exist", Files.exists(MCRVersionedPath.head(DERIVATE_1, "/")));
    }

    @Test
    public void getRootDirectories() throws FileSystemException {
        MCROCFLFileSystem fs = MCROCFLFileSystemProvider.getMCROCFLFileSystem();
        assertEquals(1, getRootDirectoryList(fs).size());

        // add
        MCRTransactionManager.beginTransactions();
        fs.createRoot(DERIVATE_2);
        assertEquals(2, getRootDirectoryList(fs).size());
        MCRTransactionManager.commitTransactions();
        assertEquals(2, getRootDirectoryList(fs).size());

        // rm
        MCRTransactionManager.beginTransactions();
        fs.removeRoot(DERIVATE_2);
        assertEquals(1, getRootDirectoryList(fs).size());
        MCRTransactionManager.commitTransactions();
        assertEquals(1, getRootDirectoryList(fs).size());

        MCRTransactionManager.beginTransactions();
        fs.removeRoot(DERIVATE_1);
        assertEquals(0, getRootDirectoryList(fs).size());
        MCRTransactionManager.commitTransactions();
        assertEquals(0, getRootDirectoryList(fs).size());
    }

    private static List<Path> getRootDirectoryList(MCROCFLFileSystem ocflFileSystem) {
        return StreamSupport
            .stream(ocflFileSystem.getRootDirectories().spliterator(), false)
            .toList();
    }

}
