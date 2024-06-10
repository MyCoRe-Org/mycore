package org.mycore.ocfl.niofs.storage;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Test;
import org.mycore.common.MCRTransactionHelper;
import org.mycore.ocfl.niofs.MCROCFLInactiveTransactionException;

public class MCROCFLHybridStorageTest extends MCROCFLStorageTestCase {

    private MCROCFLHybridStorage storage;

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
        MCRTransactionHelper.beginTransaction();
        assertTrue("'path1' should exist after transaction start", storage.exists(path1));
        MCRTransactionHelper.commitTransaction();
    }

    @Test
    public void toPhysicalPath() {
        assertTrue("should access rolling store", storage.toPhysicalPath(path1).endsWith("rolling/owner1/v0/file1"));
        MCRTransactionHelper.beginTransaction();
        assertTrue("should access transactional store",
            storage.toPhysicalPath(path1).endsWith("transaction/1/owner1/v0/file1"));
        MCRTransactionHelper.commitTransaction();
        assertTrue("should access rolling store", storage.toPhysicalPath(path1).endsWith("rolling/owner1/v0/file1"));
        MCRTransactionHelper.beginTransaction();
        assertTrue("should access transactional store",
            storage.toPhysicalPath(path1).endsWith("transaction/2/owner1/v0/file1"));
        MCRTransactionHelper.commitTransaction();
    }

    @Test
    public void copyInputStream() throws IOException, MCROCFLInactiveTransactionException {
        storage.copy(new ByteArrayInputStream(new byte[] { 1 }), path1);
        assertTrue("'path1' should exist", storage.exists(path1));

        MCRTransactionHelper.beginTransaction();
        assertTrue("'path1' should exist", storage.exists(path1));
        storage.copy(new ByteArrayInputStream(new byte[] { 2 }), path1);
        assertTrue("'path1' should exist", storage.exists(path1));
        assertArrayEquals("'path1' should have the content of the transactional store", new byte[] { 2 }, read(path1));
        MCRTransactionHelper.commitTransaction();

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

        MCRTransactionHelper.beginTransaction();
        assertArrayEquals("'path1' should have the content of the rollover store", path1Data, read(path1));
        assertArrayEquals("'path2' should have the content of the rollover store", path1Data, read(path2));
        storage.copy(path1, path3);
        assertArrayEquals("'path3' should have the content of the rollover store", path1Data, read(path3));
        write(path3, path3Data);
        storage.copy(path3, path2);
        assertArrayEquals("'path2' should have the content of the transactional store", path3Data, read(path2));
        MCRTransactionHelper.commitTransaction();
    }

    @Test
    public void move() throws IOException {
        storage.copy(new ByteArrayInputStream(new byte[] { 1 }), path1);
        storage.move(path1, path2);
        assertTrue("'path2' should exist after move", storage.exists(path2));

        MCRTransactionHelper.beginTransaction();
        storage.move(path2, path3);
        assertTrue("'path3' should exist after move", storage.exists(path3));
        MCRTransactionHelper.commitTransaction();
    }

    @Test
    public void deleteIfExists() throws IOException {
        write(path1);
        assertTrue("'path1' should exist before deletion", storage.exists(path1));
        storage.deleteIfExists(path1);
        assertFalse("'path1' should not exist after deletion", storage.exists(path1));

        MCRTransactionHelper.beginTransaction();
        write(path1);
        assertTrue("'path1' should exist before deletion", storage.exists(path1));
        storage.deleteIfExists(path1);
        assertFalse("'path1' should not exist after deletion", storage.exists(path1));
        MCRTransactionHelper.commitTransaction();
    }

}
