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
import java.util.ArrayList;
import java.util.Date;

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
public class MCROCFLMigrationUtil {

    private final static Logger LOGGER = LogManager.getLogger();

    private int i;

    private static MCROcflUtil util = new MCROcflUtil();

    private static final String SUCCESS = "success";

    private static final String SUCCESS_WITH_HISTORY = SUCCESS + " with history";

    private static final String SUCCESS_WITHOUT_HISTORY = SUCCESS + " without history";

    private static final String FAILED = "failed";

    private static final String FAILED_AND_NOW_INVALID_STATE = FAILED + " and now invalid state";

    /**
     * Convert from XML to OCFL using {@code MCROCFLMigration}
     * @param repository RepositoryKey
     * @throws IOException if an I/O error occurs
     */
    public static void migrateXMLToOcfl(String repository) throws IOException {

        MCROCFLMigration migration = new MCROCFLMigration(repository);

        migration.start();

        ArrayList<String> success = migration.getSuccess();
        ArrayList<String> failed = migration.getFailed();
        ArrayList<String> invalidState = migration.getInvalidState();
        ArrayList<String> withoutHistory = migration.getWithoutHistory();

        LOGGER.info("The migration resulted in: \n" +
            SUCCESS_WITH_HISTORY + ": {} \n" +
            SUCCESS_WITHOUT_HISTORY + ": {} \n" +
            FAILED + ": {} \n" +
            FAILED_AND_NOW_INVALID_STATE + ": {} \n",
            String.join(", ", success),
            String.join(", ", withoutHistory),
            String.join(", ", failed),
            String.join(", ", invalidState));

        LOGGER.info("The migration resulted in: \n" +
            SUCCESS_WITH_HISTORY + ": {} \n" +
            SUCCESS_WITHOUT_HISTORY + ": {} \n" +
            FAILED + ": {} \n" +
            FAILED_AND_NOW_INVALID_STATE + ": {} \n",
            success.size(),
            withoutHistory.size(),
            failed.size(),
            invalidState.size());
    }

    /**
     * Convert between OCFL Layouts or create backups of Repos
     * <p>Info: due to how its implemented, if a Layout is not changed it just makes a backup</p>
     * @param repositoryKey Repository to migrate
     * @throws IOException if an I/O error occurs
     */
    public void migrateOcflToOcfl(String repositoryKey) throws IOException {
        if (null == util.getAdaptRepository()) {
            throw new MCRUsageException("Adapt Repository has not been initialized yet!");
        }
        util.setRepositoryKey(repositoryKey)
            .updateMainRepo()
            .exportRepository()
            .importAdapt()
            .updateRoot()
            .reloadRepository()
            .getAdaptRepository()
            .close();
        LOGGER.info("Migration finished, please restart MyCoRe with the new Layout");
    }

    /**
     * Copies all objects from the OCFL Repository into the Native Metadata Manager
     * @param repositoryKey Name of the Repository
     * @throws IOException if an I/O error occurs
     * @throws MCRException Metadata Manager <strong>MUST NOT</strong> be {@code MCROCFLXMLMetadataManager}
     */
    public void migrateOcflToXML(String repositoryKey) throws IOException, MCRException {
        MCROCFLAdaptionRepositoryProvider adaptRepo = util.adaptClass;
        if (null == adaptRepo) {
            throw new MCRUsageException("Adapt Repository has not been initialized yet!");
        }
        String managerConfig = MCRConfiguration2.getString("MCR.Metadata.Manager").orElse("xml");
        MCRXMLMetadataManager manager = MCRXMLMetadataManager.instance();
        MCROCFLXMLMetadataManager ocflManager = new MCROCFLXMLMetadataManager();
        ocflManager.setRepositoryKey(repositoryKey);
        if ("org.mycore.ocfl.MCROCFLXMLMetadataManager".equals(managerConfig)) {
            throw new MCRException(
                "Cannot convert from OCFL to XML with the OCFL Metadata Manager!\n" +
                    "Change the Metadata setting to use the Native XML Metadata Manager: " +
                    "'MCR.Metadata.Manager' set to 'MCROCFLXMLMetadataManager'");
        }
        adaptRepo.setRepositoryRoot(
            MCRConfiguration2.getStringOrThrow("MCR.OCFL.Repository." + repositoryKey + ".RepositoryRoot"));
        adaptRepo.init();
        ArrayList<MCRObjectID> newObjects = new ArrayList<MCRObjectID>();
        ArrayList<MCRObjectID> existObjects = new ArrayList<MCRObjectID>();
        i = 0;
        OcflRepository repo = adaptRepo.getRepository();
        repo.listObjectIds()
            .filter(id -> id.startsWith(MCROCFLXMLMetadataManager.MCR_OBJECT_ID_PREFIX))
            .map(id -> id.substring(MCROCFLXMLMetadataManager.MCR_OBJECT_ID_PREFIX.length()))
            .forEach(objId -> {
                MCRObjectID mcrid = MCRObjectID.getInstance(objId);
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
        util.getAdaptRepository().close();
    }
}
