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

package org.mycore.ocfl.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImpl;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.ocfl.MCROCFLMigration;
import org.mycore.ocfl.MCROCFLPersistenceTransaction;
import org.mycore.ocfl.MCROCFLRepositoryProvider;
import org.mycore.ocfl.MCROCFLXMLClassificationManager;

import edu.wisc.library.ocfl.api.OcflRepository;

@MCRCommandGroup(name = "OCFL Commands")
public class MCROCFLCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String SUCCESS = "success";

    public static final String SUCCESS_BUT_WITHOUT_HISTORY = SUCCESS + " but without history";

    public static final String FAILED = "failed";

    public static final String FAILED_AND_NOW_INVALID_STATE = FAILED + " and now invalid state";

    @MCRCommand(syntax = "migrate metadata to repository {0}",
        help = "migrates all the metadata to the ocfl " +
            "repository with the id {0}")
    public static void migrateToOCFL(String repository) {
        MCROCFLMigration migration = new MCROCFLMigration(repository);

        migration.start();

        ArrayList<String> success = migration.getSuccess();
        ArrayList<String> failed = migration.getFailed();
        ArrayList<String> invalidState = migration.getInvalidState();
        ArrayList<String> withoutHistory = migration.getWithoutHistory();

        LOGGER.info("The migration resulted in \n" +
            SUCCESS + ": {}, \n" +
            FAILED + ": {} \n" +
            FAILED_AND_NOW_INVALID_STATE + ": {} \n" +
            SUCCESS_BUT_WITHOUT_HISTORY + ": {} \n",
            String.join(", ", success),
            String.join(", ", failed),
            String.join(", ", invalidState),
            String.join(", ", withoutHistory));

        LOGGER.info("The migration resulted in \n" +
            SUCCESS + ": {}, \n" +
            FAILED + ": {} \n" +
            FAILED_AND_NOW_INVALID_STATE + ": {} \n" +
            SUCCESS_BUT_WITHOUT_HISTORY + ": {} \n",
            success.size(),
            failed.size(),
            invalidState.size(),
            withoutHistory.size());
    }

    @MCRCommand(syntax = "update ocfl classifications",
        help = "Update all Classifications in the OCFL Store from Database")
    public static void updateOCFLClassifications() {
        List<MCRCategoryID> list = new MCRCategoryDAOImpl().getRootCategoryIDs();
        try {
            list.forEach(cId -> {
                MCRCategory category = new MCRCategoryDAOImpl().getCategory(cId, -1);
                MCROCFLPersistenceTransaction.addClassfication(cId, category);
            });
            LOGGER.info("Staged {} Classifications for Update in OCFL Store", list.size());
        } catch (Exception e) {
            throw new MCRException("Error Updating OCFL Storage:", e);
        }
    }

    @MCRCommand(syntax = "repair ocfl classifications",
        help = "Update all Classifications and delete deleted Classifications to resync OCFL Store to the Database")
    public static void syncClassificationRepository() {
        String repositoryKey = MCRConfiguration2.getStringOrThrow("MCR.Classification.Manager.Repository");
        List<MCRCategoryID> list = new MCRCategoryDAOImpl().getRootCategoryIDs();

        try {
            list.forEach(cId -> {
                MCRCategory category = new MCRCategoryDAOImpl().getCategory(cId, -1);
                MCROCFLPersistenceTransaction.addClassfication(cId, category);
            });
            LOGGER.info("Staged {} Classifications for Update in OCFL Store {}", list.size(), repositoryKey);
        } catch (Exception e) {
            throw new MCRException("Error Updating OCFL Storage:", e);
        }

        List<String> classDAOList = list.stream().map(MCRCategoryID::toString)
            .collect(Collectors.toList());
        OcflRepository repository = MCROCFLRepositoryProvider.getRepository(repositoryKey);
        List<String> outOfSync = repository.listObjectIds()
            .filter(obj -> obj.contains("mcrclass"))
            .filter(obj -> !MCROCFLXMLClassificationManager.MESSAGE_DELETED.equals(repository.describeObject(obj)
                .getHeadVersion().getVersionInfo().getMessage()))
            .map(obj -> obj.replace("mcrclass:", ""))
            // .filter(obj -> !classDAOList.contains(obj))
            .filter(Predicate.not(classDAOList::contains))
            .collect(Collectors.toList());

        try {
            outOfSync
                .forEach(cId -> MCROCFLPersistenceTransaction.addClassfication(MCRCategoryID.fromString(cId), null));
            LOGGER.info("Staged {} Classifications for Deletion in OCFL Store {}", outOfSync.size(), repositoryKey);
        } catch (Exception e) {
            throw new MCRException("Error Updating OCFL Storage:", e);
        }

    }
}
