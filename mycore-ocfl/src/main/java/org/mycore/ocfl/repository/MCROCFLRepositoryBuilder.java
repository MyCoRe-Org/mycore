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

package org.mycore.ocfl.repository;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import io.ocfl.api.OcflConfig;
import io.ocfl.api.OcflRepository;
import io.ocfl.api.util.Enforce;
import io.ocfl.core.OcflRepositoryBuilder;
import io.ocfl.core.cache.Cache;
import io.ocfl.core.db.ObjectDetailsDatabase;
import io.ocfl.core.db.ObjectDetailsDatabaseBuilder;
import io.ocfl.core.extension.OcflExtensionConfig;
import io.ocfl.core.extension.UnsupportedExtensionBehavior;
import io.ocfl.core.inventory.InventoryMapper;
import io.ocfl.core.lock.ObjectLock;
import io.ocfl.core.lock.ObjectLockBuilder;
import io.ocfl.core.model.Inventory;
import io.ocfl.core.path.constraint.ContentPathConstraintProcessor;
import io.ocfl.core.path.mapper.LogicalPathMapper;
import io.ocfl.core.storage.OcflStorage;
import io.ocfl.core.storage.OcflStorageBuilder;

/**
 * MyCoRe-specific repository builder for constructing OCFL repositories.
 * <p>
 * This class extends the standard {@link io.ocfl.core.OcflRepositoryBuilder} and provides additional 
 * configuration and customization for the MyCoRe system. It allows you to specify repository properties such 
 * as the repository ID, remote mode, working directory, storage configuration, object locking, inventory 
 * caching, and various OCFL-specific options.
 * <p>
 * The builder methods are chainable and return an instance of {@code MCROCFLRepositoryBuilder} for 
 * convenient configuration. Once configured, calling {@link #buildMCR()} builds a new 
 * {@link MCROCFLRepository} instance, wrapping the underlying OCFL repository with MyCoRe-specific behavior.
 * <p>
 * <b>Example Usage:</b>
 * <pre>{@code
 * MCROCFLRepository repository = new MCROCFLRepositoryBuilder()
 *     .id("myRepositoryId")
 *     .remote(false)
 *     .workDir(Paths.get("/path/to/workDir"))
 *     .storage(storage -> {
 *         // Configure storage settings...
 *     })
 *     .objectLock(lock -> {
 *         // Configure object locking...
 *     })
 *     .defaultLayoutConfig(myExtensionConfig)
 *     .buildMCR();
 * }</pre>
 * </p>
 *
 * @see io.ocfl.core.OcflRepositoryBuilder
 * @see MCROCFLRepository
 */
public class MCROCFLRepositoryBuilder extends OcflRepositoryBuilder {

    private String id;

    private boolean remote;

    /**
     * Constructs a local file system based OCFL repository sensible defaults that can be overridden prior to calling
     * {@link #build()}.
     *
     * <p>Important: The same OcflRepositoryBuilder instance MUST NOT be used to initialize multiple repositories.
     */
    public MCROCFLRepositoryBuilder() {
        super();
        id = UUID.randomUUID().toString();
        remote = false;
    }

    /**
     * Sets the unique identifier for the OCFL repository.
     * <p>
     * This identifier uniquely distinguishes the repository within the MyCoRe system.
     * If not explicitly set, the builder will generate a random UUID by default.
     *
     * @param id the unique repository identifier; must not be {@code null}.
     * @return this builder instance for chaining.
     * @throws NullPointerException if {@code id} is {@code null}.
     */
    public MCROCFLRepositoryBuilder id(String id) {
        this.id = Enforce.notNull(id, "id cannot be null");
        return this;
    }

