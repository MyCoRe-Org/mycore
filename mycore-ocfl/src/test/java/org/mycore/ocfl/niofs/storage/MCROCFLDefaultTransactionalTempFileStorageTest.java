package org.mycore.ocfl.niofs.storage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Test;
import org.mycore.common.MCRTransactionManager;
import org.mycore.ocfl.niofs.MCROCFLInactiveTransactionException;

public class MCROCFLDefaultTransactionalTempFileStorageTest extends MCROCFLStorageTestCase {

    private MCROCFLDefaultTransactionalTempFileStorage storage;

    public MCROCFLDefaultTransactionalTempFileStorageTest(boolean remote) {
        super(remote);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        storage = new MCROCFLDefaultTransactionalTempFileStorage(rootPath);
    }

    @Override
    public MCROCFLDefaultTransactionalTempFileStorage getStorage() {
        return storage;
    }

    @Test
    public void exists() throws IOException {
        MCRTransactionManager.beginTransactions();
        assertFalse("Path should not exist before any operation", storage.exists(path1));
        write(path1);
        assertTrue("Path should exist after write operation", storage.exists(path1));
        MCRTransactionManager.commitTransactions();
    }

    @Test
    public void toPhysicalPath() {
        MCRTransactionManager.beginTransactions();
        assertTrue(storage.toPhysicalPath(path1).endsWith("1/owner1/v0/file1"));
        MCRTransactionManager.commitTransactions();
        MCRTransactionManager.beginTransactions();
        assertTrue(storage.toPhysicalPath(path1).endsWith("2/owner1/v0/file1"));
        MCRTransactionManager.commitTransactions();
    }

    @Test
    public void copyInputStream() throws IOException, MCROCFLInactiveTransactionException {
        MCRTransactionManager.beginTransactions();
        storage.copy(new ByteArrayInputStream(new byte[] { 1 }), path1);
        storage.copy(new ByteArrayInputStream(new byte[] { 2 }), path2);
        storage.copy(new ByteArrayInputStream(new byte[] { 3 }), path3);
        assertTrue("'path1' should exist", storage.exists(path1));
        assertTrue("'path2' should exist", storage.exists(path2));
        assertTrue("'path3' should exist", storage.exists(path3));
        MCRTransactionManager.commitTransactions();
    }

    @Test
    public void copyWithSourceAndTarget() throws IOException, MCROCFLInactiveTransactionException {
        MCRTransactionManager.beginTransactions();
        write(path1);
        storage.copy(path1, path2);
        assertTrue("'path1' should exist", storage.exists(path1));
        assertTrue("'path2' should exist", storage.exists(path2));
        storage.copy(path1, path3);
        assertTrue("'path1' should exist", storage.exists(path1));
        assertTrue("'path3' should exist", storage.exists(path3));
        MCRTransactionManager.commitTransactions();
    }

    @Test
    public void move() throws IOException, MCROCFLInactiveTransactionException {
        MCRTransactionManager.beginTransactions();
        write(path1);
        storage.move(path1, path2);
        assertFalse("'path1' should not exist after move", storage.exists(path1));
        assertTrue("'path2' should exist after move", storage.exists(path2));
        MCRTransactionManager.commitTransactions();
    }

    @Test
    public void deleteIfExists() throws IOException, MCROCFLInactiveTransactionException {
        MCRTransactionManager.beginTransactions();
        storage.copy(new ByteArrayInputStream(new byte[] { 1 }), path1);
        assertTrue("'path1' should exist before deletion", storage.exists(path1));
        storage.deleteIfExists(path1);
        assertFalse("'path1' should not exist after deletion", storage.exists(path1));
        MCRTransactionManager.commitTransactions();
    }

    @Test
    public void newByteChannelWriteOperation() throws IOException, MCROCFLInactiveTransactionException {
        MCRTransactionManager.beginTransactions();
        write(path1);
        assertTrue("'path1' should exist after write operation", storage.exists(path1));
        write(path2);
        assertTrue("'path2' should exist after write operation", storage.exists(path2));
        write(path3);
        assertTrue("'path3' should exist after write operation", storage.exists(path3));
        MCRTransactionManager.commitTransactions();
    }

}
