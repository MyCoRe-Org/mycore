/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.digest.MCRDigest;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.niofs.channels.MCROCFLClosableCallbackChannel;

/**
 * Implementation of {@link MCROCFLFileStorage} that provides a rolling cache storage mechanism.
 * This class maintains a cache of files and employs an eviction strategy to manage the cache size.
 */
public class MCROCFLRollingCacheStorage implements MCROCFLFileStorage {

    private static final Logger LOGGER = LogManager.getLogger();

    private Path root;

    private Map<Path, Long> cache;

    private Queue<Path> queue;

    private AtomicLong totalAllocation;

    @MCRProperty(name = "Path")
    public String rootPathProperty;

    @MCRInstance(name = "EvictionStrategy", valueClass = MCROCFLEvictionStrategy.class)
    public MCROCFLEvictionStrategy evictionStrategy;

    /**
     * Default constructor for MCRConfiguration2 instantiation.
     */
    @SuppressWarnings("unused")
    public MCROCFLRollingCacheStorage() {
    }

    /**
     * Constructs a new {@code MCROCFLRollingCacheStorage} with the specified root path and eviction strategy.
     *
     * @param root the root directory for the storage.
     * @param evictionStrategy the strategy to use for evicting items from the rolling cache.
     */
    public MCROCFLRollingCacheStorage(Path root, MCROCFLEvictionStrategy evictionStrategy) {
        this.root = root;
        this.evictionStrategy = evictionStrategy;
        init();
    }

    /**
     * Initializes the remote storage, setting up transactional and rolling storage paths.
     */
    @MCRPostConstruction
    public void postConstruct() {
        this.root = Path.of(rootPathProperty);
        init();
    }

    private void init() {
        this.cache = new ConcurrentHashMap<>();
        this.queue = new ConcurrentLinkedDeque<>();
        this.totalAllocation = new AtomicLong(0);
        try {
            initCache();
        } catch (IOException initException) {
            try {
                clear();
                LOGGER.error(() -> "Unable to initialize the cache storage. Cleared it.", initException);
            } catch (IOException clearException) {
                clearException.initCause(initException);
                LOGGER.error(
                    () -> "Unable to initialize the cache storage. Tried to clear but this also didn't work.",
                    clearException);
            }
        }
    }

    private void initCache() throws IOException {
        final List<Path> toRemoveList = new ArrayList<>();
        if (!Files.exists(root)) {
            return;
        }
        // update cache
        try (Stream<Path> pathStream = Files.walk(root)) {
            pathStream.filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        cacheUpdate(path, true);
                    } catch (IOException e) {
                        LOGGER.error(() -> "Unable to cache file " + path + ". Path will be removed from cache.", e);
                        toRemoveList.add(path);
                    }
                });
        }
        // remove
        for (Path toRemove : toRemoveList) {
            try {
                Files.delete(toRemove);
            } catch (IOException removeException) {
                LOGGER.error(() -> "Unable to remove path " + toRemove + " from rolling cache.", removeException);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getRoot() {
        return root;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(MCRVersionedPath path) {
        return cache.containsKey(toPhysicalPath(path));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SeekableByteChannel newByteChannel(MCRVersionedPath path, Set<? extends OpenOption> options,
        FileAttribute<?>... fileAttributes) throws IOException {
        final AtomicReference<Exception> exceptionReference = new AtomicReference<>(null);
        try {
            SeekableByteChannel channel = MCROCFLFileStorage.super.newByteChannel(path, options, fileAttributes);
            return new MCROCFLClosableCallbackChannel(channel, () -> {
                if (exceptionReference.get() != null) {
                    return;
                }
                try {
                    cacheUpdate(toPhysicalPath(path), options.contains(StandardOpenOption.WRITE));
                    rollOver();
                } catch (IOException e) {
                    throw new UncheckedIOException("Unable to update cache for " + path, e);
                }
            });
        } catch (Exception exception) {
            exceptionReference.set(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(InputStream is, MCRVersionedPath target, CopyOption... options) throws IOException {
        MCROCFLFileStorage.super.copy(is, target, options);
        cacheUpdate(toPhysicalPath(target), true);
        rollOver();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(MCRVersionedPath source, MCRVersionedPath target, CopyOption... options) throws IOException {
        MCROCFLFileStorage.super.copy(source, target, options);
        cacheUpdate(toPhysicalPath(source), false);
        cacheUpdate(toPhysicalPath(target), true);
        rollOver();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(MCRVersionedPath source, MCRVersionedPath target, CopyOption... options) throws IOException {
        MCROCFLFileStorage.super.move(source, target, options);
        cacheRemove(toPhysicalPath(source));
        cacheUpdate(toPhysicalPath(target), true);
        rollOver();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteIfExists(MCRVersionedPath path) throws IOException {
        try {
            MCROCFLFileStorage.super.deleteIfExists(path);
        } finally {
            cacheRemove(toPhysicalPath(path));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() throws IOException {
        this.cache.clear();
        this.queue.clear();
        this.totalAllocation.set(0);
        MCROCFLFileStorage.super.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path toPhysicalPath(String owner, String version) {
        return this.root.resolve(owner).resolve(version);
    }

    /**
     * Removes the specified path from the cache.
     *
     * @param physicalPath The path to remove from the cache.
     */
    private void cacheRemove(Path physicalPath) {
        this.queue.remove(physicalPath);
        Long removedBytes = this.cache.remove(physicalPath);
        if (removedBytes != null) {
            this.totalAllocation.addAndGet(-removedBytes);
        }
    }

    /**
     * Updates the cache with the specified path and recalculates the size if necessary.
     *
     * @param physicalPath The path to update in the cache.
     * @param updateSize Whether to update the size in the cache.
     * @throws IOException If an I/O error occurs.
     */
    private void cacheUpdate(Path physicalPath, boolean updateSize) throws IOException {
        try {
            this.queue.remove(physicalPath);
            this.queue.offer(physicalPath);
            boolean recalculate = !this.cache.containsKey(physicalPath) || updateSize;
            if (recalculate) {
                Long oldSize = this.cache.get(physicalPath);
                long newSize = Files.size(physicalPath);
                long deltaSize = newSize - (oldSize != null ? oldSize : 0);
                this.cache.put(physicalPath, newSize);
                this.totalAllocation.addAndGet(deltaSize);
            }
        } catch (IOException ioException) {
            this.queue.remove(physicalPath);
            throw ioException;
        }
    }

    /**
     * Performs cache rollover based on the eviction strategy.
     */
    private void rollOver() {
        while (!this.queue.isEmpty() && evictionStrategy.shouldEvict(this.queue.size(), this.totalAllocation.get())) {
            Path headPath = this.queue.poll();
            cacheRemove(headPath);
        }
    }

    private record FileInfo(Path physicalPath, Long size, MCRDigest digest) {
    }

}