    /**
     * Configures the repository mode as remote or local.
     * <p>
     * When set to {@code true}, the repository is treated as remote, and the builder will apply additional
     * configuration appropriate for remote storage. By default, the repository is configured in local mode
     * ({@code false}).
     *
     * @param remote {@code true} to configure the repository as remote; {@code false} for local.
     * @return this builder instance for chaining.
     */
    public MCROCFLRepositoryBuilder remote(boolean remote) {
        this.remote = remote;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCROCFLRepositoryBuilder storage(OcflStorage storage) {
        return (MCROCFLRepositoryBuilder) super.storage(storage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCROCFLRepositoryBuilder storage(Consumer<OcflStorageBuilder> configureStorage) {
        return (MCROCFLRepositoryBuilder) super.storage(configureStorage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCROCFLRepositoryBuilder workDir(Path workDir) {
        return (MCROCFLRepositoryBuilder) super.workDir(workDir);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCROCFLRepositoryBuilder objectLock(ObjectLock objectLock) {
        return (MCROCFLRepositoryBuilder) super.objectLock(objectLock);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCROCFLRepositoryBuilder objectLock(Consumer<ObjectLockBuilder> configureLock) {
        return (MCROCFLRepositoryBuilder) super.objectLock(configureLock);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCROCFLRepositoryBuilder inventoryCache(Cache<String, Inventory> inventoryCache) {
        return (MCROCFLRepositoryBuilder) super.inventoryCache(inventoryCache);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCROCFLRepositoryBuilder objectDetailsDb(ObjectDetailsDatabase objectDetailsDb) {
        return (MCROCFLRepositoryBuilder) super.objectDetailsDb(objectDetailsDb);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCROCFLRepositoryBuilder objectDetailsDb(Consumer<ObjectDetailsDatabaseBuilder> configureDb) {
        return (MCROCFLRepositoryBuilder) super.objectDetailsDb(configureDb);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCROCFLRepositoryBuilder prettyPrintJson() {
        return (MCROCFLRepositoryBuilder) super.prettyPrintJson();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCROCFLRepositoryBuilder inventoryMapper(InventoryMapper inventoryMapper) {
        return (MCROCFLRepositoryBuilder) super.inventoryMapper(inventoryMapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCROCFLRepositoryBuilder logicalPathMapper(LogicalPathMapper logicalPathMapper) {
        return (MCROCFLRepositoryBuilder) super.logicalPathMapper(logicalPathMapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCROCFLRepositoryBuilder contentPathConstraints(ContentPathConstraintProcessor contentPathConstraints) {
        return (MCROCFLRepositoryBuilder) super.contentPathConstraints(contentPathConstraints);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCROCFLRepositoryBuilder ocflConfig(OcflConfig config) {
        return (MCROCFLRepositoryBuilder) super.ocflConfig(config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCROCFLRepositoryBuilder ocflConfig(Consumer<OcflConfig> configureConfig) {
        return (MCROCFLRepositoryBuilder) super.ocflConfig(configureConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCROCFLRepositoryBuilder defaultLayoutConfig(OcflExtensionConfig defaultLayoutConfig) {
        return (MCROCFLRepositoryBuilder) super.defaultLayoutConfig(defaultLayoutConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCROCFLRepositoryBuilder unsupportedExtensionBehavior(UnsupportedExtensionBehavior unsupportedBehavior) {
        return (MCROCFLRepositoryBuilder) super.unsupportedExtensionBehavior(unsupportedBehavior);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCROCFLRepositoryBuilder ignoreUnsupportedExtensions(Set<String> ignoreUnsupportedExtensions) {
        return (MCROCFLRepositoryBuilder) super.ignoreUnsupportedExtensions(ignoreUnsupportedExtensions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCROCFLRepositoryBuilder verifyStaging(boolean verifyStaging) {
        return (MCROCFLRepositoryBuilder) super.verifyStaging(verifyStaging);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCROCFLRepositoryBuilder fileLockTimeoutDuration(Duration fileLockTimeoutDuration) {
        return (MCROCFLRepositoryBuilder) super.fileLockTimeoutDuration(fileLockTimeoutDuration);
    }

    public MCROCFLRepository buildMCR() {
        OcflRepository baseRepository = super.build();
        return new MCROCFLRepository(id, baseRepository, remote, storage);
    }

}
