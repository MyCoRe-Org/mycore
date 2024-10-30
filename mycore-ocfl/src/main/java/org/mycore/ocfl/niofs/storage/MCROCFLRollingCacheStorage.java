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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.niofs.channels.MCROCFLClosableCallbackChannel;

/**
 * Implementation of {@link MCROCFLTempFileStorage} that provides a rolling cache storage mechanism.
 * This class maintains a cache of files and employs an eviction strategy to manage the cache size.
 */
public class MCROCFLRollingCacheStorage implements MCROCFLTempFileStorage {

    private final Path root;

    private final Map<Path, Long> cache;

    private final Queue<Path> queue;

    private final MCROCFLEvictionStrategy evictionStrategy;

    private final AtomicLong totalAllocation;

    /**
     * Constructs a new {@code MCROCFLRollingCacheStorage} instance.
     *
     * @param root The root directory for the cache storage.
     * @param evictionStrategy The strategy to use for evicting items from the cache.
     */
    public MCROCFLRollingCacheStorage(Path root, MCROCFLEvictionStrategy evictionStrategy) {
        this.root = root;
        this.evictionStrategy = evictionStrategy;
        this.cache = Collections.synchronizedMap(new HashMap<>());
        this.queue = new ConcurrentLinkedDeque<>();
        this.totalAllocation = new AtomicLong(0);
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
            SeekableByteChannel channel = MCROCFLTempFileStorage.super.newByteChannel(path, options, fileAttributes);
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
        MCROCFLTempFileStorage.super.copy(is, target, options);
        cacheUpdate(toPhysicalPath(target), true);
        rollOver();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(MCRVersionedPath source, MCRVersionedPath target, CopyOption... options) throws IOException {
        MCROCFLTempFileStorage.super.copy(source, target, options);
        cacheUpdate(toPhysicalPath(source), false);
        cacheUpdate(toPhysicalPath(target), true);
        rollOver();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(MCRVersionedPath source, MCRVersionedPath target, CopyOption... options) throws IOException {
        MCROCFLTempFileStorage.super.move(source, target, options);
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
            MCROCFLTempFileStorage.super.deleteIfExists(path);
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
        MCROCFLTempFileStorage.super.clear();
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

}
