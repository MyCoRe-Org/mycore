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

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRUsageException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.ocfl.MCROCFLAdaptionRepositoryProvider;
import org.mycore.ocfl.MCROCFLBaseClass;

@MCRCommandGroup(name = "OCFL Commands")
public class MCROCFLCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CONFIGURED_REPOSITORY = MCRConfiguration2
        .getStringOrThrow("MCR.Metadata.Manager.Repository");

    public static MCROCFLBaseClass baseClass;

    @MCRCommand(syntax = "export repository {0}", help = "export repository {0} to ocfl-export")
    public static void exportRepository(String repositoryKey) throws IOException {
        MCROCFLAdaptionRepositoryProvider adapter = new MCROCFLAdaptionRepositoryProvider(repositoryKey);
        // MCROCFLAdaptionRepositoryProvider adapter = new MCROCFLAdaptionRepositoryProvider();
        adapter.init();
        adapter.exportRepository();
        // MCROCFLAdaptionRepositoryProvider.getRepository("Adapt");
        LOGGER.info("Successfully exported repository {}", repositoryKey);
    }

    @MCRCommand(syntax = "export object {0} in repository {1}",
        help = "export object {0} in repository {1} to ocfl-export")
    public static void exportObject(String mcrid, String repositoryKey) throws IOException {
        MCROCFLAdaptionRepositoryProvider adapter = new MCROCFLAdaptionRepositoryProvider(repositoryKey);
        adapter.init();
        adapter.exportObject(mcrid);
        LOGGER.info("Successfully exported object {}", mcrid);
    }

    @MCRCommand(syntax = "import repository {0}", help = "import repository {0} from ocfl-export")
    public static void importRepository(String repositoryKey) throws IOException {
        MCROCFLAdaptionRepositoryProvider adapter = new MCROCFLAdaptionRepositoryProvider(repositoryKey);
        adapter.init();
        adapter.importRepository();
        LOGGER.info("Successfully imported repository {}", repositoryKey);
    }

    @MCRCommand(syntax = "restore repository", help = "restore ocfl repository from backup if available")
    public static void restoreRepo() throws IOException {
        MCROCFLBaseClass.restoreRoot(CONFIGURED_REPOSITORY);
    }

    @MCRCommand(syntax = "purge repository backup", help = "clear the backup of a ocfl repository")
    public static void purgeRepoBCK() throws IOException {
        MCROCFLBaseClass.clearBackup(CONFIGURED_REPOSITORY);
    }

    @MCRCommand(syntax = "migrate metadata from {0} to {1}",
        help = "migrate metadata from {0} to {1} with repository Main")
    public static void migrateMetadata(String from, String to) throws IOException {
        migrateMetadata(from, to, CONFIGURED_REPOSITORY);
    }

    @MCRCommand(syntax = "migrate metadata from {0} to {1} with {2}",
        help = "migrate metadata from {0} to {1} with repository {2}")
    public static void migrateMetadata(String from, String to, String repositoryKey) throws IOException {
        baseClass = new MCROCFLBaseClass(repositoryKey);
        switch (from) {
            case "xml":
            case "svn":
                switch (to) {
                    case "ocfl":
                        MCROCFLBaseClass.convertXMLToOcfl(repositoryKey);
                        break;
                    default:
                        throw new MCRUsageException("Invalid Command Parameter 'to'");
                }
                break;
            case "ocfl":
                switch (to) {
                    case "ocfl":
                        baseClass.convertOcflToOcfl();
                        break;
                    case "xml":
                    case "svn":
                        baseClass.convertOcflToXML(repositoryKey);
                        break;
                    default:
                        throw new MCRUsageException("Invalid Command Parameter 'to'");
                }
                break;
            default:
                throw new MCRUsageException("Invalid Command Parameter 'from'");
        }
    }
}
