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
 * This is a copy of the {@link io.ocfl.core.OcflRepositoryBuilder}.
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

    public MCROCFLRepositoryBuilder id(String id) {
        this.id = Enforce.notNull(id, "id cannot be null");
        return this;
    }

    public MCROCFLRepositoryBuilder remote(boolean remote) {
        this.remote = remote;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public MCROCFLRepositoryBuilder storage(OcflStorage storage) {
        return (MCROCFLRepositoryBuilder) super.storage(storage);
    }

    /**
     * {@inheritDoc}
     */
    public MCROCFLRepositoryBuilder storage(Consumer<OcflStorageBuilder> configureStorage) {
        return (MCROCFLRepositoryBuilder) super.storage(configureStorage);
    }

    /**
     * {@inheritDoc}
     */
    public MCROCFLRepositoryBuilder workDir(Path workDir) {
        return (MCROCFLRepositoryBuilder) super.workDir(workDir);
    }

    /**
     * {@inheritDoc}
     */
    public MCROCFLRepositoryBuilder objectLock(ObjectLock objectLock) {
        return (MCROCFLRepositoryBuilder) super.objectLock(objectLock);
    }

    /**
     * {@inheritDoc}
     */
    public MCROCFLRepositoryBuilder objectLock(Consumer<ObjectLockBuilder> configureLock) {
        return (MCROCFLRepositoryBuilder) super.objectLock(configureLock);
    }

    /**
     * {@inheritDoc}
     */
    public MCROCFLRepositoryBuilder inventoryCache(Cache<String, Inventory> inventoryCache) {
        return (MCROCFLRepositoryBuilder) super.inventoryCache(inventoryCache);
    }

    /**
     * {@inheritDoc}
     */
    public MCROCFLRepositoryBuilder objectDetailsDb(ObjectDetailsDatabase objectDetailsDb) {
        return (MCROCFLRepositoryBuilder) super.objectDetailsDb(objectDetailsDb);
    }

    /**
     * {@inheritDoc}
     */
    public MCROCFLRepositoryBuilder objectDetailsDb(Consumer<ObjectDetailsDatabaseBuilder> configureDb) {
        return (MCROCFLRepositoryBuilder) super.objectDetailsDb(configureDb);
    }

    /**
     * {@inheritDoc}
     */
    public MCROCFLRepositoryBuilder prettyPrintJson() {
        return (MCROCFLRepositoryBuilder) super.prettyPrintJson();
    }

    /**
     * {@inheritDoc}
     */
    public MCROCFLRepositoryBuilder inventoryMapper(InventoryMapper inventoryMapper) {
        return (MCROCFLRepositoryBuilder) super.inventoryMapper(inventoryMapper);
    }

    /**
     * {@inheritDoc}
     */
    public MCROCFLRepositoryBuilder logicalPathMapper(LogicalPathMapper logicalPathMapper) {
        return (MCROCFLRepositoryBuilder) super.logicalPathMapper(logicalPathMapper);
    }

    /**
     * {@inheritDoc}
     */
    public MCROCFLRepositoryBuilder contentPathConstraints(ContentPathConstraintProcessor contentPathConstraints) {
        return (MCROCFLRepositoryBuilder) super.contentPathConstraints(contentPathConstraints);
    }

    /**
     * {@inheritDoc}
     */
    public MCROCFLRepositoryBuilder ocflConfig(OcflConfig config) {
        return (MCROCFLRepositoryBuilder) super.ocflConfig(config);
    }

    /**
     * {@inheritDoc}
     */
    public MCROCFLRepositoryBuilder ocflConfig(Consumer<OcflConfig> configureConfig) {
        return (MCROCFLRepositoryBuilder) super.ocflConfig(configureConfig);
    }

    /**
     * {@inheritDoc}
     */
    public MCROCFLRepositoryBuilder defaultLayoutConfig(OcflExtensionConfig defaultLayoutConfig) {
        return (MCROCFLRepositoryBuilder) super.defaultLayoutConfig(defaultLayoutConfig);
    }

    /**
     * {@inheritDoc}
     */
    public MCROCFLRepositoryBuilder unsupportedExtensionBehavior(UnsupportedExtensionBehavior unsupportedBehavior) {
        return (MCROCFLRepositoryBuilder) super.unsupportedExtensionBehavior(unsupportedBehavior);
    }

    /**
     * {@inheritDoc}
     */
    public MCROCFLRepositoryBuilder ignoreUnsupportedExtensions(Set<String> ignoreUnsupportedExtensions) {
        return (MCROCFLRepositoryBuilder) super.ignoreUnsupportedExtensions(ignoreUnsupportedExtensions);
    }

    /**
     * {@inheritDoc}
     */
    public MCROCFLRepositoryBuilder verifyStaging(boolean verifyStaging) {
        return (MCROCFLRepositoryBuilder) super.verifyStaging(verifyStaging);
    }

    /**
     * {@inheritDoc}
     */
    public MCROCFLRepositoryBuilder fileLockTimeoutDuration(Duration fileLockTimeoutDuration) {
        return (MCROCFLRepositoryBuilder) super.fileLockTimeoutDuration(fileLockTimeoutDuration);
    }

    public MCROCFLRepository buildMCR() {
        OcflRepository baseRepository = super.build();
        return new MCROCFLRepository(id, baseRepository, remote, storage);
    }

}
