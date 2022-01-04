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
import org.mycore.ocfl.MCROCFLMigrationUtil;
import org.mycore.ocfl.MCROcflUtil;

@MCRCommandGroup(name = "OCFL Commands")
public class MCROCFLCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CONFIGURED_REPOSITORY = MCRConfiguration2
        .getStringOrThrow("MCR.Metadata.Manager.Repository");

    private static final int WITH_REPOSITORY = 1;
        
    private static final int WITHOUT_REPOSITORY = 2;
        
    private static MCROCFLMigrationUtil migrationUtil;

    private static MCROcflUtil ocflUtil = new MCROcflUtil();

    @MCRCommand(syntax = "export repository {0}",
        order = WITH_REPOSITORY,
        help = "export repository {0} to ocfl-export")
    public static void exportRepository(String repositoryKey) throws IOException {
        ocflUtil.setRepositoryKey(repositoryKey).updateMainRepo().exportRepository();
        LOGGER.info("Successfully exported repository {}", repositoryKey);
    }

    @MCRCommand(syntax = "export object {0} in repository {1}",
        order = WITH_REPOSITORY,
        help = "export object {0} in repository {1} to ocfl-export")
    public static void exportObject(String mcrid, String repositoryKey) throws IOException {
        ocflUtil.setRepositoryKey(repositoryKey).updateMainRepo().exportObject(mcrid);
        LOGGER.info("Successfully exported object {} from repository {}", mcrid, repositoryKey);
    }

    @MCRCommand(syntax = "export repository",
        order = WITHOUT_REPOSITORY,
        help = "export default repository to ocfl-export")
    public static void exportRepository() throws IOException {
        ocflUtil.exportRepository();
        LOGGER.info("Successfully exported repository {}", CONFIGURED_REPOSITORY);
    }

    @MCRCommand(syntax = "export repository object {0}",
        order = WITHOUT_REPOSITORY,
        help = "export object {0} in default repository to ocfl-export")
    public static void exportObject(String mcrid) throws IOException {
        ocflUtil.exportObject(mcrid);
        LOGGER.info("Successfully exported object {} from repository {}", mcrid, CONFIGURED_REPOSITORY);
    }

    @MCRCommand(syntax = "import repository {0}",
        order = WITH_REPOSITORY,
        help = "import repository {0} from ocfl-export")
    public static void importRepository(String repositoryKey) throws IOException {
        ocflUtil.setRepositoryKey(repositoryKey).updateMainRepo().importRepository();
        LOGGER.info("Successfully imported repository {}", repositoryKey);
    }

    @MCRCommand(syntax = "import repository",
        order = WITHOUT_REPOSITORY,
        help = "import default repository from ocfl-export")
    public static void importRepository() throws IOException {
        ocflUtil.importRepository();
        LOGGER.info("Successfully imported repository {}", CONFIGURED_REPOSITORY);
    }

    @MCRCommand(syntax = "restore repository",
        order = WITHOUT_REPOSITORY,
        help = "restore default ocfl repository from backup if available")
    public static void restoreRepo() throws IOException {
        ocflUtil.restoreRoot();
    }

    @MCRCommand(syntax = "restore repository {0}",
    order = WITH_REPOSITORY,
    help = "restore ocfl repository {0} from backup if available")
    public static void restoreRepo(String repositoryKey) throws IOException {
        ocflUtil.setRepositoryKey(repositoryKey).updateMainRepo().restoreRoot();
    }

    @MCRCommand(syntax = "purge repository backup", order = 3, help = "clear the backup of a ocfl repository")
    public static void purgeRepoBackup() throws IOException {
        ocflUtil.clearBackup();
    }

    @MCRCommand(syntax = "migrate metadata from {0} to {1}",
        order = WITHOUT_REPOSITORY,
        help = "migrate metadata from {0} to {1} with default repository")
    public static void migrateMetadata(String from, String to) throws IOException {
        migrateMetadata(from, to, CONFIGURED_REPOSITORY);
    }

    @MCRCommand(syntax = "migrate metadata from {0} to {1} with {2}",
        order = WITH_REPOSITORY,
        help = "migrate metadata from {0} to {1} with repository {2}")
    public static void migrateMetadata(String from, String to, String repositoryKey) throws IOException {
        migrationUtil = new MCROCFLMigrationUtil(repositoryKey);
        switch (from) {
            case "xml":
            case "svn":
                switch (to) {
                    case "ocfl":
                        MCROCFLMigrationUtil.convertXMLToOcfl(repositoryKey);
                        break;
                    default:
                        throw new MCRUsageException("Invalid Command Parameter 'to'");
                }
                break;
            case "ocfl":
                switch (to) {
                    case "ocfl":
                        migrationUtil.convertOcflToOcfl(repositoryKey);
                        break;
                    case "xml":
                    case "svn":
                        migrationUtil.convertOcflToXML(repositoryKey);
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
