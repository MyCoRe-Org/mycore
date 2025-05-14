package org.mycore.ocfl.niofs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mycore.ocfl.MCROCFLTestCaseHelper.DERIVATE_1;
import static org.mycore.ocfl.MCROCFLTestCaseHelper.DERIVATE_2;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTransactionManager;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.test.MCROCFLSetupExtension;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.ocfl.test.PermutedParam;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith({ MCRPermutationExtension.class, MCROCFLSetupExtension.class })
public class MCROCFLFileSystemTest {

    protected MCROCFLRepository repository;

    @PermutedParam
    private boolean remote;

    @PermutedParam
    private boolean purge;

    @TestTemplate
    public void createRoot() throws Exception {
        MCROCFLFileSystem fs = MCROCFLFileSystemProvider.getMCROCFLFileSystem();
        // check already existing
        assertThrows(FileAlreadyExistsException.class, () -> fs.createRoot(DERIVATE_1),
            "root should already exists " + DERIVATE_1);
        // check active transaction
        assertThrows(MCROCFLInactiveTransactionException.class, () -> fs.createRoot(DERIVATE_2),
            "should not be possible to create root because there is no active transaction");

        // begin transaction
        MCRTransactionManager.beginTransactions();
        fs.createRoot(DERIVATE_2);
        assertTrue(Files.exists(MCRVersionedPath.head(DERIVATE_2, "/")), DERIVATE_2 + " should exist");
        assertThrows(FileAlreadyExistsException.class, () -> fs.createRoot(DERIVATE_2),
            "root should already exists " + DERIVATE_2);
        MCRTransactionManager.commitTransactions();
        assertTrue(Files.exists(MCRVersionedPath.head(DERIVATE_2, "/")),
            DERIVATE_2 + " should exist after commiting");
    }

    @TestTemplate
    public void removeRoot() throws Exception {
        MCROCFLFileSystem fs = MCROCFLFileSystemProvider.getMCROCFLFileSystem();

        assertThrows(MCROCFLInactiveTransactionException.class, () -> fs.removeRoot(DERIVATE_1),
            "should not be possible to remove root because there is no active transaction");
        fs.removeRoot(DERIVATE_2);

        MCRTransactionManager.beginTransactions();
        fs.removeRoot(DERIVATE_1);
        assertFalse(Files.exists(MCRVersionedPath.head(DERIVATE_1, "/")), "root path should not exist");
        MCRTransactionManager.commitTransactions();
        assertFalse(Files.exists(MCRVersionedPath.head(DERIVATE_1, "/")), "root path should not exist");
    }

    @TestTemplate
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
