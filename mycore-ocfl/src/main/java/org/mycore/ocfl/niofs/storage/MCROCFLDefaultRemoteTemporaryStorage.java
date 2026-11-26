/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.ocfl.niofs.storage;

import java.io.Closeable;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.digest.MCRDigest;
import org.mycore.common.digest.MCRDigestValidationException;
import org.mycore.common.digest.MCRMD5Digest;
import org.mycore.common.digest.MCRSHA512Digest;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.ocfl.niofs.MCROCFLDigestCalculator;

/**
 * Default implementation of a temporary storage for remote OCFL repositories.
 * <p>
 * This class acts as a local cache for files from a remote repository (like S3) to avoid repeated downloads.
 * It functions as a <b>content-addressable storage</b>, where files are identified by their cryptographic digest
 * ({@link MCRDigest}). This design provides automatic deduplication of content.
 * </p>
 * <h2>Key Features:</h2>
 * <ul>
 *   <li><b>LRU Eviction:</b> Uses a pluggable {@link MCROCFLEvictionStrategy} to manage its size, with a
 *   Least Recently Used (LRU) policy. The cache index is kept in access order, so a cache hit and the
 *   eviction of the least recently used entry are O(1).</li>
 *   <li><b>Journaling for Persistence:</b> All cache operations (add, remove) are logged to a journal file.
 *   This ensures that the cache state can be fully restored after an application restart, preventing the need to
 *   re-verify all cached files. The journal is compacted on startup if it contains much more events than cache
 *   entries. Temporary files of downloads that were interrupted, for example by a crash, are deleted on startup once
 *   they are older than {@link #STALE_TEMP_FILE_AGE}.</li>
 *   <li>
 *     <b>Shared Use by Several Processes:</b>
 *     Several processes may use the same directory, like the command line interface while the web application is
 *     running. This is best effort: each process has its own cache index and LRU order, files are only deleted on
 *     eviction or explicitly. A file evicted by another process is a cache miss and loaded again. All processes
 *     append to the same journal, it is only compacted on startup, see {@link #compactJournal()}. Clearing the cache
 *     should only be done while no other process uses the directory.
 *   </li>
 *   <li>
 *     <b>Concurrency:</b>
 *     Designed for safe use in a multithreaded environment. The cache index, the allocation and the journal
 *     appends are guarded by one lock. The short file operations that have to match the index run under the same
 *     lock: moving a new file to its final path, deleting an evicted file and checking whether a cached file still
 *     exists. Downloads, digest calculations and writes of temporary files run outside the lock. The lock is always
 *     taken before the lock of the journal.
 *   </li>
 *   <li>
 *     <b>Transactional Writes:</b>
 *     The {@link #newCacheEntry(String, FileAttribute...)} method provides a safe way to stream large
 *     files into the cache.</li>
 * </ul>
 *
 * @see MCROCFLRemoteTemporaryStorage
 * @see MCROCFLEvictionStrategy
 * @see CacheEntryWriter
 */
@MCRConfigurationProxy(proxyClass = MCROCFLDefaultRemoteTemporaryStorage.Factory.class)
public class MCROCFLDefaultRemoteTemporaryStorage implements MCROCFLRemoteTemporaryStorage, Closeable {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String JOURNAL_ADD = "ADD";

    private static final String JOURNAL_REMOVE = "REMOVE";

    private static final String TEMP_FILE_PREFIX = "tmp-";

    /**
     * Minimum age of a temporary file before it is deleted on startup. Younger files may be downloads of another
     * process using the same directory that are still in progress.
     */
    static final Duration STALE_TEMP_FILE_AGE = Duration.ofHours(24);

    /**
     * The journal is compacted on startup if it contains more than this factor times the number of cache entries.
     */
    private static final int COMPACT_ON_STARTUP_FACTOR = 2;

    public static final String JOURNAL_CACHE_FILE = "cache.journal";

    private final Path root;

    /**
     * Guarded by {@link #lock}.
     */
    private MCROCFLEvictionStrategy evictionStrategy;

    private final MCROCFLDigestCalculator<Path, MCRDigest> digestCalculator;

    /**
     * Guards {@link #cache}, {@link #totalAllocation}, {@link #evictionStrategy} and the journal. Always take it
     * before the journal lock.
     */
    private final Object lock = new Object();

    /**
     * The cache index in access order: the eldest entry is the least recently used one. Guarded by {@link #lock}.
     */
    private SequencedMap<MCRDigest, FileInfo> cache;

