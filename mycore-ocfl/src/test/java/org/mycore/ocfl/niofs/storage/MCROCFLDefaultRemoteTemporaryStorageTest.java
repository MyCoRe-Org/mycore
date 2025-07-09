package org.mycore.ocfl.niofs.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTransactionManager;
import org.mycore.common.digest.MCRDigest;
import org.mycore.common.digest.MCRMD5Digest;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.niofs.MCROCFLFileSystemProvider;
import org.mycore.ocfl.test.MCROCFLSetupExtension;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.ocfl.test.PermutedParam;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith({ MCRPermutationExtension.class, MCROCFLSetupExtension.class })
public class MCROCFLDefaultRemoteTemporaryStorageTest extends MCROCFLStorageTestCase {

    private MCROCFLDefaultRemoteTemporaryStorage storage;

    private MCRDigest digest1;

    private MCRDigest digest2;

    private MCRDigest randomDigest;

    private final boolean remote = true;

    @PermutedParam
    private boolean purge;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        storage = (MCROCFLDefaultRemoteTemporaryStorage) MCROCFLFileSystemProvider.get().remoteStorage();
        storage.setEvictionStrategy(new FileCountEvictionStrategy(2));

        digest1 = storage.write(path1, new byte[] { 1 });
        digest2 = storage.write(path2, new byte[] { 2 });
        randomDigest = new MCRMD5Digest("698d7aec27728941d9dfae9bf5ab969c");
    }

    @TestTemplate
    public void exists() {
        assertTrue(storage.exists(digest1), "'digest1' should exist.");
        assertTrue(storage.exists(digest2), "'digest2' should exist.");
        assertFalse(storage.exists(randomDigest), "a random digest should not exist.");
    }

    @TestTemplate
    public void write() throws IOException {
        MCRDigest digest3 = storage.write(path3, new byte[] { 3 });

        // writing to path3 should rollover path1
        assertFalse(storage.exists(digest1), "'digest1' should not exist.");
        assertTrue(storage.exists(digest2), "'digest2' should exist.");
        assertTrue(storage.exists(digest3), "'digest3' should exist.");

        // writing to path1 should rollover path2
        digest1 = storage.write(path1, new byte[] { 4 });
        assertTrue(storage.exists(digest1), "'digest1' should exist.");
        assertFalse(storage.exists(digest2), "'digest2' should not exist.");
        assertTrue(storage.exists(digest3), "'digest3' should exist.");

        // check bytes
        assertArrayEquals(new byte[] { 4 }, read(storage, digest1));
        assertArrayEquals(new byte[] { 3 }, read(storage, digest3));

        // count files in the store
        try (Stream<Path> stream = Files.find(storage.getRoot(), Integer.MAX_VALUE,
            (ignore, attr) -> attr.isRegularFile())) {
            assertEquals(3, stream.count(), "there should be three files. 2 cached files and the journal.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @TestTemplate
    public void readByteChannel() throws IOException {
        assertThrows(NoSuchFileException.class, () -> read(storage, randomDigest));
        assertArrayEquals(new byte[] { 1 }, read(storage, digest1));
        assertArrayEquals(new byte[] { 2 }, read(storage, digest2));
    }

    @TestTemplate
    public void probeContentType() throws IOException {
        assertNull(storage.probeContentType(digest1));
        MCRDigest digest3 =
            storage.write(MCRVersionedPath.head(DERIVATE_3, "text.txt"), new byte[] { 3 });
        assertEquals("text/plain", storage.probeContentType(digest3));
    }

    @TestTemplate
    public void copy() throws IOException {
        // copy from path1 to path3
        MCRTransactionManager.beginTransactions();
        storage.copy(digest1, path3);
        assertTrue(Files.exists(path3), "'path3' should exist");
        assertArrayEquals(new byte[] { 1 }, Files.readAllBytes(path3), "'path3' bytes should be equal to path1");
        MCRTransactionManager.commitTransactions();

        // copy from path2 to path3 -> overwrite
        MCRTransactionManager.beginTransactions();
        assertThrows(FileAlreadyExistsException.class, () -> storage.copy(digest2, path3));
        storage.copy(digest2, path3, StandardCopyOption.REPLACE_EXISTING);
        assertTrue(Files.exists(path3), "'path3' should exist");
        assertArrayEquals(new byte[] { 2 }, Files.readAllBytes(path3), "'path3' bytes should be equal to path2");
        MCRTransactionManager.commitTransactions();
    }

    @TestTemplate
    public void newCacheEntry() throws IOException {
        // === Scenario 1: Successful Commit ===
        // This test verifies that content written via a CacheEntryWriter is correctly
        // added to the cache when commit() is called. It also checks that the
        // eviction policy is triggered as expected.

        byte[] commitContent = "This content should be committed.".getBytes();

        MCRDigest committedDigest;
        // Use the writer within a try-with-resources block and commit it
        MCROCFLDefaultRemoteTemporaryStorage.CacheEntryWriter writer = storage.newCacheEntry(path3);
        try {
            writer.getChannel().write(ByteBuffer.wrap(commitContent));
            committedDigest = writer.commit();
        } finally {
            writer.getChannel().close();
        }

        // Verification for commit:
        // 1. The cache had 2 items. We added a 3rd. The eviction strategy (max 2 files) should kick in.
        assertFalse(storage.exists(digest1), "The oldest entry (digest1) should have been evicted.");
        assertTrue(storage.exists(digest2), "The second entry (digest2) should still exist.");

        // 2. The newly committed content should now be in the cache.
        assertTrue(storage.exists(committedDigest), "The newly committed digest should exist in the cache.");

        // 3. The content read back from the cache should be identical to what was written.
        assertArrayEquals(commitContent, read(storage, committedDigest),
            "Content read from cache should match original content.");

        // === Scenario 2: Explicit Abort ===
        // This test ensures that if abort() is called, no changes are made to the
        // cache and the temporary file is cleaned up.

        byte[] abortContent = "This content should be aborted.".getBytes();
        MCRVersionedPath pathForAbort = MCRVersionedPath.head(DERIVATE_3, "abort.txt");
        Path tempFilePath;

        MCROCFLDefaultRemoteTemporaryStorage.CacheEntryWriter abortWriter = storage.newCacheEntry(pathForAbort);
        tempFilePath = abortWriter.getPath();

        try {
            assertTrue(Files.exists(tempFilePath), "Temporary file should exist before aborting.");
            abortWriter.getChannel().write(ByteBuffer.wrap(abortContent));
        } finally {
            abortWriter.abort();
            abortWriter.getChannel().close();
        }
        assertFalse(Files.exists(tempFilePath), "The temporary file should be deleted after abort.");
    }

    @TestTemplate
    public void testJournalRecovery() throws IOException {
        // 1. Perform more actions to create a more complex journal
        // Add a third file, which should trigger the eviction of digest1 (the oldest)
        byte[] content3 = new byte[] { 3 };
        MCRDigest digest3 = storage.write(path3, content3);

        // State check 1: digest1 evicted
        assertFalse(storage.exists(digest1), "Digest1 should be evicted by the write of digest3.");
        assertTrue(storage.exists(digest2), "Digest2 should still exist.");
        assertTrue(storage.exists(digest3), "Digest3 should now exist.");
        // Current state: cache=[digest2, digest3], queue=[digest2, digest3]

        // "Touch" digest2 by reading it, which moves it to the end of the LRU queue.
        // This will make digest3 the next candidate for eviction.
        byte[] content2 = read(storage, digest2);
        assertEquals((byte) 2, content2[0]);
        // Current state: cache=[digest2, digest3], queue=[digest3, digest2] (digest2 is newest)

        // Capture the path to the storage root before closing it.
        Path storageRoot = storage.getRoot();

        // 2. Simulate a shutdown by closing the storage instance.
        // This ensures the journal file is flushed and closed.
        storage.close();

        // 3. Simulate a restart by creating a new storage instance with the same configuration.
        // The constructor will trigger the init() method, which opens and replays the journal.
        MCROCFLDefaultRemoteTemporaryStorage newStorage = new MCROCFLDefaultRemoteTemporaryStorage(
            storageRoot,
            new FileCountEvictionStrategy(2),
            MCROCFLFileSystemProvider.get().getDigestCalculator());

        // 4. Verify existence of items first (this is a non-destructive check).
        // The state should be perfectly recovered from the journal at this point.
        assertFalse(newStorage.exists(digest1), "After recovery, digest1 should still not exist.");
        assertTrue(newStorage.exists(digest2), "After recovery, digest2 should exist.");
        assertTrue(newStorage.exists(digest3), "After recovery, digest3 should exist.");

        // This MUST be done before reading content, as reading alters the LRU queue.
        // Adding a new file should evict digest3, because digest2 was "touched" and is newer.
        byte[] content4 = new byte[] { 4 };
        MCRVersionedPath path4 = MCRVersionedPath.head(DERIVATE_3, "file4");
        MCRDigest digest4 = newStorage.write(path4, content4);

        // Assert that the correct item was evicted.
        assertTrue(newStorage.exists(digest4), "Digest4 should exist after write.");
        assertTrue(newStorage.exists(digest2),
            "Digest2 should remain because it was most recently used before shutdown.");
        assertFalse(newStorage.exists(digest3), "Digest3 should have been evicted as it was the least recently used.");

        // Check content
        assertArrayEquals(content2, read(newStorage, digest2), "Content of digest2 should be correct.");
        assertArrayEquals(content4, read(newStorage, digest4), "Content of digest4 should be correct.");

        // Clean up
        newStorage.close();
    }

    @TestTemplate
    public void testCompactJournal() throws IOException {
        // === Phase 1: Create a "messy" journal ===
        // The setUp method already added digest1 and digest2. The LRU queue is [digest1, digest2].

        // Add a third file, which will evict digest1 (the oldest).
        MCRDigest digest3 = storage.write(path3, new byte[] { 3 });
        // Journal now contains: ADD(1), ADD(2), ADD(3), REMOVE(1).
        // In-memory queue: [digest2, digest3]

        // "Touch" digest2 by reading it, moving it to the end of the LRU queue.
        read(storage, digest2);
        // Journal now contains: ..., TOUCH(2).
        // In-memory queue: [digest3, digest2]

        // Add a fourth file, which will evict digest3.
        MCRVersionedPath path4 = MCRVersionedPath.head(DERIVATE_3, "file4");
        MCRDigest digest4 = storage.write(path4, new byte[] { 4 });
        // Journal now contains: ..., ADD(4), REMOVE(3).
        // Final in-memory state: cache has {digest2, digest4}, queue is [digest2, digest4].

        // Get the state of the journal file before compaction.
        Path journalFile = storage.getRoot().resolve(MCROCFLDefaultRemoteTemporaryStorage.JOURNAL_CACHE_FILE);
        long journalSizeBefore = Files.size(journalFile);

        // === Phase 2: Perform compaction and verify ===
        storage.compactJournal();

        long journalSizeAfter = Files.size(journalFile);

        // The compacted journal should be smaller as it only contains two "ADD" events.
        assertTrue(journalSizeAfter < journalSizeBefore, "Journal file size should be smaller after compaction.");

        // Verify the in-memory state is still correct after compaction.
        assertFalse(storage.exists(digest1), "Digest1 should still be evicted.");
        assertTrue(storage.exists(digest2), "Digest2 should still exist.");
        assertFalse(storage.exists(digest3), "Digest3 should still be evicted.");
        assertTrue(storage.exists(digest4), "Digest4 should still exist.");
        assertEquals(2, storage.count(), "Cache should contain exactly 2 items.");

        // === Phase 3: Simulate restart and test recovery from the compacted journal ===
        // Close the current storage instance to ensure all file handles are released.
        storage.close();

        // Create a new storage instance pointing to the same directory.
        // This will force it to replay the *compacted* journal.
        MCROCFLDefaultRemoteTemporaryStorage newStorage = new MCROCFLDefaultRemoteTemporaryStorage(
            storage.getRoot(),
            new FileCountEvictionStrategy(2), // Same eviction strategy
            MCROCFLFileSystemProvider.get().getDigestCalculator());

        // Verify the state of the newly loaded cache.
        assertTrue(newStorage.exists(digest2), "After recovery, digest2 should exist.");
        assertTrue(newStorage.exists(digest4), "After recovery, digest4 should exist.");
        assertFalse(newStorage.exists(digest1), "After recovery, digest1 should not exist.");
        assertFalse(newStorage.exists(digest3), "After recovery, digest3 should not exist.");
        assertEquals(2, newStorage.count(), "Recovered cache should contain exactly 2 items.");

        // The recovered queue order should be [digest2, digest4].
        // Adding a new item should evict digest2.
        MCRVersionedPath path5 = MCRVersionedPath.head(DERIVATE_3, "file5");
        MCRDigest digest5 = newStorage.write(path5, new byte[] { 5 });

        assertFalse(newStorage.exists(digest2), "Digest2 should have been evicted, as it was the LRU item.");
        assertTrue(newStorage.exists(digest4), "Digest4 should remain.");
        assertTrue(newStorage.exists(digest5), "Digest5 should now be in the cache.");

        // Cleanup
        newStorage.close();
    }

    protected byte[] read(MCROCFLRemoteTemporaryStorage storage, MCRDigest digest) throws IOException {
        try (SeekableByteChannel channel = storage.readByteChannel(digest)) {
            ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
            channel.read(buffer);
            return buffer.array();
        }
    }

    /**
     * Eviction strategy based on the number of files.
     *
     * @param maxFiles number of files
     */
    protected record FileCountEvictionStrategy(int maxFiles) implements MCROCFLEvictionStrategy {

        @Override
        public boolean shouldEvict(MCROCFLRemoteTemporaryStorage storage) {
            return storage.count() > maxFiles;
        }

    }

}
