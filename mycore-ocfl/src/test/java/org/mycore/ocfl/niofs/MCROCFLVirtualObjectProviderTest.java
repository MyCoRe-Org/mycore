package org.mycore.ocfl.niofs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.FileSystemException;
import java.nio.file.Files;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTransactionManager;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.ocfl.MCROCFLTestCaseHelper;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.test.PermutedParam;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.ocfl.test.MCROCFLSetupExtension;
import org.mycore.test.MyCoReTest;

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

}
