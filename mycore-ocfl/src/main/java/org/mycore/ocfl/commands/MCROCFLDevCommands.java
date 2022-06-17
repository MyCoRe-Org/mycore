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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.common.events.MCREvent;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImpl;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.ocfl.MCROCFLPersistenceTransaction;
import org.mycore.ocfl.MCROCFLXMLClassificationManager;

/**
 * Development Commands for the OCFL Module
 * @author Tobias Lenhardt [Hammer1279]
 */
@MCRCommandGroup(name = "OCFL Development Commands")
public class MCROCFLDevCommands {

    private static final Logger LOGGER = LogManager.getLogger(MCROCFLDevCommands.class);

    private static final Path EXPORT_DIR = Path.of(MCRConfiguration2.getStringOrThrow("MCR.savedir"), "class-export");

    private static final MCROCFLXMLClassificationManager OCFL_XML_CLASSIFICATION_MANAGER = MCRConfiguration2
        .getSingleInstanceOf("MCR.Classification.Manager", MCROCFLXMLClassificationManager.class)
        .orElseThrow();

    private MCROCFLDevCommands() {
        throw new IllegalStateException();
    }

    @MCRCommand(syntax = "export ocfl class {0} rev {1}",
        help = "export ocfl class {0} rev {1}",
        order = 2)
    public static void exportClassificationFromOCFL(String mclass, String rev) throws IOException {
        final MCRCategoryID classID = MCRCategoryID.rootID(mclass);
        MCRContent content = OCFL_XML_CLASSIFICATION_MANAGER.retrieveContent(classID, rev);
        Files.createDirectories(EXPORT_DIR);
        content.sendTo(EXPORT_DIR.resolve(classID + ".xml"), StandardCopyOption.REPLACE_EXISTING);
        LOGGER.info("Exported Class <{}> with Revision <{}> to <{}>", mclass, (rev == null) ? "HEAD" : rev, EXPORT_DIR);

    }

    @MCRCommand(syntax = "export ocfl class {0}",
        help = "export ocfl class {0}",
        order = 4)
    public static void exportClassificationFromOCFL(String mclass) throws IOException {
        exportClassificationFromOCFL(mclass, null);
    }

    @MCRCommand(syntax = "export all ocfl classes",
        help = "export all ocfl classes",
        order = 2)
    public static List<String> exportAllClassificationsFromOCFL() throws IOException {
        return MCRCategoryDAOFactory.getInstance()
            .getRootCategoryIDs()
            .stream()
            .map(MCRCategoryID::toString)
            .map(id -> "export ocfl class " + id)
            .collect(Collectors.toList());
    }

    // maybe use "repair ocfl class store" instead, plus "repair ocfl class {0}" to replace single class
    @MCRCommand(syntax = "rebuild ocfl class store",
        help = "Clear the OCFL Store Classifications and reload them from the Database\nWILL WIPE MCRCLASS FOLDER",
        order = 2)
    public static void rebuildClassStore() throws IOException {
        String repositoryKey = MCRConfiguration2.getStringOrThrow("MCR.Classification.Manager.Repository");
        String ocflRoot = MCRConfiguration2
            .getStringOrThrow("MCR.OCFL.Repository." + repositoryKey + ".RepositoryRoot");
        Path classDir = Path.of(ocflRoot, "mcrclass");
        try (Stream<Path> walker = Files.walk(classDir)) {
            walker.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        } catch (Exception e) {
            // throw new IOException(e);
            // expected Error incase the directory doesn't exist yet
        }
        List<MCRCategoryID> list = new MCRCategoryDAOImpl().getRootCategoryIDs();
        try {
            list.forEach(cId -> {
                MCREvent evt = new MCREvent(MCREvent.CLASS_TYPE, MCREvent.CREATE_EVENT);
                MCRCategory category = new MCRCategoryDAOImpl().getCategory(cId, -1);
                evt.put("class", category);
                MCROCFLPersistenceTransaction.addClassfication(cId, category);
            });
            LOGGER.info("Staged {} Objects for Update in OCFL Store", list.size());
        } catch (Exception e) {
            throw new MCRException("Error Updating Class Storage:", e);
        }
    }
}
