package org.mycore.ocfl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.stream.Stream;

import javax.persistence.PersistenceException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;

import edu.wisc.library.ocfl.api.OcflRepository;

/**
 * <strong><u>The Base Class</u></strong> for migrating and
 * converting OCFL Objects into others and some other functions
 * that are documented per method
 * @author Tobias Lenhardt [Hammer1279]
 * @author Sebastian Hofmann
 */
public class MCROCFLBaseClass {

    private final static Logger LOGGER = LogManager.getLogger();

    private int i;

    public final MCROCFLAdaptionRepositoryProvider adaptionRepo;

    private static final String SUCCESS = "success";

    private static final String SUCCESS_BUT_WITHOUT_HISTORY = SUCCESS + " but without history";

    private static final String FAILED = "failed";

    private static final String FAILED_AND_NOW_INVALID_STATE = FAILED + " and now invalid state";

    private static final String BACKUP_CONFIG = "MCR.OCFL.Repository.Adapt.BackupDir";

    /**
     * Moves a directory from origin to target
     * @param origin Origin Directory as Path
     * @param target Target Directory as Path
     * @throws IOException
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
     * @throws IOException
     * @throws PersistenceException
     */
    public static final void delDir(Path dir) throws IOException, PersistenceException {
        if (dir.toFile().exists()) {
            Stream<Path> walker = Files.walk(dir);
            walker
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
            walker.close();
        } else {
            throw new PersistenceException("Directory " + dir.getFileName() + " does not exist!");
        }
    }

    public MCROCFLBaseClass(String repoKey) {
        this.adaptionRepo = new MCROCFLAdaptionRepositoryProvider(repoKey);
    }

    /**
     * Restore the {@code ocfl-root} from {@code ocfl-backup}
     * @param repository RepositoryKey for config
     * @throws IOException
     */
    public static final void restoreRoot(String repository) throws IOException {
        Path rootDir = Paths
            .get(MCRConfiguration2.getStringOrThrow("MCR.OCFL.Repository." + repository + ".RepositoryRoot"));
        Path backupDir = Paths.get(MCRConfiguration2.getStringOrThrow(BACKUP_CONFIG));
        if (!backupDir.toFile().exists()) {
            throw new MCRPersistenceException("There is no backup to restore!");
        }
        moveDir(backupDir, rootDir);
        LOGGER.info("Restored OCFL-Root from backup.");
    }

    /**
     * Deletes the {@code ocfl-backup} directory
     * @param repository <i>unused</i>
     * @throws IOException
     */
    public static final void clearBackup(String repository) throws IOException {
        Path backupDir = Paths.get(MCRConfiguration2.getStringOrThrow(BACKUP_CONFIG));
        delDir(backupDir);
    }

    /**
     * Convert from XML to OCFL using {@code MCROCFLMigration}
     * @param repository RepositoryKey
     * @throws IOException
     */
    public static void convertXMLToOcfl(String repository) throws IOException {

        Path rootDir = Paths
            .get(MCRConfiguration2.getStringOrThrow("MCR.OCFL.Repository." + repository + ".RepositoryRoot"));
        Path backupDir = Paths.get(MCRConfiguration2.getStringOrThrow(BACKUP_CONFIG));
        if (rootDir.toFile().exists()) {
            moveDir(rootDir, backupDir);
        }

        MCROFCLMigration migration = new MCROFCLMigration(repository);

        migration.start();

        ArrayList<String> success = migration.getSuccess();
        ArrayList<String> failed = migration.getFailed();
        ArrayList<String> invalidState = migration.getInvalidState();
        ArrayList<String> withoutHistory = migration.getWithoutHistory();

        LOGGER.info("The migration resulted in: \n" +
            SUCCESS + ": {}, \n" +
            FAILED + ": {} \n" +
            FAILED_AND_NOW_INVALID_STATE + ": {} \n" +
            SUCCESS_BUT_WITHOUT_HISTORY + ": {} \n ",
            String.join(", ", success),
            String.join(", ", failed),
            String.join(", ", invalidState),
            String.join(", ", withoutHistory));

        LOGGER.info("The migration resulted in: \n" +
            SUCCESS + ": {}, \n" +
            FAILED + ": {} \n" +
            FAILED_AND_NOW_INVALID_STATE + ": {} \n" +
            SUCCESS_BUT_WITHOUT_HISTORY + ": {} \n ",
            success.size(),
            failed.size(),
            invalidState.size(),
            withoutHistory.size());
    }

