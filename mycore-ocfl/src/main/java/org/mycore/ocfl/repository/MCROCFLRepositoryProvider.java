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

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;

import io.ocfl.api.OcflRepository;
import io.ocfl.core.OcflRepositoryBuilder;
import io.ocfl.core.extension.OcflExtensionConfig;
import io.ocfl.core.storage.OcflStorageBuilder;

/**
 * Base Class to provide a {@link OcflRepository}. A {@link MCROCFLRepositoryProvider} will be loaded from the property
 * <code>MCR.OCFL.Repository.%id%</code> and the Method getRepository will be executed.
 */
public abstract class MCROCFLRepositoryProvider {

    public static final String REPOSITORY_PROPERTY_PREFIX = "MCR.OCFL.Repository.";

    protected Path repositoryRoot;

    protected Path workDir;

    protected MCROCFLRepository repository;

    @MCRPostConstruction
    public void init(String prop) throws IOException {
        Files.createDirectories(workDir);
        Files.createDirectories(repositoryRoot);
        OcflRepositoryBuilder builder = new OcflRepositoryBuilder()
            .defaultLayoutConfig(getExtensionConfig())
            .storage(this::getStorage)
            .workDir(workDir);
        String id = prop.substring(REPOSITORY_PROPERTY_PREFIX.length());
        this.repository = new MCROCFLRepository(id, builder.build());
    }

    public MCROCFLRepository getRepository() {
        return repository;
    }

    public Path getRepositoryRoot() {
        return repositoryRoot;
    }

    public Path getWorkDir() {
        return workDir;
    }

    @MCRProperty(name = "RepositoryRoot")
    public MCROCFLRepositoryProvider setRepositoryRoot(String repositoryRoot) {
        this.repositoryRoot = Paths.get(repositoryRoot);
        return this;
    }

    @MCRProperty(name = "WorkDir")
    public MCROCFLRepositoryProvider setWorkDir(String workDir) {
        this.workDir = Paths.get(workDir);
        return this;
    }

    public String getConfigurationPrefix() {
        return REPOSITORY_PROPERTY_PREFIX + repository.getId() + ".";
    }

    public static MCROCFLRepository getRepository(String id) {
        return getProvider(id).getRepository();
    }

    public static MCROCFLRepositoryProvider getProvider(String id) {
        return MCRConfiguration2.getSingleInstanceOfOrThrow(
            MCROCFLRepositoryProvider.class, REPOSITORY_PROPERTY_PREFIX + id);
    }

    public abstract OcflExtensionConfig getExtensionConfig();

    public abstract OcflStorageBuilder getStorage(OcflStorageBuilder storageBuilder);

}
