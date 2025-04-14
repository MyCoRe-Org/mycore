package org.mycore.ocfl.niofs.storage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.StandardOpenOption;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.test.PermutedParam;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith({ MCRPermutationExtension.class })
public class MCROCFLRollingCacheStorageTest extends MCROCFLStorageTestCase {

    private MCROCFLRollingCacheStorage storage;

    @PermutedParam
    private boolean remote;

    @PermutedParam
    private boolean purge;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        FileCountEvictionStrategy evictionStrategy = new FileCountEvictionStrategy(2);
        storage = new MCROCFLRollingCacheStorage(rootPath, evictionStrategy);
    }

    @Override
    public MCROCFLRollingCacheStorage getStorage() {
        return storage;
    }

    @TestTemplate
    public void copyInputStream() throws IOException {
        storage.copy(new ByteArrayInputStream(new byte[] { 1 }), path1);
        storage.copy(new ByteArrayInputStream(new byte[] { 2 }), path2);
        storage.copy(new ByteArrayInputStream(new byte[] { 3 }), path3);
        assertFalse(storage.exists(path1), "'path1' should be rolled over");
        assertTrue(storage.exists(path2), "'path2' should exist");
        assertTrue(storage.exists(path3), "'path3' should exist");
    }

    @TestTemplate
    public void copyWithSourceAndTarget() throws IOException {
        storage.copy(new ByteArrayInputStream(new byte[] { 1 }), path1);
        storage.copy(path1, path2);
        assertTrue(storage.exists(path1), "'path1' should exist");
        assertTrue(storage.exists(path2), "'path2' should exist");
        storage.copy(path1, path3);
        assertTrue(storage.exists(path1), "'path1' should exist");
        assertFalse(storage.exists(path2), "'path2' should be rolled over");
        assertTrue(storage.exists(path3), "'path3' should exist");
    }

    @TestTemplate
    public void move() throws IOException {
        storage.copy(new ByteArrayInputStream(new byte[] { 1 }), path1);
        storage.move(path1, path2);
        assertFalse(storage.exists(path1), "'path1' should not exist after move");
        assertTrue(storage.exists(path2), "'path2' should exist after move");
    }

    @TestTemplate
    public void deleteIfExists() throws IOException {
        write(path1);
        assertTrue(storage.exists(path1), "'path1' should exist before deletion");
        storage.deleteIfExists(path1);
        assertFalse(storage.exists(path1), "'path1' should not exist after deletion");
    }

    @TestTemplate
    public void newByteChannelWriteOperation() throws IOException {
        write(path1);
        assertTrue(storage.exists(path1), "'path1' should exist after write operation");
        write(path2);
        assertTrue(storage.exists(path2), "'path2' should exist after write operation");
        write(path3);
        assertFalse(storage.exists(path1), "'path1' should be rolled over");
        assertTrue(storage.exists(path2), "'path2' should exist");
        assertTrue(storage.exists(path3), "'path3' should exist");
    }

    @TestTemplate
    public void newByteChannelFailureTest() {
        MCRVersionedPath nonExistingPath = MCRVersionedPath.head("owner1", "file4");
        try (SeekableByteChannel ignored = storage.newByteChannel(nonExistingPath, Set.of(StandardOpenOption.READ))) {
            fail("Expected an IOException to be thrown due to non-existent file.");
        } catch (IOException e) {
            assertFalse(storage.exists(nonExistingPath),
                "Cache should not contain the file after failed open");
        }
    }

}
