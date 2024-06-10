package org.mycore.ocfl.niofs.storage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Test;

public class MCROCFLRollingCacheStorageTest extends MCROCFLStorageTestCase {

    private MCROCFLRollingCacheStorage storage;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        FileCountEvictionStrategy evictionStrategy = new FileCountEvictionStrategy(2);
        storage = new MCROCFLRollingCacheStorage(rootPath, evictionStrategy);
    }

    @Override
    public MCROCFLRollingCacheStorage getStorage() {
        return storage;
    }

    @Test
    public void copyInputStream() throws IOException {
        storage.copy(new ByteArrayInputStream(new byte[] { 1 }), path1);
        storage.copy(new ByteArrayInputStream(new byte[] { 2 }), path2);
        storage.copy(new ByteArrayInputStream(new byte[] { 3 }), path3);
        assertFalse("'path1' should be rolled over", storage.exists(path1));
        assertTrue("'path2' should exist", storage.exists(path2));
        assertTrue("'path3' should exist", storage.exists(path3));
    }

    @Test
    public void copyWithSourceAndTarget() throws IOException {
        storage.copy(new ByteArrayInputStream(new byte[] { 1 }), path1);
        storage.copy(path1, path2);
        assertTrue("'path1' should exist", storage.exists(path1));
        assertTrue("'path2' should exist", storage.exists(path2));
        storage.copy(path1, path3);
        assertTrue("'path1' should exist", storage.exists(path1));
        assertFalse("'path2' should be rolled over", storage.exists(path2));
        assertTrue("'path3' should exist", storage.exists(path3));
    }

    @Test
    public void move() throws IOException {
        storage.copy(new ByteArrayInputStream(new byte[] { 1 }), path1);
        storage.move(path1, path2);
        assertFalse("'path1' should not exist after move", storage.exists(path1));
        assertTrue("'path2' should exist after move", storage.exists(path2));
    }

    @Test
    public void deleteIfExists() throws IOException {
        write(path1);
        assertTrue("'path1' should exist before deletion", storage.exists(path1));
        storage.deleteIfExists(path1);
        assertFalse("'path1' should not exist after deletion", storage.exists(path1));
    }

    @Test
    public void newByteChannelWriteOperation() throws IOException {
        write(path1);
        assertTrue("'path1' should exist after write operation", storage.exists(path1));
        write(path2);
        assertTrue("'path2' should exist after write operation", storage.exists(path2));
        write(path3);
        assertFalse("'path1' should be rolled over", storage.exists(path1));
        assertTrue("'path2' should exist", storage.exists(path2));
        assertTrue("'path3' should exist", storage.exists(path3));
    }

}