    /**
     * The sum of the sizes of all cached files. Guarded by {@link #lock}.
     */
    private long totalAllocation;

    private MCROCFLJournal<CacheEvent> journal;

    /**
     * Constructs a new {@code MCROCFLDefaultRemoteTemporaryStorage} with the specified root path and eviction strategy.
     *
     * @param root the root directory for the storage.
     * @param evictionStrategy the strategy to use for evicting items
     */
    public MCROCFLDefaultRemoteTemporaryStorage(Path root, MCROCFLEvictionStrategy evictionStrategy,
        MCROCFLDigestCalculator<Path, MCRDigest> digestCalculator) {
        this.root = root;
        this.evictionStrategy = evictionStrategy;
        this.digestCalculator = digestCalculator;
        init();
    }

    /**
     * Initializes the core data structures of the storage, creates the root directory if necessary,
     * and replays the journal to restore the cache state from the previous session.
     */
    public void init() {
        synchronized (lock) {
            this.cache = new LinkedHashMap<>(16, 0.75f, true);
            this.totalAllocation = 0;

            // create root directory
            try {
                Files.createDirectories(root);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to initialize cache journal", e);
            }

            // init journal
            try {
                long events = initJournal();
                compactOnStartup(events);
            } catch (IOException | MCROCFLJournal.JournalEntry.DeserializationException e) {
                LOGGER.error(() -> "Corrupt journal found. Clearing cache and starting fresh.", e);
                try {
                    clearAndReset();
                } catch (IOException ioException) {
                    ioException.addSuppressed(e);
                    throw new IllegalStateException("Failed to recover from corrupt journal", ioException);
                }
            }
        }

        deleteStaleTempFiles();

        // shutdown handler
        MCRShutdownHandler instance = MCRShutdownHandler.getInstance();
        if (instance != null) {
            instance.addCloseable(() -> {
                try {
                    this.close();
                } catch (IOException ioException) {
                    LOGGER.error("Failed to close cache journal", ioException);
                }
            });
        }
    }

    /**
     * Deletes temporary files of interrupted downloads, see {@link #newTempFile()}. Only files older than
     * {@link #STALE_TEMP_FILE_AGE} are deleted: younger ones may be downloads of another process in progress.
     * <p>
     * Cache files that are not in the cache index, because the process ended between writing the file and appending
     * to the journal, are kept. Their content matches their digest, and the next write of the same content takes
     * them over.
     */
    private void deleteStaleTempFiles() {
        Instant threshold = Instant.now().minus(STALE_TEMP_FILE_AGE);
        try (DirectoryStream<Path> tempFiles = Files.newDirectoryStream(root, TEMP_FILE_PREFIX + "*")) {
            for (Path tempFile : tempFiles) {
                try {
                    if (Files.isRegularFile(tempFile, LinkOption.NOFOLLOW_LINKS)
                        && Files.getLastModifiedTime(tempFile).toInstant().isBefore(threshold)) {
                        Files.deleteIfExists(tempFile);
                        LOGGER.info(() -> "Deleted stale temporary file " + tempFile);
                    }
                } catch (IOException e) {
                    LOGGER.warn(() -> "Failed to delete stale temporary file " + tempFile, e);
                }
            }
        } catch (IOException | DirectoryIteratorException e) {
            LOGGER.warn(() -> "Failed to delete stale temporary files in " + root, e);
        }
    }

    /**
     * Opens the journal and replays its events.
     *
     * @return the number of replayed events
     */
    private long initJournal() throws IOException {
        Path journalFile = root.resolve(JOURNAL_CACHE_FILE);
        this.journal = new MCROCFLJournal<>(journalFile, this::serializeCacheEvent, this::deserializeCacheEvent);
        AtomicLong events = new AtomicLong();
        this.journal.open(event -> {
            events.incrementAndGet();
            applyCacheEvent(event);
        });
        return events.get();
    }

    /**
     * Compacts the journal if it contains much more events than cache entries. Right after the replay, the cache
     * state equals the journal, so compacting loses nothing that is in the journal.
     */
    private void compactOnStartup(long events) {
        if (events <= (long) COMPACT_ON_STARTUP_FACTOR * count()) {
            return;
        }
        try {
            compactJournal();
        } catch (IOException e) {
            LOGGER.warn(() -> "Failed to compact the cache journal on startup.", e);
        }
    }

