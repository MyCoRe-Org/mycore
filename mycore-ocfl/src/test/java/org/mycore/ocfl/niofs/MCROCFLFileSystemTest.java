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
import org.mycore.common.MCRTransactionHelper;
import org.mycore.datamodel.niofs.MCRVersionedPath;

public class MCROCFLFileSystemTest extends MCROCFLTestCase {

    public MCROCFLFileSystemTest(boolean remote) {
        super(remote);
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
        MCRTransactionHelper.beginTransaction();
        fs.createRoot(DERIVATE_2);
        assertTrue(DERIVATE_2 + " should exist", Files.exists(MCRVersionedPath.head(DERIVATE_2, "/")));
        assertThrows("root should already exists " + DERIVATE_2,
            FileAlreadyExistsException.class, () -> fs.createRoot(DERIVATE_2));
        MCRTransactionHelper.commitTransaction();
        assertTrue(DERIVATE_2 + " should exist after commiting",
            Files.exists(MCRVersionedPath.head(DERIVATE_2, "/")));
    }

    @Test
    public void removeRoot() throws Exception {
        MCROCFLFileSystem fs = MCROCFLFileSystemProvider.getMCROCFLFileSystem();

        assertThrows("should not be possible to remove root because there is no active transaction",
            MCROCFLInactiveTransactionException.class, () -> fs.removeRoot(DERIVATE_1));
        fs.removeRoot(DERIVATE_2);

        MCRTransactionHelper.beginTransaction();
        fs.removeRoot(DERIVATE_1);
        assertFalse(Files.exists(MCRVersionedPath.head(DERIVATE_1, "/")));
        MCRTransactionHelper.commitTransaction();
        assertFalse(Files.exists(MCRVersionedPath.head(DERIVATE_1, "/")));
    }

    @Test
    public void getRootDirectories() throws FileSystemException {
        MCROCFLFileSystem fs = MCROCFLFileSystemProvider.getMCROCFLFileSystem();
        assertEquals(1, getRootDirectoryList(fs).size());

        // add
        MCRTransactionHelper.beginTransaction();
        fs.createRoot(DERIVATE_2);
        assertEquals(2, getRootDirectoryList(fs).size());
        MCRTransactionHelper.commitTransaction();
        assertEquals(2, getRootDirectoryList(fs).size());

        // rm
        MCRTransactionHelper.beginTransaction();
        fs.removeRoot(DERIVATE_2);
        assertEquals(1, getRootDirectoryList(fs).size());
        MCRTransactionHelper.commitTransaction();
        assertEquals(1, getRootDirectoryList(fs).size());

        MCRTransactionHelper.beginTransaction();
        fs.removeRoot(DERIVATE_1);
        assertEquals(0, getRootDirectoryList(fs).size());
        MCRTransactionHelper.commitTransaction();
        assertEquals(0, getRootDirectoryList(fs).size());
    }

    private static List<Path> getRootDirectoryList(MCROCFLFileSystem ocflFileSystem) {
        return StreamSupport
            .stream(ocflFileSystem.getRootDirectories().spliterator(), false)
            .toList();
    }

}