    /**
     * Convert between OCFL Layouts or create backups of Repos
     * @apiNote due to how its implemented, if a Layout is not changed it just makes a backup
     * @throws IOException
     */
    public void convertOcflToOcfl() throws IOException {
        if (null == adaptionRepo) {
            throw new MCRUsageException("Adapt Repository has not been initialized yet!");
        }
        this.adaptionRepo.init();
        this.adaptionRepo.exportRepository();
        this.adaptionRepo.importRepository();
        this.adaptionRepo.updateRoot();
        this.adaptionRepo.reloadRepository();
        this.adaptionRepo.getRepository().close();
        LOGGER.info("Migration finished, please restart MyCoRe with the new Layout");
    }

    /**
     * Copies all objects from the OCFL Repository into the Native Metadata Manager
     * @param repositoryKey Name of the Repository
     * @throws IOException
     * @throws MCRException Metadata Manager <strong>MUST NOT</strong> be {@code MCROCFLXMLMetadataManager}
     */
    public void convertOcflToXML(String repositoryKey) throws IOException, MCRException {
        if (null == adaptionRepo) {
            throw new MCRUsageException("Adapt Repository has not been initialized yet!");
        }
        String managerConfig = MCRConfiguration2.getString("MCR.Metadata.Manager").orElse("xml");
        MCRXMLMetadataManager manager = MCRXMLMetadataManager.instance();
        MCROCFLXMLMetadataManager ocflManager = adaptionRepo.manager;
        if ("org.mycore.ocfl.MCROCFLXMLMetadataManager".equals(managerConfig)) {
            throw new MCRException(
                "Cannot convert from OCFL to XML with the OCFL Metadata Manager!\n" +
                    "Change the Metadata setting to use the Native XML Metadata Manager: " +
                    "'MCR.Metadata.Manager' set to 'MCROCFLXMLMetadataManager'");
        }
        adaptionRepo.setRepositoryRoot(
            MCRConfiguration2.getStringOrThrow("MCR.OCFL.Repository." + repositoryKey + ".RepositoryRoot"));
        adaptionRepo.init();
        ArrayList<MCRObjectID> newObjects = new ArrayList<MCRObjectID>();
        ArrayList<MCRObjectID> existObjects = new ArrayList<MCRObjectID>();
        i = 0;
        OcflRepository repo = adaptionRepo.getRepository();
        repo.listObjectIds()
            .filter(id -> id.startsWith(MCROCFLXMLMetadataManager.MCR_OBJECT_ID_PREFIX))
            .map(id -> id.substring(MCROCFLXMLMetadataManager.MCR_OBJECT_ID_PREFIX.length()))
            .forEach(objId -> {
                MCRObjectID mcrid = MCRObjectID.getInstance(objId);
                // MCRContent xml;
                // Date lastModified;
                // try {
                //     xml = ocflManager.retrieveContent(mcrid);
                //     lastModified = new Date(ocflManager.getLastModified(mcrid));
                // } catch (IOException e) {
                //     throw new MCRPersistenceException("Error while accessing MCRObject: ", e);
                // }
                // manager.update(mcrid, xml, lastModified);
                // i++;
                if (manager.exists(mcrid)) {
                    existObjects.add(mcrid);
                } else {
                    newObjects.add(mcrid);
                }
            });
        newObjects.forEach(mcrid -> {
            MCRContent xml;
            Date lastModified;
            try {
                xml = ocflManager.retrieveContent(mcrid);
                lastModified = new Date(ocflManager.getLastModified(mcrid));
                manager.create(mcrid, xml, lastModified);
                i++;
            } catch (MCRUsageException e) {
                LOGGER.debug("MCRObject {} was deleted, skipping!", mcrid);
            } catch (IOException e) {
                throw new MCRPersistenceException("Error while accessing MCRObject: ", e);
            }
        });
        existObjects.forEach(mcrid -> {
            MCRContent xml;
            Date lastModified;
            try {
                xml = ocflManager.retrieveContent(mcrid);
                lastModified = new Date(ocflManager.getLastModified(mcrid));
                manager.update(mcrid, xml, lastModified);
                i++;
            } catch (MCRUsageException e) {
                LOGGER.debug("MCRObject {} was deleted, skipping!", mcrid);
            } catch (IOException e) {
                throw new MCRPersistenceException("Error while accessing MCRObject: ", e);
            }
        });
        LOGGER.info("Migrated {} Objects to XML", i);
        this.adaptionRepo.getRepository().close();
    }
}
