package org.mycore.ocfl.niofs.storage;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Test;
import org.mycore.common.MCRTransactionManager;
import org.mycore.ocfl.niofs.MCROCFLInactiveTransactionException;

public class MCROCFLHybridStorageTest extends MCROCFLStorageTestCase {

    private MCROCFLHybridStorage storage;

    public MCROCFLHybridStorageTest(boolean remote, boolean purge) {
        super(remote, purge);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        FileCountEvictionStrategy evictionStrategy = new FileCountEvictionStrategy(2);
        storage = new MCROCFLHybridStorage(rootPath, evictionStrategy);
    }

    @Override
    public MCROCFLHybridStorage getStorage() {
        return storage;
    }

    @Test
    public void exists() throws IOException {
        assertFalse("'path1' should not exist before any operation", storage.exists(path1));
        write(path1);
        assertTrue("'path1' should exist after write operation", storage.exists(path1));
        MCRTransactionManager.beginTransactions();
        assertTrue("'path1' should exist after transaction start", storage.exists(path1));
        MCRTransactionManager.commitTransactions();
    }

    @Test
    public void toPhysicalPath() throws IOException {
        String rollingDirectory = "/" + MCROCFLHybridStorage.ROLLING_DIRECTORY + "/";
        String transactionDirectory = "/" + MCROCFLHybridStorage.TRANSACTION_DIRECTORY + "/";
        assertTrue("should access rolling store", storage.toPhysicalPath(path1).toString().contains(rollingDirectory));
        MCRTransactionManager.beginTransactions();
        assertTrue("should access rolling store because the transaction store is empty",
            storage.toPhysicalPath(path1).toString().contains(rollingDirectory));
        write(path1);
        assertTrue("should access transactional store because a file was written there",
            storage.toPhysicalPath(path1).toString().contains(transactionDirectory));
        MCRTransactionManager.commitTransactions();
        assertTrue("should access rolling store because there is no active transaction",
            storage.toPhysicalPath(path1).toString().contains(rollingDirectory));
        MCRTransactionManager.beginTransactions();
        assertTrue("should access rolling store because the transaction store is empty",
            storage.toPhysicalPath(path1).toString().contains(rollingDirectory));
        MCRTransactionManager.commitTransactions();
    }

    @Test
    public void copyInputStream() throws IOException, MCROCFLInactiveTransactionException {
        storage.copy(new ByteArrayInputStream(new byte[] { 1 }), path1);
        assertTrue("'path1' should exist", storage.exists(path1));

        MCRTransactionManager.beginTransactions();
        assertTrue("'path1' should exist", storage.exists(path1));
        storage.copy(new ByteArrayInputStream(new byte[] { 2 }), path1);
        assertTrue("'path1' should exist", storage.exists(path1));
        assertArrayEquals("'path1' should have the content of the transactional store", new byte[] { 2 }, read(path1));
        MCRTransactionManager.commitTransactions();

        assertArrayEquals("'path1' should have the content of the rollover store", new byte[] { 1 }, read(path1));
    }

    @Test
    public void copyWithSourceAndTarget() throws IOException, MCROCFLInactiveTransactionException {
        byte[] path1Data = { 1 };
        byte[] path3Data = { 2 };

        write(path1, path1Data);
        storage.copy(path1, path2);
        assertTrue("'path1' should exist", storage.exists(path1));
        assertTrue("'path2' should exist", storage.exists(path2));
        assertArrayEquals("'path1' should have the content of the rollover store", path1Data, read(path1));
        assertArrayEquals("'path2' should have the content of the rollover store", path1Data, read(path2));

        MCRTransactionManager.beginTransactions();
        assertArrayEquals("'path1' should have the content of the rollover store", path1Data, read(path1));
        assertArrayEquals("'path2' should have the content of the rollover store", path1Data, read(path2));
        storage.copy(path1, path3);
        assertArrayEquals("'path3' should have the content of the rollover store", path1Data, read(path3));
        write(path3, path3Data);
        storage.copy(path3, path2);
        assertArrayEquals("'path2' should have the content of the transactional store", path3Data, read(path2));
        MCRTransactionManager.commitTransactions();
    }

    @Test
    public void move() throws IOException {
        storage.copy(new ByteArrayInputStream(new byte[] { 1 }), path1);
        storage.move(path1, path2);
        assertTrue("'path2' should exist after move", storage.exists(path2));

        MCRTransactionManager.beginTransactions();
        storage.move(path2, path3);
        assertTrue("'path3' should exist after move", storage.exists(path3));
        MCRTransactionManager.commitTransactions();
    }

    @Test
    public void deleteIfExists() throws IOException {
        write(path1);
        assertTrue("'path1' should exist before deletion", storage.exists(path1));
        storage.deleteIfExists(path1);
        assertFalse("'path1' should not exist after deletion", storage.exists(path1));

        MCRTransactionManager.beginTransactions();
        write(path1);
        assertTrue("'path1' should exist before deletion", storage.exists(path1));
        storage.deleteIfExists(path1);
        assertFalse("'path1' should not exist after deletion", storage.exists(path1));
        MCRTransactionManager.commitTransactions();
    }

}
