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
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

        digest1 = storage.write(path1.getFileName().toString(), new byte[] { 1 });
        digest2 = storage.write(path2.getFileName().toString(), new byte[] { 2 });
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
        MCRDigest digest3 = storage.write(path3.getFileName().toString(), new byte[] { 3 });

        // writing to path3 should roll over path1
        assertFalse(storage.exists(digest1), "'digest1' should not exist.");
        assertTrue(storage.exists(digest2), "'digest2' should exist.");
        assertTrue(storage.exists(digest3), "'digest3' should exist.");

        // writing to path1 should roll over path2
        digest1 = storage.write(path1.getFileName().toString(), new byte[] { 4 });
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
        MCRDigest digest3 = storage.write("text.txt", new byte[] { 3 });
        assertEquals("text/plain", storage.probeContentType(digest3));
    }

    @TestTemplate
    public void exportFile() throws IOException {
        // copy from path1 to path3
        MCRTransactionManager.beginTransactions();
        storage.exportFile(digest1, path3);
        assertTrue(Files.exists(path3), "'path3' should exist");
        assertArrayEquals(new byte[] { 1 }, Files.readAllBytes(path3), "'path3' bytes should be equal to path1");
        MCRTransactionManager.commitTransactions();

        // copy from path2 to path3 -> overwrite
        MCRTransactionManager.beginTransactions();
        assertThrows(FileAlreadyExistsException.class, () -> storage.exportFile(digest2, path3));
        storage.exportFile(digest2, path3, StandardCopyOption.REPLACE_EXISTING);
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
        MCROCFLDefaultRemoteTemporaryStorage.CacheEntryWriter writer =
            storage.newCacheEntry(path3.getFileName().toString());
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

        MCROCFLDefaultRemoteTemporaryStorage.CacheEntryWriter abortWriter =
            storage.newCacheEntry(pathForAbort.getFileName().toString());
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

    /**
     * A file on disk the cache does not know, for example written by another process, must not block the import of
     * the same content.
     */
    @TestTemplate
    public void importFileWithOrphanOnDisk() throws IOException {
        Path source = Files.createTempFile("orphan", ".txt");
        try {
            byte[] content = { 9, 9, 9 };
            Files.write(source, content);
            MCRDigest digest = writeOrphan(source, content);

            assertEquals(digest, storage.importFile(source));
            assertTrue(storage.exists(digest), "the imported content should be in the cache");
            assertArrayEquals(content, read(storage, digest));
        } finally {
            Files.deleteIfExists(source);
        }
    }

    /**
     * A file on disk the cache does not know must not block a new cache entry with the same content.
     */
    @TestTemplate
    public void newCacheEntryWithOrphanOnDisk() throws IOException {
        Path source = Files.createTempFile("orphan", ".txt");
        try {
            byte[] content = { 8, 8, 8 };
            Files.write(source, content);
            MCRDigest digest = writeOrphan(source, content);

            MCROCFLDefaultRemoteTemporaryStorage.CacheEntryWriter writer = storage.newCacheEntry("orphan.txt");
            try {
                writer.getChannel().write(ByteBuffer.wrap(content));
                assertEquals(digest, writer.commit());
            } finally {
                writer.getChannel().close();
            }
            assertTrue(storage.exists(digest), "the committed content should be in the cache");
            assertArrayEquals(content, read(storage, digest));
        } finally {
            Files.deleteIfExists(source);
        }
    }

    /**
     * Concurrent writes of the same content may all pass the cache check before one of them updates the cache. The
     * content has to be allocated only once.
     */
    @TestTemplate
    public void concurrentWritesOfSameContent() throws Exception {
        storage.setEvictionStrategy(new FileCountEvictionStrategy(Integer.MAX_VALUE));
        int threads = 8;
        for (int round = 0; round < 20; round++) {
            byte[] content = ("concurrent content " + round).getBytes();
            long allocatedBefore = storage.allocated();
            CyclicBarrier barrier = new CyclicBarrier(threads);
            List<Callable<MCRDigest>> tasks = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                tasks.add(() -> {
                    barrier.await();
                    return storage.write("concurrent.txt", content);
                });
            }
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            try {
                for (Future<MCRDigest> future : executor.invokeAll(tasks)) {
                    future.get();
                }
            } finally {
                executor.shutdownNow();
            }
            assertEquals(allocatedBefore + content.length, storage.allocated(),
                "the content should be allocated only once in round " + round);
        }
    }

    /**
     * Concurrent writes, reads and evictions must keep the cache index, the allocation, the files on disk and the
     * journal consistent: no index entry without a file, no file without an index entry and a journal that replays
     * to the same index.
     */
    @TestTemplate
    public void concurrentWritesReadsAndEvictions() throws Exception {
        storage.setEvictionStrategy(new FileCountEvictionStrategy(4));
        int threads = 8;
        List<Callable<Void>> tasks = writeAndReadTasks(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            for (Future<Void> future : executor.invokeAll(tasks)) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        List<Path> cacheFiles;
        try (Stream<Path> stream = Files.find(storage.getRoot(), Integer.MAX_VALUE,
            (path, attr) -> attr.isRegularFile() && !path.getParent().equals(storage.getRoot()))) {
            cacheFiles = stream.toList();
        }
        assertEquals(storage.count(), cacheFiles.size(), "every cache entry should have exactly one file on disk");
        long sizeOnDisk = 0;
        for (Path cacheFile : cacheFiles) {
            sizeOnDisk += Files.size(cacheFile);
        }
        assertEquals(sizeOnDisk, storage.allocated(), "the allocation should match the files on disk");

        // the order of the add and remove events in the journal must match the index: replaying the uncompacted
        // journal has to restore the same state
        try (MCROCFLDefaultRemoteTemporaryStorage replayed = newStorage(new FileCountEvictionStrategy(4))) {
            assertEquals(storage.count(), replayed.count(), "the replayed journal should match the cache index");
            assertEquals(storage.allocated(), replayed.allocated(),
                "the replayed journal should match the allocation");
        }
    }

    private List<Callable<Void>> writeAndReadTasks(int threads) {
        int contents = 12;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            int offset = t;
            tasks.add(() -> {
                barrier.await();
                for (int i = 0; i < 200; i++) {
                    byte[] content = ("content " + ((i + offset) % contents)).getBytes();
                    MCRDigest digest = storage.write("file.txt", content);
                    if (storage.exists(digest)) {
                        try {
                            assertArrayEquals(content, read(storage, digest));
                        } catch (NoSuchFileException e) {
                            // evicted by another thread between exists and read
                        }
                    }
                }
                return null;
            });
        }
        return tasks;
    }

    /**
     * A journal may contain more than one add event for the same digest. Replaying it must allocate the content only
     * once.
     */
    @TestTemplate
    public void replayDuplicateAddEvents() throws IOException {
        long allocated = storage.allocated();
        storage.close();

        // the journal contains one add event per cached digest -> duplicate each of them
        Path journalFile = storage.getRoot().resolve(MCROCFLDefaultRemoteTemporaryStorage.JOURNAL_CACHE_FILE);
        List<String> lines = Files.readAllLines(journalFile);
        Files.write(journalFile, lines, StandardOpenOption.APPEND);

        try (MCROCFLDefaultRemoteTemporaryStorage newStorage = new MCROCFLDefaultRemoteTemporaryStorage(
            storage.getRoot(),
            new FileCountEvictionStrategy(2),
            MCROCFLFileSystemProvider.get().getDigestCalculator())) {
            assertEquals(2, newStorage.count(), "the recovered cache should contain two entries");
            assertEquals(allocated, newStorage.allocated(), "duplicate add events should not be allocated twice");
        }
    }

    /**
     * Temporary files of interrupted downloads are deleted on startup once they are stale. Younger temporary files
     * may be downloads of another process in progress, cache files without a journal entry are complete and are
     * taken over by the next write of the same content: both are kept.
     */
    @TestTemplate
    public void initDeletesStaleTempFiles() throws IOException {
        Path source = Files.createTempFile("orphan", ".txt");
        try {
            Files.write(source, new byte[] { 7, 7, 7 });
            MCRDigest orphanDigest = writeOrphan(source, new byte[] { 7, 7, 7 });
            Path staleTempFile = storage.getRoot().resolve("tmp-interrupted-download");
            Files.write(staleTempFile, new byte[] { 6 });
            Files.setLastModifiedTime(staleTempFile, FileTime.from(Instant.now()
                .minus(MCROCFLDefaultRemoteTemporaryStorage.STALE_TEMP_FILE_AGE).minus(Duration.ofMinutes(1))));
            Path recentTempFile = storage.getRoot().resolve("tmp-download-in-progress");
            Files.write(recentTempFile, new byte[] { 5 });
            storage.close();

            try (MCROCFLDefaultRemoteTemporaryStorage newStorage = new MCROCFLDefaultRemoteTemporaryStorage(
                storage.getRoot(),
                new FileCountEvictionStrategy(10),
                MCROCFLFileSystemProvider.get().getDigestCalculator())) {
                assertFalse(Files.exists(staleTempFile), "the stale temporary file should be deleted");
                assertTrue(Files.exists(recentTempFile), "the recent temporary file should be kept");
                assertTrue(Files.exists(toCachePath(orphanDigest)), "the orphaned cache file should be kept");
                assertArrayEquals(new byte[] { 1 }, read(newStorage, digest1));
                assertArrayEquals(new byte[] { 2 }, read(newStorage, digest2));
            }
        } finally {
            Files.deleteIfExists(source);
        }
    }

    /**
     * Another process using the same directory may still append to the journal, closing must not replace it.
     */
    @TestTemplate
    public void closeDoesNotCompactJournal() throws IOException {
        storage.write(path3.getFileName().toString(), new byte[] { 3 });
        Path journalFile = journalFile();
        List<String> linesBefore = Files.readAllLines(journalFile);
        assertTrue(linesBefore.size() > storage.count(), "the journal should contain more events than entries");

        storage.close();

        assertEquals(linesBefore, Files.readAllLines(journalFile), "closing should not compact the journal");
    }

    /**
     * A journal with much more events than cache entries is compacted on startup.
     */
    @TestTemplate
    public void initCompactsJournal() throws IOException {
        MCRDigest digest3 = storage.write(path3.getFileName().toString(), new byte[] { 3 });
        MCRDigest digest4 = storage.write("file4", new byte[] { 4 });
        storage.close();
        assertTrue(Files.readAllLines(journalFile()).size() > 2 * storage.count(),
            "the journal should contain more than twice as many events as entries");

        try (MCROCFLDefaultRemoteTemporaryStorage newStorage = newStorage(new FileCountEvictionStrategy(2))) {
            assertEquals(2, Files.readAllLines(journalFile()).size(), "the journal should be compacted on startup");
            assertEquals(2, newStorage.count());
            assertTrue(newStorage.exists(digest3), "digest3 should exist after compaction");
            assertTrue(newStorage.exists(digest4), "digest4 should exist after compaction");
        }
    }

    /**
     * If another process replaces the journal, for example by compacting it on startup, the entries of this process
     * must be appended to the new journal instead of the replaced one.
     */
    @TestTemplate
    public void appendAfterJournalWasReplacedByAnotherProcess() throws IOException {
        storage.setEvictionStrategy(new FileCountEvictionStrategy(Integer.MAX_VALUE));
        MCROCFLDefaultRemoteTemporaryStorage otherProcess = newStorage(new FileCountEvictionStrategy(2));
        MCRDigest digest3;
        try {
            otherProcess.compactJournal();
            digest3 = storage.write(path3.getFileName().toString(), new byte[] { 3 });
        } finally {
            otherProcess.close();
        }
        storage.close();

        try (MCROCFLDefaultRemoteTemporaryStorage newStorage = newStorage(new FileCountEvictionStrategy(10))) {
            assertEquals(3, newStorage.count(), "the journal should contain all three entries");
            assertTrue(newStorage.exists(digest3), "digest3 should be appended to the new journal");
        }
    }

    private Path journalFile() {
        return storage.getRoot().resolve(MCROCFLDefaultRemoteTemporaryStorage.JOURNAL_CACHE_FILE);
    }

    private MCROCFLDefaultRemoteTemporaryStorage newStorage(MCROCFLEvictionStrategy evictionStrategy) {
        return new MCROCFLDefaultRemoteTemporaryStorage(storage.getRoot(), evictionStrategy,
            MCROCFLFileSystemProvider.get().getDigestCalculator());
    }

    private Path toCachePath(MCRDigest digest) {
        String hex = digest.toHexString();
        return storage.getRoot().resolve(hex.substring(0, 3)).resolve(hex);
    }

    private MCRDigest writeOrphan(Path source, byte[] content) throws IOException {
        MCRDigest digest = MCROCFLFileSystemProvider.get().getDigestCalculator().calculate(source);
        Path orphan = toCachePath(digest);
        Files.createDirectories(orphan.getParent());
        Files.write(orphan, content);
        return digest;
    }

    @TestTemplate
    public void testJournalRecovery() throws IOException {
        // 1. Perform more actions to create a more complex journal
        // Add a third file, which should trigger the eviction of digest1 (the oldest)
        byte[] content3 = new byte[] { 3 };
        MCRDigest digest3 = storage.write(path3.getFileName().toString(), content3);

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

        // 4. Verify the existence of items first (this is a non-destructive check).
        // The state should be perfectly recovered from the journal at this point.
        assertTrue(newStorage.exists(digest3), "After recovery, digest3 should exist.");
        assertTrue(newStorage.exists(digest2), "After recovery, digest2 should exist.");
        assertFalse(newStorage.exists(digest1), "After recovery, digest1 should still not exist.");

        // This MUST be done before reading content, as reading alters the LRU queue.
        // Adding a new file should evict digest3, because digest2 was "touched" and is newer.
        byte[] content4 = new byte[] { 4 };
        MCRVersionedPath path4 = MCRVersionedPath.head(DERIVATE_3, "file4");
        MCRDigest digest4 = newStorage.write(path4.getFileName().toString(), content4);

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
        MCRDigest digest3 = storage.write(path3.getFileName().toString(), new byte[] { 3 });
        // Journal now contains: ADD(1), ADD(2), ADD(3), REMOVE(1).
        // In-memory queue: [digest2, digest3]

        // "Touch" digest2 by reading it, moving it to the end of the LRU queue.
        read(storage, digest2);
        // Journal now contains: ..., TOUCH(2).
        // In-memory queue: [digest3, digest2]

        // Add a fourth file, which will evict digest3.
        MCRVersionedPath path4 = MCRVersionedPath.head(DERIVATE_3, "file4");
        MCRDigest digest4 = storage.write(path4.getFileName().toString(), new byte[] { 4 });
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
        MCRDigest digest5 = newStorage.write(path5.getFileName().toString(), new byte[] { 5 });

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
