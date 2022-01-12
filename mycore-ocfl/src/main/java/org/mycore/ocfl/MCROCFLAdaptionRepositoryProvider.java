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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.ocfl.layout.MCRLayoutConfig;
import org.mycore.ocfl.layout.MCRLayoutExtension;

import edu.wisc.library.ocfl.api.MutableOcflRepository;
import edu.wisc.library.ocfl.api.OcflRepository;
import edu.wisc.library.ocfl.core.OcflRepositoryBuilder;
import edu.wisc.library.ocfl.core.extension.OcflExtensionConfig;
import edu.wisc.library.ocfl.core.extension.OcflExtensionRegistry;
import edu.wisc.library.ocfl.core.extension.storage.layout.config.HashedNTupleIdEncapsulationLayoutConfig;

/**
 * The Adaptation Repository allows conversation between different OCFL Layout Types
 * @author Tobias Lenhardt [Hammer1279]
 * @version 1.0
 */
public class MCROCFLAdaptionRepositoryProvider extends MCRSimpleOcflRepositoryProvider {

    private static final Logger LOGGER = LogManager.getLogger();

    // if anything ever extends this class,
    // the configurations dont have to be loaded again and can be called from child classes
    protected final OcflExtensionConfig hashedNTupleConfig = new HashedNTupleIdEncapsulationLayoutConfig();

    protected final OcflExtensionConfig mcrLayoutConfig = new MCRLayoutConfig();

    private Path repositoryRoot;

    private Path mainRoot;

    private Path exportDir = MCROcflUtil.getExportDir();

    private Path workDir;

    /**
     * New repository with new layout, this is for rewriting only
     * and should not be used as base repository in production
     */
    private MutableOcflRepository repository;

    Thread cleanDir = new Thread(() -> {
        if (repositoryRoot.equals(mainRoot)) {
            return;
        }
        if (this.repositoryRoot.toFile().exists()) {
            LOGGER.info("Cleaning Directory on Exit...");
            if (this.repositoryRoot.getFileName().toString().equals("ocfl-root")) {
                return;
            }
            exportDir.toFile().delete();
            try (Stream<Path> walker = Files.walk(this.repositoryRoot)) {
                walker
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
                walker.close();
            } catch (IOException e) {
                throw new MCRException("Error while cleaning directory: ", e);
            }
        }
    });

    @Override
    public OcflRepository getRepository() {
        return this.repository;
    }

    @Override
    @MCRPostConstruction
    public void init(String property) throws IOException {
        this.init();
    }

    /**
     * Initiate Adaption Repository
     * @throws IOException if an I/O Error occurs
     * @return Adapt Repository
     */
    public MutableOcflRepository init() throws IOException {

        if (Files.notExists(workDir)) {
            Files.createDirectories(workDir).toFile().deleteOnExit();
        }
        if (Files.notExists(exportDir)) {
            Files.createDirectories(exportDir).toFile().deleteOnExit();
        }
        if (Files.notExists(repositoryRoot)) {
            Files.createDirectories(repositoryRoot);
        }

        try {
            Runtime.getRuntime().addShutdownHook(cleanDir);
        } catch (IllegalArgumentException err) {
            // expected error after repository restart, so this is dropped
        }

        OcflExtensionRegistry.register(MCRLayoutExtension.EXTENSION_NAME, MCRLayoutExtension.class);

        this.repository = new OcflRepositoryBuilder()
            .defaultLayoutConfig(getExtensionConfig())
            .storage(storage -> storage.fileSystem(repositoryRoot))
            .workDir(workDir)
            .buildMutable();

        return this.repository;
    }

    @Override
    @MCRProperty(name = "RepositoryRoot")
    public MCROCFLAdaptionRepositoryProvider setRepositoryRoot(String repositoryRoot) {
        this.repositoryRoot = Paths.get(repositoryRoot);
        return this;
    }

    @Override
    @MCRProperty(name = "WorkDir")
    public MCROCFLAdaptionRepositoryProvider setWorkDir(String workDir) {
        this.workDir = Paths.get(workDir);
        return this;
    }

    @MCRProperty(name = "Layout")
    public MCROCFLAdaptionRepositoryProvider setLayout(String layout) {
        this.layout = layout;
        return this;
    }

    @Override
    public Path getRepositoryRoot() {
        return this.repositoryRoot;
    }

    public Path getExportDir() {
        return this.exportDir;
    }

    public String getLayout() {
        return this.layout;
    }

}
