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
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.ocfl.layout.MCRLayoutConfig;
import org.mycore.ocfl.layout.MCRLayoutExtension;

import edu.wisc.library.ocfl.api.MutableOcflRepository;
import edu.wisc.library.ocfl.api.OcflOption;
import edu.wisc.library.ocfl.api.OcflRepository;
import edu.wisc.library.ocfl.api.model.ObjectVersionId;
import edu.wisc.library.ocfl.core.OcflRepositoryBuilder;
import edu.wisc.library.ocfl.core.extension.OcflExtensionConfig;
import edu.wisc.library.ocfl.core.extension.OcflExtensionRegistry;
import edu.wisc.library.ocfl.core.extension.storage.layout.config.HashedNTupleIdEncapsulationLayoutConfig;

/**
 * The Adaptation Repository allows conversation between different OCFL Layout Types
 * @author Tobias Lenhardt [Hammer1279]
 * @version 1.0
 */
public class MCROCFLAdaptionRepositoryProvider extends MCROCFLRepositoryProvider {

    private static final Logger LOGGER = LogManager.getLogger();

    public final MCROCFLXMLMetadataManager manager;

    // if anything ever extends this class,
    // the configurations dont have to be loaded again and can be called from child classes
    protected final OcflExtensionConfig hashedNTupleConfig = new HashedNTupleIdEncapsulationLayoutConfig();

    protected final OcflExtensionConfig mcrLayoutConfig = new MCRLayoutConfig();

    private Path repositoryRoot;

    private Path mainRoot;

    private Path exportDir;

    private Path backupDir;

    private Path workDir;

    private String layout;

    /**
     * New repository with new layout, this is for rewriting only
     * and should not be used as base repository in production
     */
    private MutableOcflRepository repository;

    /**
     * Currently loaded "old" repository
     */
    private OcflRepository prevRepository;

