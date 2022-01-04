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

package org.mycore.ocfl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRPostConstruction;

import edu.wisc.library.ocfl.api.OcflRepository;
import edu.wisc.library.ocfl.core.OcflRepositoryBuilder;
import edu.wisc.library.ocfl.core.extension.OcflExtensionConfig;
import edu.wisc.library.ocfl.core.extension.OcflExtensionRegistry;
import edu.wisc.library.ocfl.core.extension.storage.layout.config.HashedNTupleIdEncapsulationLayoutConfig;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.ocfl.layout.MCRLayoutExtension;
import org.mycore.ocfl.layout.MCRLayoutConfig;

import javax.inject.Singleton;

/**
 * Simple way to provide a {@link OcflRepository}
 */
@Singleton
public class MCRSimpleOcflRepositoryProvider extends MCROCFLRepositoryProvider {

    private Path repositoryRoot;

    private Path workDir;

    private OcflRepository repository;

    @MCRProperty(name = "Layout")
    public String layout;

    @Override
    public OcflRepository getRepository() {
        return repository;
    }

    @MCRPostConstruction
    public void init(String prop) throws IOException {
        if (Files.notExists(workDir)) {
            Files.createDirectories(workDir).toFile().deleteOnExit();
        }
        if (Files.notExists(repositoryRoot)) {
            Files.createDirectories(repositoryRoot);
        }

        OcflExtensionRegistry.register(MCRLayoutExtension.EXTENSION_NAME, MCRLayoutExtension.class);

        this.repository = new OcflRepositoryBuilder()
            .defaultLayoutConfig(getExtensionConfig())
            .storage(storage -> storage.fileSystem(repositoryRoot))
            .workDir(workDir)
            .build();
    }

    public OcflExtensionConfig getExtensionConfig() {
        switch (this.layout) {
            case "hash":
                return new HashedNTupleIdEncapsulationLayoutConfig();
            case "mcr":
                return new MCRLayoutConfig();
            default:
                throw new MCRConfigurationException("Wrong Config for MCR.Metadata.Manager.Repository.Layout");
        }
    }

    public Path getRepositoryRoot() {
        return repositoryRoot;
    }

    public Path getWorkDir() {
        return workDir;
    }

    @MCRProperty(name = "RepositoryRoot")
    public MCRSimpleOcflRepositoryProvider setRepositoryRoot(String repositoryRoot) {
        this.repositoryRoot = Paths.get(repositoryRoot);
        return this;
    }

    @MCRProperty(name = "WorkDir")
    public MCRSimpleOcflRepositoryProvider setWorkDir(String workDir) {
        this.workDir = Paths.get(workDir);
        return this;
    }
}
