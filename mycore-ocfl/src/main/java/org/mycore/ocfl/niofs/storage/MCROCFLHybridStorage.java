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
import java.nio.channels.SeekableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.niofs.MCROCFLFileSystemTransaction;

/**
 * Hybrid storage implementation that combines transactional and rolling cache storage.
 * This class supports transactional operations while maintaining a rolling cache for non-transactional access.
 * Transactional storage always takes precedence over rolling storage when both are available.
 */
public class MCROCFLHybridStorage implements MCROCFLTransactionalTempFileStorage {

    public final static String TRANSACTION_DIRECTORY = "transaction";

    public final static String ROLLING_DIRECTORY = "rolling";

    private MCROCFLDefaultTransactionalTempFileStorage transactionalStorage;

    private MCROCFLRollingCacheStorage rollingStorage;

    private Path root;

    @MCRProperty(name = "Path")
    public String rootPathProperty;

    @MCRInstance(name = "EvictionStrategy", valueClass = MCROCFLEvictionStrategy.class)
    public MCROCFLEvictionStrategy evictionStrategy;

    /**
     * Default constructor for MCRConfiguration2 instantiation.
     */
    @SuppressWarnings("unused")
    public MCROCFLHybridStorage() {
    }

    /**
     * Constructs a new {@code MCROCFLHybridStorage} with the specified root path and eviction strategy.
     *
     * @param root the root directory for the storage.
     * @param evictionStrategy the strategy to use for evicting items from the rolling cache.
     */
    public MCROCFLHybridStorage(Path root, MCROCFLEvictionStrategy evictionStrategy) {
        this.root = root;
        this.evictionStrategy = evictionStrategy;
        initialize();
    }

    /**
     * Initializes the hybrid storage, setting up transactional and rolling storage paths.
     */
    @MCRPostConstruction
    public void postConstruct() {
        this.root = Path.of(rootPathProperty);
        initialize();
    }

    private void initialize() {
        Path transactionPath = root.resolve(TRANSACTION_DIRECTORY);
        Path rollingPath = root.resolve(ROLLING_DIRECTORY);
        this.transactionalStorage = new MCROCFLDefaultTransactionalTempFileStorage(transactionPath);
        this.rollingStorage = new MCROCFLRollingCacheStorage(rollingPath, evictionStrategy);
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
        return getStore(path).exists(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SeekableByteChannel newByteChannel(MCRVersionedPath path, Set<? extends OpenOption> options,
        FileAttribute<?>... fileAttributes) throws IOException {
        boolean existsInTransactionalStore = this.transactionalStorage.exists(path);
        // always serve from transactional store if it exists
        if (existsInTransactionalStore) {
            return this.transactionalStorage.newByteChannel(path, options, fileAttributes);
        }
        // it does not exist in transactional store, so we check for read access, if read then we can safely
        // return the rolling store
        boolean read = options.isEmpty() || options.contains(StandardOpenOption.READ);
        if (read) {
            return this.rollingStorage.newByteChannel(path, options, fileAttributes);
        }
        // we are writing to the file and it does not exists in transactional store
        boolean existsInRollingStore = this.rollingStorage.exists(path);
        boolean transactionActive = MCROCFLFileSystemTransaction.isActive();
        Set<OpenOption> transactionalOptions = new HashSet<>(options);
        if (existsInRollingStore && transactionActive) {
            Path transactionalStoragePhysicalPath = this.transactionalStorage.toPhysicalPath(path);
            Files.createDirectories(transactionalStoragePhysicalPath.getParent());
            boolean append = options.contains(StandardOpenOption.APPEND);
            boolean truncate = options.contains(StandardOpenOption.TRUNCATE_EXISTING);
            if (append) {
                // we are appending -> need to do a copy first
                Path rollingStoragePhysicalPath = this.rollingStorage.toPhysicalPath(path);
                Files.copy(rollingStoragePhysicalPath, transactionalStoragePhysicalPath);
            } else if (truncate) {
                // we are truncating, but the file does only exist in rolling store -> no truncate required
                transactionalOptions.remove(StandardOpenOption.TRUNCATE_EXISTING);
                transactionalOptions.add(StandardOpenOption.CREATE);
            }
        }
        return transactionActive ? this.transactionalStorage.newByteChannel(path, transactionalOptions, fileAttributes)
            : this.rollingStorage.newByteChannel(path, options, fileAttributes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(InputStream stream, MCRVersionedPath target, CopyOption... options) throws IOException {
        if (MCROCFLFileSystemTransaction.isActive()) {
            this.transactionalStorage.copy(stream, target, options);
        } else {
            this.rollingStorage.copy(stream, target, options);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(MCRVersionedPath source, MCRVersionedPath target, CopyOption... options) throws IOException {
        if (MCROCFLFileSystemTransaction.isActive()) {
            if (this.transactionalStorage.exists(source)) {
                this.transactionalStorage.copy(source, target, options);
            } else {
                Path physicalSource = this.rollingStorage.toPhysicalPath(source);
                try (InputStream inputStream = Files.newInputStream(physicalSource)) {
                    this.transactionalStorage.copy(inputStream, target, options);
                }
            }
            return;
        }
        this.rollingStorage.copy(source, target, options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(MCRVersionedPath source, MCRVersionedPath target, CopyOption... options) throws IOException {
        if (MCROCFLFileSystemTransaction.isActive()) {
            if (this.transactionalStorage.exists(source)) {
                this.transactionalStorage.move(source, target, options);
            } else {
                Path physicalSource = this.rollingStorage.toPhysicalPath(source);
                try (InputStream inputStream = Files.newInputStream(physicalSource)) {
                    this.transactionalStorage.copy(inputStream, target, options);
                }
            }
            return;
        }
        this.rollingStorage.move(source, target, options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteIfExists(MCRVersionedPath path) throws IOException {
        if (MCROCFLFileSystemTransaction.isActive()) {
            this.transactionalStorage.deleteIfExists(path);
        }
        this.rollingStorage.deleteIfExists(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createDirectories(MCRVersionedPath directoryPath, FileAttribute<?>... attrs) throws IOException {
        if (MCROCFLFileSystemTransaction.isActive()) {
            this.transactionalStorage.createDirectories(directoryPath, attrs);
        }
        this.rollingStorage.createDirectories(directoryPath, attrs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void purge(Long transactionId) throws IOException {
        this.transactionalStorage.purge(transactionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() throws IOException {
        this.transactionalStorage.clear();
        this.rollingStorage.clear();
        FileUtils.deleteDirectory(getRoot().toFile());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearTransactional() throws IOException {
        this.transactionalStorage.clearTransactional();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create(String owner, String version) throws IOException {
        if (MCROCFLFileSystemTransaction.isActive()) {
            this.transactionalStorage.create(owner, version);
        }
        this.rollingStorage.create(owner, version);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path toPhysicalPath(MCRVersionedPath path) {
        return getStore(path).toPhysicalPath(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path toPhysicalPath(String owner, String version) {
        if (MCROCFLFileSystemTransaction.isActive()) {
            return this.transactionalStorage.toPhysicalPath(owner, version);
        }
        return this.rollingStorage.toPhysicalPath(owner, version);
    }

    private MCROCFLTempFileStorage getStore(MCRVersionedPath path) {
        boolean activeTransaction = MCROCFLFileSystemTransaction.isActive();
        if (!activeTransaction) {
            return this.rollingStorage;
        }
        boolean doesExistInTransaction = this.transactionalStorage.exists(path);
        if (doesExistInTransaction) {
            return this.transactionalStorage;
        }
        return this.rollingStorage;
    }

}