    Thread cleanDir = new Thread(() -> {
        if (repositoryRoot.equals(mainRoot)) {
            return;
        }
        if (this.repositoryRoot.toFile().exists()) {
            LOGGER.info("Cleaning Directory on Exit...");
            if (this.repositoryRoot.getFileName().equals("ocfl-root")) {
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

    public MCROCFLAdaptionRepositoryProvider(String repositoryKey) {
        manager = new MCROCFLXMLMetadataManager();
        manager.setRepositoryKey(repositoryKey);
        setRepositoryRoot(MCRConfiguration2.getStringOrThrow("MCR.OCFL.Repository.Adapt.RepositoryRoot"));
        this.mainRoot = Paths.get(MCRConfiguration2.getStringOrThrow("MCR.OCFL.Repository.Main.RepositoryRoot"));
        setExportDir(MCRConfiguration2.getStringOrThrow("MCR.OCFL.Repository.Adapt.ExportDir"));
        this.backupDir = Paths.get(MCRConfiguration2.getStringOrThrow("MCR.OCFL.Repository.Adapt.BackupDir"));
        setWorkDir(MCRConfiguration2.getStringOrThrow("MCR.OCFL.Repository.Adapt.WorkDir"));
        this.layout = MCRConfiguration2.getStringOrThrow("MCR.OCFL.Repository.Adapt.Layout");
    }

    @Override
    public OcflRepository getRepository() {
        return this.repository;
    }

    /**
     * Initiate Adaption Repository
     * close after use to prevent memory leaks
     * @throws IOException
     * @return Adapt Repository
     */
    public OcflRepository init() throws IOException {

        if (Files.notExists(workDir)) {
            Files.createDirectories(workDir).toFile().deleteOnExit();
        }
        if (Files.notExists(exportDir)) {
            Files.createDirectories(exportDir).toFile().deleteOnExit();
        }
        if (Files.notExists(repositoryRoot)) {
            Files.createDirectories(repositoryRoot);
        }

        Runtime.getRuntime().addShutdownHook(cleanDir);

        OcflExtensionRegistry.register(MCRLayoutExtension.EXTENSION_NAME, MCRLayoutExtension.class);

        this.repository = new OcflRepositoryBuilder()
            .defaultLayoutConfig(getExtensionConfig())
            .storage(storage -> storage.fileSystem(repositoryRoot))
            .workDir(workDir)
            .buildMutable();

        this.prevRepository = manager.getRepository();
        return this.repository;
    }

    /**
     * This function exports the entire current repository into the ocfl-export directory
     * @apiNote Make sure to Initiate the Repository before Use!
     * @exception MCRUsageException if Repository not Initiated
     */
    public void exportRepository() {
        if (prevRepository == null || repository == null) {
            throw new MCRUsageException("Repository must be initialized before use!");
        }
        prevRepository.listObjectIds()
            .filter(id -> id.startsWith(MCROCFLXMLMetadataManager.MCR_OBJECT_ID_PREFIX))
            .forEach(objId -> {
                LOGGER.info("Exporting Object with ID {}", objId);
                prevRepository.exportObject(objId, Paths.get(exportDir + "/" + objId), OcflOption.NO_VALIDATION,
                    OcflOption.OVERWRITE);
            });
    }

    /**
     * Exports entire Object from Initiated Repository
     * @apiNote Make sure to Initiate the Repository before Use!
     * @param mcrid
     */
    public void exportObject(String mcrid) {
        if (prevRepository == null || repository == null) {
            throw new MCRUsageException("Repository must be initialized before use!");
        }
        String prefixedMCRID = MCROCFLXMLMetadataManager.MCR_OBJECT_ID_PREFIX + mcrid;
        prevRepository.exportObject(prefixedMCRID, Paths.get(exportDir + "/" + prefixedMCRID), OcflOption.NO_VALIDATION,
            OcflOption.OVERWRITE);
    }

    /**
     * Exports Object version from Initiated Repository
     * @apiNote Make sure to Initiate the Repository before Use!
     * @param mcrid
     */
    public void exportObjectVersion(String mcrid, String version) {
        if (prevRepository == null || repository == null) {
            throw new MCRUsageException("Repository must be initialized before use!");
        }
        String prefixedMcrid = MCROCFLXMLMetadataManager.MCR_OBJECT_ID_PREFIX + mcrid;
        prevRepository.exportVersion(ObjectVersionId.version(prefixedMcrid, version),
            Paths.get(exportDir + "/" + prefixedMcrid + "_" + version), OcflOption.NO_VALIDATION, OcflOption.OVERWRITE);
    }

    /**
     * Imports all Objects from specified export directory
     * @apiNote Make sure to Initiate the Repository before Use!
     * @throws IOException
     */
    public void importRepository() throws IOException {
        if (prevRepository == null || repository == null) {
            throw new MCRUsageException("Repository must be initialized before use!");
        }
        Files.list(exportDir)
            .filter(Files::isDirectory)
            .forEach(dir -> {
                repository.importObject(dir, OcflOption.MOVE_SOURCE, OcflOption.NO_VALIDATION);
            });
    }

    /**
     * Move Root to Backup and Adapt to Root
     * @throws IOException
     */
    void updateRoot() throws IOException {
        if (backupDir.toFile().exists()) {
            Stream<Path> walker = Files.walk(backupDir);
            walker
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
            walker.close();
        }
        Files.move(mainRoot, backupDir, StandardCopyOption.ATOMIC_MOVE);
        if (Files.notExists(mainRoot)) {
            Files.createDirectories(mainRoot);
        }
        Files.move(repositoryRoot, mainRoot, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Destroy the current Repository Instances and recreate them
     * @throws IOException
     */
    public void reloadRepository() throws IOException {
        repository.close();
        prevRepository.close();
        new MCRSimpleOcflRepositoryProvider().init("prop");
        this.init();
        LOGGER.info("Repositories Reloaded!");
    }

    /**
     * Return the current Layout Config for this Repository
     * @return LayoutConfig
     */
    public OcflExtensionConfig getExtensionConfig() {
        switch (this.layout) {
            case "hash":
                return this.hashedNTupleConfig;
            case "mcr":
                return this.mcrLayoutConfig;
            default:
                throw new MCRConfigurationException("Wrong Config for MCR.Metadata.Manager.Repository.Layout");
        }
    }

    @MCRProperty(name = "RepositoryRoot")
    public void setRepositoryRoot(String repositoryRoot) {
        this.repositoryRoot = Paths.get(repositoryRoot);
    }

    @MCRProperty(name = "ExportDir")
    public void setExportDir(String exportDir) {
        this.exportDir = Paths.get(exportDir);
    }

    @MCRProperty(name = "WorkDir")
    public void setWorkDir(String workDir) {
        this.workDir = Paths.get(workDir);
    }

}
