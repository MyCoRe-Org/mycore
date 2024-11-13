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

package org.mycore.ocfl.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;

import io.ocfl.core.OcflRepositoryBuilder;
import io.ocfl.core.extension.OcflExtensionConfig;
import io.ocfl.core.storage.OcflStorageBuilder;

/**
 * Abstract base class for providing a local OCFL repository implementation.
 * <p>
 * This class initializes and manages a local {@link MCROCFLRepository}, configuring its root directory and working
 * directory, and setting up the underlying storage using OCFL's {@link OcflStorageBuilder}.
 * </p>
 * <p>
 * The {@code MCROCFLLocalRepositoryProvider} is typically instantiated based on the MyCoRe configuration property
 * {@code MCR.OCFL.Repository.%id%}, and it provides an initialized OCFL repository through the {@link #getRepository()}
 * method.
 * </p>
 *
 * <h2>Configuration Properties</h2>
 * <ul>
 *   <li>{@code RepositoryRoot}: Specifies the root directory for the repository's data storage.</li>
 *   <li>{@code WorkDir}: Specifies the working directory for temporary or intermediate storage.</li>
 * </ul>
 */
public abstract class MCROCFLLocalRepositoryProvider implements MCROCFLRepositoryProvider {

    protected Path repositoryRoot;

    protected Path workDir;

    protected MCROCFLRepository repository;

    /**
     * Initializes the OCFL repository by setting up the working and repository root directories
     * and configuring the repository builder.
     *
     * @param prop the repository configuration property key, used to determine the repository ID.
     * @throws IOException if an I/O error occurs while creating the necessary directories.
     */
    @MCRPostConstruction
    public void init(String prop) throws IOException {
        Files.createDirectories(workDir);
        Files.createDirectories(repositoryRoot);
        OcflRepositoryBuilder builder = new OcflRepositoryBuilder()
            .defaultLayoutConfig(getExtensionConfig())
            .storage(this::configureStorage)
            .workDir(workDir);
        String id = prop.substring(REPOSITORY_PROPERTY_PREFIX.length());
        this.repository = createRepository(id, builder);
    }

    /**
     * Creates and initializes an OCFL repository with the specified repository ID and builder configuration.
     *
     * @param id the unique identifier for the OCFL repository.
     * @param builder the {@link OcflRepositoryBuilder} to build and configure the repository.
     * @return a new {@link MCROCFLRepository} instance.
     */
    protected MCROCFLRepository createRepository(String id, OcflRepositoryBuilder builder) {
        return new MCROCFLRepository(id, builder.build(), false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCROCFLRepository getRepository() {
        return repository;
    }

    /**
     * Retrieves the repository root directory path.
     *
     * @return the root path of the repository.
     */
    public Path getRepositoryRoot() {
        return repositoryRoot;
    }

    /**
     * Retrieves the working directory path used for temporary storage.
     *
     * @return the working directory path.
     */
    public Path getWorkDir() {
        return workDir;
    }

    /**
     * Sets the root directory path for the OCFL repository.
     *
     * @param repositoryRoot the path to the repository's root directory.
     * @return this instance for chaining.
     */
    @MCRProperty(name = "RepositoryRoot")
    public MCROCFLLocalRepositoryProvider setRepositoryRoot(String repositoryRoot) {
        this.repositoryRoot = Paths.get(repositoryRoot);
        return this;
    }

    /**
     * Sets the working directory path for the OCFL repository.
     *
     * @param workDir the path to the repository's working directory.
     * @return this instance for chaining.
     */
    @MCRProperty(name = "WorkDir")
    public MCROCFLLocalRepositoryProvider setWorkDir(String workDir) {
        this.workDir = Paths.get(workDir);
        return this;
    }

    /**
     * Retrieves the OCFL extension configuration to specify the repository layout.
     *
     * @return the {@link OcflExtensionConfig} for the repository.
     */
    public abstract OcflExtensionConfig getExtensionConfig();

    /**
     * Configures the storage backend for the OCFL repository.
     * <p>
     * By default, it configures a file system storage backend using the {@code repositoryRoot} path.
     * </p>
     *
     * @param storageBuilder the storage builder used to configure storage settings.
     */
    public void configureStorage(OcflStorageBuilder storageBuilder) {
        storageBuilder.fileSystem(repositoryRoot);
    }

}
