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
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
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
 *   Least Recently Used (LRU) policy managed by an internal queue.</li>
 *   <li><b>Journaling for Persistence:</b> All cache operations (add, remove, touch) are logged to a journal file.
 *   This ensures that the cache state can be fully restored after an application restart, preventing the need to
 *   re-verify all cached files.</li>
 *   <li>
 *     <b>Concurrency:</b>
 *     Designed for safe use in a multithreaded environment, using concurrent data structures.
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

    private static final String JOURNAL_TOUCH = "TOUCH";

    public static final String JOURNAL_CACHE_FILE = "cache.journal";

    private final Path root;

    private MCROCFLEvictionStrategy evictionStrategy;

    private final MCROCFLDigestCalculator<Path, MCRDigest> digestCalculator;

    private Map<MCRDigest, FileInfo> cache;

    private Queue<MCRDigest> queue;

    private AtomicLong totalAllocation;

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
        this.cache = new ConcurrentHashMap<>();
        this.queue = new ConcurrentLinkedDeque<>();
        this.totalAllocation = new AtomicLong(0);

        // create root directory
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize cache journal", e);
        }

        // init journal
        try {
            initJournal();
        } catch (IOException | MCROCFLJournal.JournalEntry.DeserializationException e) {
            LOGGER.error(() -> "Corrupt journal found. Clearing cache and starting fresh.", e);
            try {
                clearAndReset();
            } catch (IOException ioException) {
                ioException.addSuppressed(e);
                throw new IllegalStateException("Failed to recover from corrupt journal", ioException);
            }
        }

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

    private void initJournal() throws IOException {
        Path journalFile = root.resolve(JOURNAL_CACHE_FILE);
        this.journal = new MCROCFLJournal<>(journalFile, this::serializeCacheEvent, this::deserializeCacheEvent);
        this.journal.open(this::applyCacheEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int count() {
        return this.cache.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long allocated() {
        return this.totalAllocation.get();
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
        this.evictionStrategy = evictionStrategy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(MCRDigest digest) {
        return cache.containsKey(digest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() throws IOException {
        // clear
        this.cache.clear();
        this.queue.clear();
        this.totalAllocation.set(0);
        this.journal.close();
        clearAndReset();
    }

    private void clearAndReset() throws IOException {
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
        if (this.cache.containsKey(digest)) {
            cacheTouch(digest);
            return digest;
        }

        // cache-miss: new content -> write to cache
        Path target = toCachePath(digest);
        Files.createDirectories(target.getParent());
        Files.write(target, bytes, options);

        // update cache
        cacheUpdate(digest, target, originalFileName);
        rollOver();
        return digest;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This operation is considered a "use" of the cached item, so it updates the item's position
     * in the LRU eviction queue, making it less likely to be evicted.
     */
    @Override
    public SeekableByteChannel readByteChannel(MCRDigest digest) throws IOException {
        Objects.requireNonNull(digest, "Digest should not be null.");
        FileInfo fileInfo = cache.get(digest);
        if (fileInfo == null) {
            throw new NoSuchFileException(digest + " is not cached.");
        }
        try {
            Path path = toCachePath(fileInfo.digest);
            return Files.newByteChannel(path, StandardOpenOption.READ);
        } finally {
            cacheTouch(digest);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCRDigest importFile(Path source) throws IOException {
        MCRDigest digest = this.digestCalculator.calculate(source);
        FileInfo fileInfo = this.cache.get(digest);

        // cache hit -> nothing to do
        if (fileInfo != null) {
            cacheTouch(digest);
            return digest;
        }

        //  cache miss -> create
        Path target = toCachePath(digest);
        Files.createDirectories(target.getParent());

        // actual copy
        Files.copy(source, target);

        // update cache
        cacheUpdate(digest, target, source.getFileName().toString());
        rollOver();
        return digest;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This operation also "touches" the cached item, updating its position in the LRU eviction queue.
     */
    @Override
    public void exportFile(MCRDigest sourceDigest, Path target, CopyOption... options) throws IOException {
        FileInfo fileInfo = this.cache.get(sourceDigest);
        if (fileInfo == null) {
            throw new NoSuchFileException(sourceDigest + " is not cached.");
        }
        Path path = toCachePath(fileInfo.digest);
        Files.copy(path, target, options);
        cacheTouch(sourceDigest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String probeContentType(MCRDigest digest) throws IOException {
        FileInfo fileInfo = this.cache.get(digest);
        if (fileInfo == null) {
            throw new NoSuchFileException(digest + " is not cached.");
        }
        try {
            // probe content type by original file name
            String probedType = URLConnection.guessContentTypeFromName(fileInfo.originalFileName());
            if (probedType != null) {
                return probedType;
            }
            // fallback: probe content type with physical path
            return Files.probeContentType(toCachePath(fileInfo.digest));
        } finally {
            cacheTouch(digest);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheEntryWriter newCacheEntry(String originalFileName, FileAttribute<?>... attrs) throws IOException {
        // write to a temporary file because we don't know the final destination yet (depends on the digest)
        Path tempFile = root.resolve("tmp-" + UUID.randomUUID());
        Files.createDirectories(tempFile.getParent());
        Set<StandardOpenOption> createOptions = Set.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        SeekableByteChannel channel = Files.newByteChannel(tempFile, createOptions, attrs);

        // return entry writer
        return new CacheEntryWriter(channel, tempFile, () -> {
            // calculate the digest
            MCRDigest digest = this.digestCalculator.calculate(tempFile);

            // check if already exist
            if (this.cache.containsKey(digest)) {
                Files.delete(tempFile);
                cacheTouch(digest);
                return digest;
            }

            // move to the final path
            Path finalPath = toCachePath(digest);
            Files.createDirectories(finalPath.getParent());
            Files.move(tempFile, finalPath);

            // update cache
            cacheUpdate(digest, finalPath, originalFileName);
            rollOver();
            return digest;
        }, () -> Files.deleteIfExists(tempFile));
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
     * Updates the cache with new file information and logs the event to the journal.
     *
     * @param digest The digest of the content.
     * @param path The physical path where the content is stored.
     * @param originalFileName The original file name, used for content type detection.
     * @throws IOException If an I/O error occurs.
     */
    private void cacheUpdate(MCRDigest digest, Path path, String originalFileName) throws IOException {
        try {
            this.queue.remove(digest);
            this.queue.offer(digest);
            FileInfo fileInfo = this.cache.get(digest);
            boolean newFile = fileInfo == null;
            long size = Files.size(path);
            this.totalAllocation.addAndGet(newFile ? size : size - fileInfo.size);
            this.cache.put(digest, new FileInfo(digest, size, originalFileName));
            journal.append(new CacheEvent.Add(digest, size, originalFileName));
        } catch (IOException ioException) {
            this.queue.remove(digest);
            throw ioException;
        }
    }

    /**
     * Removes the specified {@link MCRDigest} from the cache.
     *
     * @param digest The digest to remove from the cache.
     */
    private void cacheRemove(MCRDigest digest) {
        this.queue.remove(digest);
        FileInfo fileInfo = this.cache.remove(digest);
        if (fileInfo != null) {
            this.totalAllocation.addAndGet(-fileInfo.size);
            try {
                journal.append(new CacheEvent.Remove(digest));
            } catch (IOException e) {
                LOGGER.error(() -> "Failed to delete file or append to journal for " + digest, e);
            }
        }
    }

    /**
     * "Touches" a cache entry by moving it to the end of the LRU queue and logging the event.
     *
     * @param digest The digest of the entry to touch.
     * @throws IOException If a journal write fails.
     */
    private void cacheTouch(MCRDigest digest) throws IOException {
        this.queue.remove(digest);
        this.queue.offer(digest);
        journal.append(new CacheEvent.Touch(digest));
    }

    /**
     * Performs cache rollover based on the eviction strategy. If the strategy indicates eviction is needed,
     * the least recently used items are removed from the cache and filesystem until the condition is met.
     */
    private void rollOver() {
        while (!this.queue.isEmpty() && evictionStrategy.shouldEvict(this)) {
            MCRDigest digest = this.queue.poll();
            FileInfo fileInfo = this.cache.get(digest);
            // remove from cache
            cacheRemove(digest);
            // remove from filesystem
            Path path = toCachePath(fileInfo.digest);
            try {
                Files.deleteIfExists(path);
            } catch (IOException ioException) {
                LOGGER.error(() -> "Unable to remove " + path + " from remote cache.", ioException);
            }
        }
    }

    private String serializeCacheEvent(CacheEvent event) {
        String digestStr = MCRDigestCodec.toString(event.digest());
        return switch (event) {
            case CacheEvent.Add add
                -> String.join("|", JOURNAL_ADD, digestStr, String.valueOf(add.size()), add.originalFileName());
            case CacheEvent.Remove ignored -> String.join("|", JOURNAL_REMOVE, digestStr);
            case CacheEvent.Touch ignored -> String.join("|", JOURNAL_TOUCH, digestStr);
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
                case JOURNAL_TOUCH -> new CacheEvent.Touch(digest);
                default -> throw new IllegalArgumentException("Unknown journal command: " + command);
            };
        } catch (Exception e) {
            throw new MCROCFLJournal.JournalEntry.DeserializationException("Failed to deserialize line: " + line, e);
        }
    }

    private void applyCacheEvent(CacheEvent event) {
        switch (event) {
            case CacheEvent.Add add -> {
                this.queue.remove(add.digest());
                this.cache.put(add.digest(), new FileInfo(add.digest(), add.size(), add.originalFileName()));
                this.queue.offer(add.digest());
                this.totalAllocation.addAndGet(add.size());
            }
            case CacheEvent.Remove remove -> {
                this.queue.remove(remove.digest());
                FileInfo info = this.cache.remove(remove.digest());
                if (info != null) {
                    this.totalAllocation.addAndGet(-info.size());
                }
            }
            case CacheEvent.Touch touch -> {
                if (this.queue.remove(touch.digest())) {
                    this.queue.offer(touch.digest());
                }
            }
        }
    }

    /**
     * Compacts the journal file by writing the current state of the cache to a new journal
     * and atomically replacing the old one. This operation is thread-safe.
     * <p>
     * Compaction is useful to prevent the journal from growing indefinitely and to speed up
     * the cache recovery process on startup.
     *
     * @throws IOException if an I/O error occurs during compaction.
     */
    public synchronized void compactJournal() throws IOException {
        journal.compact(() -> {
            // The snapshot must be created from the queue to preserve the LRU order.
            // We create a new list to avoid concurrent modification issues while iterating.
            List<MCRDigest> orderedDigests = new ArrayList<>(this.queue);
            List<CacheEvent> snapshot = new ArrayList<>(orderedDigests.size());

            for (MCRDigest digest : orderedDigests) {
                FileInfo fileInfo = this.cache.get(digest);
                if (fileInfo != null) {
                    snapshot.add(
                        new CacheEvent.Add(digest, fileInfo.size(), fileInfo.originalFileName()));
                } else {
                    LOGGER.warn("Inconsistency detected during compaction: Digest {} is in the queue but not"
                        + " in the cache. Skipping.", digest);
                }
            }
            return snapshot;
        });
        LOGGER.info("Temporary storage cache compaction finished successfully.");
    }

    /**
     * Closes the journal writer, ensuring all buffered entries are flushed to disk.
     * This method is automatically registered with the {@link MCRShutdownHandler}.
     *
     * @throws IOException if an I/O error occurs while closing the journal.
     */
    @Override
    public void close() throws IOException {
        if (this.journal != null) {
            this.journal.close();
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
     * A record holding metadata about a single file in the cache, including its digest,
     * physical path on disk, and size in bytes.
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

        /**
         * Represents a file being accessed ("touched"), which updates its position in the LRU queue.
         *
         * @param digest The digest of the accessed file.
         */
        record Touch(MCRDigest digest) implements CacheEvent {
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
