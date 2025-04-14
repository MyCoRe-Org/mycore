package org.mycore.ocfl.niofs.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTransactionManager;
import org.mycore.ocfl.niofs.MCROCFLInactiveTransactionException;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.test.PermutedParam;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.ocfl.test.MCROCFLSetupExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith({ MCRPermutationExtension.class, MCROCFLSetupExtension.class })
public class MCROCFLDefaultTransactionalTempFileStorageTest extends MCROCFLStorageTestCase {

    private MCROCFLDefaultTransactionalTempFileStorage storage;

    // TODO why do i need the repo here?
    protected MCROCFLRepository repository;

    @PermutedParam
    private boolean remote;

    @PermutedParam
    private boolean purge;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        storage = new MCROCFLDefaultTransactionalTempFileStorage(rootPath);
    }

    @Override
    public MCROCFLDefaultTransactionalTempFileStorage getStorage() {
        return storage;
    }

    @TestTemplate
    public void exists() throws IOException {
        MCRTransactionManager.beginTransactions();
        Assertions.assertFalse(storage.exists(path1), "Path should not exist before any operation");
        write(path1);
        Assertions.assertTrue(storage.exists(path1), "Path should exist after write operation");
        MCRTransactionManager.commitTransactions();
    }

    @TestTemplate
    public void toPhysicalPath() {
        MCRTransactionManager.beginTransactions();
        Assertions.assertTrue(storage.toPhysicalPath(path1).endsWith("1/owner1/v0/file1"));
        MCRTransactionManager.commitTransactions();
        MCRTransactionManager.beginTransactions();
        Assertions.assertTrue(storage.toPhysicalPath(path1).endsWith("2/owner1/v0/file1"));
        MCRTransactionManager.commitTransactions();
    }

    @TestTemplate
    public void copyInputStream() throws IOException, MCROCFLInactiveTransactionException {
        MCRTransactionManager.beginTransactions();
        storage.copy(new ByteArrayInputStream(new byte[] { 1 }), path1);
        storage.copy(new ByteArrayInputStream(new byte[] { 2 }), path2);
        storage.copy(new ByteArrayInputStream(new byte[] { 3 }), path3);
        Assertions.assertTrue(storage.exists(path1), "'path1' should exist");
        Assertions.assertTrue(storage.exists(path2), "'path2' should exist");
        Assertions.assertTrue(storage.exists(path3), "'path3' should exist");
        MCRTransactionManager.commitTransactions();
    }

    @TestTemplate
    public void copyWithSourceAndTarget() throws IOException, MCROCFLInactiveTransactionException {
        MCRTransactionManager.beginTransactions();
        write(path1);
        storage.copy(path1, path2);
        Assertions.assertTrue(storage.exists(path1), "'path1' should exist");
        Assertions.assertTrue(storage.exists(path2), "'path2' should exist");
        storage.copy(path1, path3);
        Assertions.assertTrue(storage.exists(path1), "'path1' should exist");
        Assertions.assertTrue(storage.exists(path3), "'path3' should exist");
        MCRTransactionManager.commitTransactions();
    }

    @TestTemplate
    public void move() throws IOException, MCROCFLInactiveTransactionException {
        MCRTransactionManager.beginTransactions();
        write(path1);
        storage.move(path1, path2);
        Assertions.assertFalse(storage.exists(path1), "'path1' should not exist after move");
        Assertions.assertTrue(storage.exists(path2), "'path2' should exist after move");
        MCRTransactionManager.commitTransactions();
    }

    @TestTemplate
    public void deleteIfExists() throws IOException, MCROCFLInactiveTransactionException {
        MCRTransactionManager.beginTransactions();
        storage.copy(new ByteArrayInputStream(new byte[] { 1 }), path1);
        Assertions.assertTrue(storage.exists(path1), "'path1' should exist before deletion");
        storage.deleteIfExists(path1);
        Assertions.assertFalse(storage.exists(path1), "'path1' should not exist after deletion");
        MCRTransactionManager.commitTransactions();
    }

    @TestTemplate
    public void newByteChannelWriteOperation() throws IOException, MCROCFLInactiveTransactionException {
        MCRTransactionManager.beginTransactions();
        write(path1);
        Assertions.assertTrue(storage.exists(path1), "'path1' should exist after write operation");
        write(path2);
        Assertions.assertTrue(storage.exists(path2), "'path2' should exist after write operation");
        write(path3);
        Assertions.assertTrue(storage.exists(path3), "'path3' should exist after write operation");
        MCRTransactionManager.commitTransactions();
    }

}
