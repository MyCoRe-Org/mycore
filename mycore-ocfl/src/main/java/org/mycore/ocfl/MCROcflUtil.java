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
import java.util.Map;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.config.MCRConfiguration2;

import edu.wisc.library.ocfl.api.OcflOption;
import edu.wisc.library.ocfl.api.OcflRepository;
import edu.wisc.library.ocfl.api.model.ObjectVersionId;

/**
 * Base Class for directly interfacing with the OCFL Repositories.
 * This Class allows changes to the Repositories without having to invoke the MetadataManager,
 * preventing it from making changes to the Database for operations like Repository Migration.
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCROcflUtil {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String REPO_INIT_ERR = "Repository must be initialized before use!";

    public MCRSimpleOcflRepositoryProvider mainClass;

    public final MCROCFLAdaptionRepositoryProvider adaptClass;

    private OcflRepository mainRepository;

    private OcflRepository adaptRepository;

    private Map<String, String> mainConfig;

    private Path mainRoot;

    private Path adaptRoot;

    private static Path exportDir = Path.of(MCRConfiguration2.getStringOrThrow("MCR.OCFL.Util.ExportDir"));

    private static Path backupDir = Path.of(MCRConfiguration2.getStringOrThrow("MCR.OCFL.Util.BackupDir"));

    private String repositoryKey = MCRConfiguration2.getStringOrThrow("MCR.Metadata.Manager.Repository");

    public MCROcflUtil() {
        mainClass = MCRConfiguration2.getInstanceOf("MCR.OCFL.Repository." + repositoryKey)
            .map(MCRSimpleOcflRepositoryProvider.class::cast).get();
        adaptClass = MCRConfiguration2.getInstanceOf("MCR.OCFL.Repository.Adapt")
            .map(MCROCFLAdaptionRepositoryProvider.class::cast).get();
        mainRepository = mainClass.getRepository();
        adaptRepository = adaptClass.getRepository();
        adaptRoot = adaptClass.getRepositoryRoot();
        mainConfig = MCRConfiguration2.getSubPropertiesMap("MCR.OCFL.Repository." + repositoryKey);
        mainRoot = Path.of(mainConfig.get(".RepositoryRoot"));
    }

    public static Path getExportDir() {
        return MCROcflUtil.exportDir;
    }

    public static Path getBackupDir() {
        return MCROcflUtil.backupDir;
    }

    /**
     * Moves a directory from origin to target
     * @param origin Origin Directory as Path
     * @param target Target Directory as Path
     * @throws IOException if an I/O error occurs
     */
    public static final void moveDir(Path origin, Path target) throws IOException {
        if (target.toFile().exists()) {
            Stream<Path> walker = Files.walk(target);
            walker
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
            walker.close();
        }
        Files.move(origin, target, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Deletes a Directory
     * @param dir Path for the Directory to delete
     * @throws IOException if an I/O error occurs
     */
    public static final void delDir(Path dir) throws IOException {
        if (dir.toFile().exists()) {
            Stream<Path> walker = Files.walk(dir);
            walker
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
            walker.close();
        } else {
            throw new IOException("Directory " + dir.getFileName() + " does not exist!");
        }
    }

    /**
     * This sets the active Repository in the OcflUtil, note that after this has been
     * changed the active repository setting has to be reloaded with {@link #updateMainRepo()}
     * @param repositoryKey New Repository Key to be set
     * @return MCROcflUtil
     */
    public MCROcflUtil setRepositoryKey(String repositoryKey) {
        this.repositoryKey = repositoryKey;
        return this;
    }

    /**
     * Update the Main Repository for the Ocfl Util
     * @return MCROcflUtil
     * @throws IOException if an I/O error occurs
     */
    public MCROcflUtil updateMainRepo() throws IOException {
        mainClass = MCRConfiguration2.getInstanceOf("MCR.OCFL.Repository." + repositoryKey)
            .map(MCRSimpleOcflRepositoryProvider.class::cast).get();
        mainRepository = mainClass.getRepository();
        mainConfig = MCRConfiguration2.getSubPropertiesMap("MCR.OCFL.Repository." + repositoryKey);
        mainRoot = Path.of(mainConfig.get(".RepositoryRoot"));
        LOGGER.debug(mainConfig);
        adaptClass.layout = mainConfig.get(".Layout");
        reloadRepository();
        return this;
    }

    /**
     * Set the per property configured active repository as active in the OcflUtil.
     * This method will update the active repository settings itself.
     * @return MCROcflUtil
     * @throws IOException if an I/O error occurs
     */
    public MCROcflUtil resetMainRepo() throws IOException {
        this.repositoryKey = MCRConfiguration2.getStringOrThrow("MCR.Metadata.Manager.Repository");
        updateMainRepo();
        return this;
    }

    public OcflRepository getMainRepository() {
        return this.mainRepository;
    }

    public OcflRepository getAdaptRepository() {
        return this.adaptRepository;
    }

    /**
     * Destroy the current Repository Instances and recreate them
     * @return MCROcflUtil
     * @throws IOException if an I/O error occurs
     */
    public MCROcflUtil reloadRepository() throws IOException {
        mainRepository.close();
        adaptRepository.close();
        mainClass.init("");
        adaptClass.init();
        // adaptClass.init("");
        mainRepository = mainClass.getRepository();
        adaptRepository = adaptClass.getRepository();
        LOGGER.info("Repositories Reloaded!");
        return this;
    }

    /**
     * This function exports the entire current repository into the ocfl-export directory
     * @exception MCRUsageException if Repository not Initiated
     * @return MCROcflUtil
     */
    public MCROcflUtil exportRepository() {
        OcflRepository repository = MCROCFLRepositoryProvider.getRepository(repositoryKey);
        if (repository == null) {
            throw new MCRUsageException(REPO_INIT_ERR);
        }
        repository.listObjectIds()
            .filter(id -> id.startsWith(MCROCFLXMLMetadataManager.MCR_OBJECT_ID_PREFIX))
            .forEach(objId -> {
                LOGGER.info("Exporting Object with ID {}", objId);
                repository.exportObject(objId, Paths.get(exportDir + "/" + objId.replace(":", "_")),
                    OcflOption.NO_VALIDATION,
                    OcflOption.OVERWRITE);
            });
        return this;
    }

    /**
     * Exports entire Object from Initiated Repository
     * @exception MCRUsageException if Repository not Initiated
     * @param mcrid Object MyCoRe ID
     * @return MCROcflUtil
     */
    public MCROcflUtil exportObject(String mcrid) {
        if (mainRepository == null || adaptRepository == null) {
            throw new MCRUsageException(REPO_INIT_ERR);
        }
        String prefixedMCRID = MCROCFLXMLMetadataManager.MCR_OBJECT_ID_PREFIX + mcrid;
        mainRepository.exportObject(prefixedMCRID, Paths.get(exportDir + "/" + prefixedMCRID.replace(":", "_")),
            OcflOption.NO_VALIDATION,
            OcflOption.OVERWRITE);
        return this;
    }

    /**
     * Exports Object version from Initiated Repository
     * @exception MCRUsageException if Repository not Initiated
     * @param mcrid Object MyCoRe ID
     * @param version Object Version
     * @return MCROcflUtil
     */
    public MCROcflUtil exportObjectVersion(String mcrid, String version) {
        if (mainRepository == null || adaptRepository == null) {
            throw new MCRUsageException(REPO_INIT_ERR);
        }
        String prefixedMcrid = MCROCFLXMLMetadataManager.MCR_OBJECT_ID_PREFIX + mcrid;
        mainRepository.exportVersion(ObjectVersionId.version(prefixedMcrid, version),
            Paths.get(exportDir + "/" + prefixedMcrid.replace(":", "_") + "_" + version), OcflOption.NO_VALIDATION,
            OcflOption.OVERWRITE);
        return this;
    }

    /**
     * Imports all Objects from specified export directory
     * @exception MCRUsageException if Repository not Initiated
     * @throws IOException if an I/O error occurs when opening the export directory
     * @return MCROcflUtil
     */
    public MCROcflUtil importRepository() throws IOException {
        if (mainRepository == null || adaptRepository == null) {
            throw new MCRUsageException(REPO_INIT_ERR);
        }
        Files.list(exportDir)
            .filter(Files::isDirectory)
            .forEach(dir -> {
                mainRepository.importObject(dir, OcflOption.MOVE_SOURCE, OcflOption.NO_VALIDATION);
            });
        return this;
    }

    /**
     * Imports all Objects from specified export directory into the Adapt Repository
     * @exception MCRUsageException if Repository not Initiated
     * @throws IOException if an I/O error occurs when opening the export directory
     * @return MCROcflUtil
     */
    public MCROcflUtil importAdapt() throws IOException {
        if (mainRepository == null || adaptRepository == null) {
            throw new MCRUsageException(REPO_INIT_ERR);
        }
        Files.list(exportDir)
            .filter(Files::isDirectory)
            .forEach(dir -> {
                adaptRepository.importObject(dir, OcflOption.MOVE_SOURCE, OcflOption.NO_VALIDATION);
            });
        return this;
    }

    /**
     * Move Root to Backup and Adapt to Root
     * @throws IOException if an I/O error occurs when opening or moving the backup, adapt or root directory
     */
    MCROcflUtil updateRoot() throws IOException {
        if (backupDir.toFile().exists()) {
            Stream<Path> walker = Files.walk(backupDir);
            walker
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
            walker.close();
        }
        Files.move(mainRoot, backupDir, StandardCopyOption.ATOMIC_MOVE);
        Files.move(adaptRoot, mainRoot, StandardCopyOption.ATOMIC_MOVE);
        return this;
    }

    /**
     * Restore the {@code ocfl-root} from {@code ocfl-backup}
     * @throws IOException if an I/O error occurs
     * @return MCROcflUtil
     */
    public final MCROcflUtil restoreRoot() throws IOException {
        if (!backupDir.toFile().exists()) {
            throw new MCRPersistenceException("There is no backup to restore!");
        }
        moveDir(backupDir, mainRoot);
        LOGGER.info("Restored OCFL-Root from backup.");
        return this;
    }

    /**
     * Deletes the {@code ocfl-backup} directory
     * @throws IOException if an I/O error occurs
     * @return MCROcflUtil
     */
    public final MCROcflUtil clearBackup() throws IOException {
        delDir(backupDir);
        return this;
    }
}
