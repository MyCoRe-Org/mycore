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

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
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
        if (MCROCFLFileSystemTransaction.get().isActive()) {
            return this.transactionalStorage.exists(path) || this.rollingStorage.exists(path);
        }
        return this.rollingStorage.exists(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SeekableByteChannel newByteChannel(MCRVersionedPath path, Set<? extends OpenOption> options,
        FileAttribute<?>... fileAttributes) throws IOException {
        boolean transactionNotActive = !MCROCFLFileSystemTransaction.get().isActive();
        boolean read = options.isEmpty() || options.contains(StandardOpenOption.READ);
        boolean doesNotExistInTransactionalStore = !this.transactionalStorage.exists(path);
        if (transactionNotActive || (doesNotExistInTransactionalStore && read)) {
            return this.rollingStorage.newByteChannel(path, options, fileAttributes);
        }
        return this.transactionalStorage.newByteChannel(path, options, fileAttributes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(InputStream stream, MCRVersionedPath target, CopyOption... options) throws IOException {
        if (MCROCFLFileSystemTransaction.get().isActive()) {
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
        if (MCROCFLFileSystemTransaction.get().isActive()) {
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
        if (MCROCFLFileSystemTransaction.get().isActive()) {
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
        if (MCROCFLFileSystemTransaction.get().isActive()) {
            this.transactionalStorage.deleteIfExists(path);
        }
        this.rollingStorage.deleteIfExists(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createDirectories(MCRVersionedPath directoryPath, FileAttribute<?>... attrs) throws IOException {
        if (MCROCFLFileSystemTransaction.get().isActive()) {
            this.transactionalStorage.createDirectories(directoryPath, attrs);
        }
        this.rollingStorage.createDirectories(directoryPath, attrs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void purge(MCROCFLFileSystemTransaction transaction) throws IOException {
        this.transactionalStorage.purge(transaction);
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
        if (MCROCFLFileSystemTransaction.get().isActive()) {
            this.transactionalStorage.create(owner, version);
        }
        this.rollingStorage.create(owner, version);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path toPhysicalPath(String owner, String version) {
        return MCROCFLFileSystemTransaction.get().isActive() ? this.transactionalStorage.toPhysicalPath(owner, version)
            : this.rollingStorage.toPhysicalPath(owner, version);
    }

}