    private String serializeCacheEvent(CacheEvent event) {
        String digestStr = MCRDigestCodec.toString(event.digest());
        return switch (event) {
            case CacheEvent.Add add
                -> String.join("|", JOURNAL_ADD, digestStr, String.valueOf(add.size()), add.originalFileName());
            case CacheEvent.Remove ignored -> String.join("|", JOURNAL_REMOVE, digestStr);
        };
    }

    private CacheEvent deserializeCacheEvent(String line) throws MCROCFLJournal.JournalEntry.DeserializationException {
        try {
            String[] parts = line.split("\\|", -1);
            String command = parts[0];
            MCRDigest digest = MCRDigestCodec.fromString(parts[1]);
            return switch (command) {
                case JOURNAL_ADD -> new CacheEvent.Add(digest, Long.parseLong(parts[2]), parts[3]);
                case JOURNAL_REMOVE -> new CacheEvent.Remove(digest);
                default -> throw new IllegalArgumentException("Unknown journal command: " + command);
            };
        } catch (Exception e) {
            throw new MCROCFLJournal.JournalEntry.DeserializationException("Failed to deserialize line: " + line, e);
        }
    }

    /**
     * Applies a replayed journal event to the cache index. Called with {@link #lock} held.
     */
    private void applyCacheEvent(CacheEvent event) {
        switch (event) {
            case CacheEvent.Add add -> {
                // the journal can contain more than one add event for the same digest. In access order, put moves
                // an existing entry to the end, like a touch
                FileInfo previous = this.cache.put(add.digest(),
                    new FileInfo(add.digest(), add.size(), add.originalFileName()));
                this.totalAllocation += previous == null ? add.size() : add.size() - previous.size();
            }
            case CacheEvent.Remove remove -> {
                FileInfo info = this.cache.remove(remove.digest());
                if (info != null) {
                    this.totalAllocation -= info.size();
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int count() {
        synchronized (lock) {
            return this.cache.size();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long allocated() {
        synchronized (lock) {
            return this.totalAllocation;
        }
    }

    /**
     * Returns the root directory where temporary files are stored.
     *
     * @return The root path of the temporary storage.
     */
    public Path getRoot() {
        return root;
    }

    /**
     * Sets the eviction strategy for the cache. This can be used to change the policy at runtime.
     *
     * @param evictionStrategy The new eviction strategy to apply.
     */
    public void setEvictionStrategy(MCROCFLEvictionStrategy evictionStrategy) {
        synchronized (lock) {
            this.evictionStrategy = evictionStrategy;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(MCRDigest digest) {
        // only cached digests are touched: a miss is often not followed by a cache write (partial read, copy)
        return touch(digest) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() throws IOException {
        synchronized (lock) {
            this.journal.close();
            clearAndReset();
        }
    }

    /**
     * Resets the cache index, deletes the cache directory and opens a new journal. Called with {@link #lock} held.
     */
    private void clearAndReset() throws IOException {
        // reset cache
        this.cache.clear();
        this.totalAllocation = 0;
        // delete everything
        FileUtils.deleteDirectory(getRoot().toFile());
        // reset
        Files.createDirectories(root);
        initJournal();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note: This method loads the entire content into memory. For large files, it is recommended to
     * use the streaming API provided by {@link #newCacheEntry(String, FileAttribute...)}.
     */
    @Override
    public MCRDigest write(String originalFileName, byte[] bytes, OpenOption... options) throws IOException {
        MCRDigest digest = this.digestCalculator.calculate(bytes);

        // check if already in cache
        if (touch(digest) != null) {
            return digest;
        }

        // cache-miss: new content -> write to cache
        Path tempFile = newTempFile();
        try {
            Files.write(tempFile, bytes, options);
            addToCache(tempFile, digest, bytes.length, originalFileName);
        } finally {
            Files.deleteIfExists(tempFile);
        }
        return digest;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This operation is considered a "use" of the cached item, so it updates the item's position
     * in the LRU order, making it less likely to be evicted.
     */
    @Override
    public SeekableByteChannel readByteChannel(MCRDigest digest) throws IOException {
        Objects.requireNonNull(digest, "Digest should not be null.");
        synchronized (lock) {
            if (touch(digest) == null) {
                throw new NoSuchFileException(digest + " is not cached.");
            }
            // open under the lock: an eviction after opening does not affect the open channel
            return Files.newByteChannel(toCachePath(digest), StandardOpenOption.READ);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCRDigest importFile(Path source) throws IOException {
        MCRDigest digest = this.digestCalculator.calculate(source);

        // cache hit -> nothing to do
        if (touch(digest) != null) {
            return digest;
        }

        //  cache miss -> create
        Path tempFile = newTempFile();
        try {
            Files.copy(source, tempFile);
            addToCache(tempFile, digest, Files.size(tempFile), source.getFileName().toString());
        } finally {
            Files.deleteIfExists(tempFile);
        }
        return digest;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This operation also "touches" the cached item, updating its position in the LRU order.
     * <p>
     * The file is copied outside the lock, so an eviction during the copy can still make it fail.
     */
    @Override
    public void exportFile(MCRDigest sourceDigest, Path target, CopyOption... options) throws IOException {
        if (touch(sourceDigest) == null) {
            throw new NoSuchFileException(sourceDigest + " is not cached.");
        }
        Files.copy(toCachePath(sourceDigest), target, options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String probeContentType(MCRDigest digest) throws IOException {
        FileInfo fileInfo = touch(digest);
        if (fileInfo == null) {
            throw new NoSuchFileException(digest + " is not cached.");
        }
        // probe content type by original file name
        String probedType = URLConnection.guessContentTypeFromName(fileInfo.originalFileName());
        if (probedType != null) {
            return probedType;
        }
        // fallback: probe content type with physical path
        return Files.probeContentType(toCachePath(fileInfo.digest));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheEntryWriter newCacheEntry(String originalFileName, FileAttribute<?>... attrs) throws IOException {
        // write to a temporary file because we don't know the final destination yet (depends on the digest)
        Path tempFile = newTempFile();
        Set<StandardOpenOption> createOptions = Set.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        SeekableByteChannel channel = Files.newByteChannel(tempFile, createOptions, attrs);

        // return entry writer
        return new CacheEntryWriter(channel, tempFile, () -> {
            // calculate the digest
            MCRDigest digest = this.digestCalculator.calculate(tempFile);
            try {
                // move to the final path, unless the content is already cached
                addToCache(tempFile, digest, Files.size(tempFile), originalFileName);
            } finally {
                Files.deleteIfExists(tempFile);
            }
            return digest;
        }, () -> Files.deleteIfExists(tempFile));
    }

    /**
     * Looks up the cache entry of the given digest and marks it as most recently used.
     * <p>
     * If the entry exists but its file is missing on disk, for example because it was evicted by another process
     * using the same directory, the entry is removed and {@code null} is returned. The check and the removal run
     * under the {@link #lock}, so a concurrent write of the same content in this process is never removed.
     * <p>
     * A file on disk without a cache entry is left alone. It is either written by another process or left by a
     * crash. Both are complete, because files are only moved to their final path after they were written, and the
     * path is derived from the digest of the content. The next write of the same content replaces the file.
     *
     * @param digest the digest to look up
     * @return the cache entry, or {@code null} on a cache miss
     */
    private FileInfo touch(MCRDigest digest) {
        synchronized (lock) {
            // get() moves the entry to the end of the access order
            FileInfo fileInfo = this.cache.get(digest);
            if (fileInfo == null) {
                return null;
            }
            if (!Files.exists(toCachePath(digest))) {
                LOGGER.warn(() -> "Cached file for digest " + digest + " is missing on disk. Removing it from the "
                    + "cache.");
                cacheRemove(digest);
                return null;
            }
            return fileInfo;
        }
    }

    /**
     * Adds a completely written temporary file to the cache: moves it to its final path, adds it to the index and
     * the journal and evicts other entries if necessary. All of this runs under the {@link #lock}, so the index and
     * the files on disk are changed together. If the content was cached by another thread in the meantime, the
     * existing entry is only touched and the temporary file is left for the caller to delete.
     *
     * @param tempFile the temporary file with the content
     * @param digest the digest of the content
     * @param size the size of the content
     * @param originalFileName the original file name, used for content type detection
     */
    private void addToCache(Path tempFile, MCRDigest digest, long size, String originalFileName)
        throws IOException {
        synchronized (lock) {
            if (touch(digest) != null) {
                return;
            }
            moveToFinalPath(tempFile, toCachePath(digest));
            cacheUpdate(digest, size, originalFileName);
            rollOver();
        }
    }

    /**
     * Creates a temporary file in the cache root, on the same file store as the final cache paths. Files of
     * interrupted downloads are deleted on startup, see {@link #deleteStaleTempFiles()}.
     */
    private Path newTempFile() throws IOException {
        Files.createDirectories(root);
        return root.resolve(TEMP_FILE_PREFIX + UUID.randomUUID());
    }

    /**
     * Moves a completely written temporary file to its final path in one step. Other threads or processes never
     * see a partially written file. A file that already exists at the final path is replaced: the path is derived
     * from the digest of the content, so the existing file has the same content. It may exist on disk without
     * being in the cache index, for example if another process using the same directory wrote it.
     */
    private static void moveToFinalPath(Path tempFile, Path finalPath) throws IOException {
        Files.createDirectories(finalPath.getParent());
        try {
            Files.move(tempFile, finalPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException atomicMoveNotSupportedException) {
            Files.move(tempFile, finalPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Converts a digest into a physical path within the cache.
     * Uses the first three characters of the digest hex string as a subdirectory
     * to avoid having too many files in a single directory.
     *
     * @param digest The digest of the content.
     * @return The physical path for storing the content in the cache.
     */
    private Path toCachePath(MCRDigest digest) {
        String hex = digest.toHexString();
        String dir = hex.substring(0, 3);
        return this.root.resolve(dir).resolve(hex);
    }

    /**
     * Adds or replaces the cache entry, marks it as most recently used and logs the event to the journal. Called
     * with {@link #lock} held.
     *
     * @param digest The digest of the content.
     * @param size The size of the content.
     * @param originalFileName The original file name, used for content type detection.
     */
    private void cacheUpdate(MCRDigest digest, long size, String originalFileName) {
        FileInfo previous = this.cache.put(digest, new FileInfo(digest, size, originalFileName));
        this.totalAllocation += previous == null ? size : size - previous.size();
        try {
            journal.append(new CacheEvent.Add(digest, size, originalFileName));
        } catch (IOException ioException) {
            // the entry stays: it is valid and part of the allocation, so it is evicted like any other entry. Only
            // after a restart it is unknown, then the next write of the same content takes the file over
            LOGGER.error(() -> "Failed to append the cache entry for " + digest + " to the journal.", ioException);
        }
    }

    /**
     * Removes the cache entry of the given digest and logs the event to the journal. The file is not deleted.
     * Called with {@link #lock} held.
     *
     * @param digest The digest to remove from the cache.
     * @return the removed entry, or {@code null} if there was none
     */
    private FileInfo cacheRemove(MCRDigest digest) {
        FileInfo fileInfo = this.cache.remove(digest);
        if (fileInfo != null) {
            this.totalAllocation -= fileInfo.size();
            try {
                journal.append(new CacheEvent.Remove(digest));
            } catch (IOException e) {
                LOGGER.error(() -> "Failed to append the removal of " + digest + " to the journal.", e);
            }
        }
        return fileInfo;
    }

    /**
     * Performs cache rollover based on the eviction strategy. If the strategy indicates eviction is needed,
     * the least recently used items are removed from the cache and filesystem until the condition is met.
     * Called with {@link #lock} held.
     */
    private void rollOver() {
        while (!this.cache.isEmpty() && evictionStrategy.shouldEvict(this)) {
            // firstEntry() does not count as an access, so it does not change the order
            evict(this.cache.firstEntry().getKey());
        }
    }

    /**
     * Removes the cache entry of the given digest and deletes its file. Called with {@link #lock} held, so no
     * concurrent write of the same content can happen between the two.
     */
    private void evict(MCRDigest digest) {
        if (cacheRemove(digest) == null) {
            return;
        }
        Path path = toCachePath(digest);
        try {
            Files.deleteIfExists(path);
        } catch (IOException ioException) {
            LOGGER.error(() -> "Unable to remove " + path + " from remote cache.", ioException);
        }
    }

    /**
     * Compacts the journal file by writing the current state of the cache to a new journal
     * and atomically replacing the old one. This operation is thread-safe.
     * <p>
     * Compaction is useful to prevent the journal from growing indefinitely and to speed up
     * the cache recovery process on startup.
     * <p>
     * The snapshot is created from the cache index in access order to preserve the LRU order. The {@link #lock} is
     * taken before the journal lock, like for every append.
     * <p>
     * The snapshot only contains the cache entries of this process. Entries another process using the same directory
     * appended to the journal since this process replayed it are lost: after a restart, their files are not in the
     * cache index, so they are neither counted nor evicted until the same content is written again. Therefore,
     * compaction is only done automatically on startup, right after the journal was replayed. Other processes reopen
     * the replaced journal with their next append.
     *
     * @throws IOException if an I/O error occurs during compaction.
     */
    public void compactJournal() throws IOException {
        synchronized (lock) {
            journal.compact(() -> {
                List<CacheEvent> snapshot = new ArrayList<>(this.cache.size());
                for (FileInfo fileInfo : this.cache.values()) {
                    snapshot.add(new CacheEvent.Add(fileInfo.digest(), fileInfo.size(), fileInfo.originalFileName()));
                }
                return snapshot;
            });
        }
        LOGGER.info("Temporary storage cache compaction finished successfully.");
    }

    /**
     * Closes the journal writer, ensuring all buffered entries are flushed to disk.
     * This method is automatically registered with the {@link MCRShutdownHandler}.
     * <p>
     * The journal is not compacted here: another process using the same directory, like the web application while a
     * command line interface ends, may still append to it. See {@link #compactJournal()}.
     *
     * @throws IOException if an I/O error occurs while closing the journal.
     */
    @Override
    public void close() throws IOException {
        synchronized (lock) {
            if (this.journal != null) {
                this.journal.close();
            }
        }
    }

    /**
     * A factory for creating {@link MCROCFLDefaultRemoteTemporaryStorage} instances via the MyCoRe configuration
     * system. This allows for dependency injection of the storage path and eviction strategy.
     */
    public static class Factory implements Supplier<MCROCFLDefaultRemoteTemporaryStorage> {

        /**
         * The path to the root directory for this temporary storage.
         */
        @MCRProperty(name = "Path")
        public String path;

        @MCRInstance(name = "EvictionStrategy", valueClass = MCROCFLEvictionStrategy.class)
        public MCROCFLEvictionStrategy evictionStrategy;

        @Override
        public MCROCFLDefaultRemoteTemporaryStorage get() {
            Path root = Path.of(this.path);
            @SuppressWarnings("unchecked")
            MCROCFLDigestCalculator<Path, MCRDigest> digestCalculator =
                (MCROCFLDigestCalculator<Path, MCRDigest>) MCRConfiguration2
                    .getSingleInstanceOfOrThrow(MCROCFLDigestCalculator.class, "MCR.Content.DigestCalculator");
            return new MCROCFLDefaultRemoteTemporaryStorage(root, evictionStrategy, digestCalculator);
        }

    }

    /**
     * A record holding metadata about a single file in the cache, including its digest, it's size in bytes
     * and the original file name.
     *
     * @param digest           The cryptographic digest that uniquely identifies the content.
     * @param size             The size of the file in bytes.
     * @param originalFileName The original file name.
     */
    public record FileInfo(MCRDigest digest, Long size, String originalFileName) {
    }

    /**
     * A sealed interface representing the different types of events that can be written to the journal.
     * Each event must be associated with a specific digest.
     */
    private sealed interface CacheEvent {
        MCRDigest digest();

        /**
         * Represents a file being added to the cache. Contains all information needed to reconstruct the
         * {@link FileInfo} on replay.
         *
         * @param digest           The digest of the added file.
         * @param size             The size of the file in bytes.
         * @param originalFileName The original file name.
         */
        record Add(MCRDigest digest, long size, String originalFileName) implements CacheEvent {
        }

        /**
         * Represents a file being removed from the cache.
         *
         * @param digest The digest of the removed file.
         */
        record Remove(MCRDigest digest) implements CacheEvent {
        }

    }

    /**
     * A private helper class for serializing and deserializing {@link MCRDigest} instances to and from
     * a string representation for storage in the journal file.
     */
    private static final class MCRDigestCodec {

        /**
         * Serializes an MCRDigest to a "algorithm:hexValue" string.
         */
        public static String toString(MCRDigest digest) {
            if (digest == null) {
                return null;
            }
            return digest.getAlgorithm().normalize() + ":" + digest.toHexString();
        }

        /**
         * Deserializes a "algorithm:hexValue" string back to the correct MCRDigest subclass.
         */
        public static MCRDigest fromString(String digestString) {
            if (digestString == null || digestString.isEmpty()) {
                return null;
            }
            final String[] parts = digestString.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid MCRDigest string format: " + digestString);
            }
            final String algorithm = parts[0];
            final String value = parts[1];
            try {
                return switch (algorithm) {
                    case MCRMD5Digest.ALGORITHM_NAME_NORMALIZED -> new MCRMD5Digest(value);
                    case MCRSHA512Digest.ALGORITHM_NAME_NORMALIZED -> new MCRSHA512Digest(value);
                    default -> throw new IllegalArgumentException("Unsupported digest algorithm: " + algorithm);
                };
            } catch (MCRDigestValidationException e) {
                throw new IllegalArgumentException("Invalid digest value for algorithm " + algorithm, e);
            }
        }
    }

}
