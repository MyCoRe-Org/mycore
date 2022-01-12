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

    private static MCROCFLMigrationUtil migrationUtil;

    private static MCROcflUtil ocflUtil = new MCROcflUtil();

    @MCRCommand(syntax = "export ocfl repository {0}",
        order = 2,
        help = "export repository {0} to ocfl-export")
    public static void exportRepository(String repositoryKey) throws IOException {
        ocflUtil.setRepositoryKey(repositoryKey).updateMainRepo().exportRepository();
        LOGGER.info("Successfully exported repository {}", repositoryKey);
    }

    @MCRCommand(syntax = "export ocfl object {0} in repository {1}",
        order = 2,
        help = "export object {0} in repository {1} to ocfl-export")
    public static void exportObject(String mcrid, String repositoryKey) throws IOException {
        ocflUtil.setRepositoryKey(repositoryKey).updateMainRepo().exportObject(mcrid);
        LOGGER.info("Successfully exported object {} from repository {}", mcrid, repositoryKey);
    }

    @MCRCommand(syntax = "export ocfl repository",
        order = 4,
        help = "export default repository to ocfl-export")
    public static void exportRepository() throws IOException {
        ocflUtil.setRepositoryKey(CONFIGURED_REPOSITORY).updateMainRepo().exportRepository();
        LOGGER.info("Successfully exported repository {}", CONFIGURED_REPOSITORY);
    }

    @MCRCommand(syntax = "export ocfl object {0}",
        order = 4,
        help = "export object {0} in default repository to ocfl-export")
    public static void exportObject(String mcrid) throws IOException {
        ocflUtil.setRepositoryKey(CONFIGURED_REPOSITORY).updateMainRepo().exportObject(mcrid);
        LOGGER.info("Successfully exported object {} from repository {}", mcrid, CONFIGURED_REPOSITORY);
    }

    @MCRCommand(syntax = "import ocfl repository {0}",
        order = 2,
        help = "import repository {0} from ocfl-export")
    public static void importRepository(String repositoryKey) throws IOException {
        ocflUtil.setRepositoryKey(repositoryKey).updateMainRepo().importRepository();
        LOGGER.info("Successfully imported repository {}", repositoryKey);
    }

    @MCRCommand(syntax = "import ocfl repository",
        order = 4,
        help = "import default repository from ocfl-export")
    public static void importRepository() throws IOException {
        ocflUtil.setRepositoryKey(CONFIGURED_REPOSITORY).updateMainRepo().importRepository();
        LOGGER.info("Successfully imported repository {}", CONFIGURED_REPOSITORY);
    }

    @MCRCommand(syntax = "restore ocfl repository",
        order = 4,
        help = "restore default ocfl repository from backup if available")
    public static void restoreRepo() throws IOException {
        ocflUtil.setRepositoryKey(CONFIGURED_REPOSITORY).updateMainRepo().restoreRoot();
    }

    @MCRCommand(syntax = "restore ocfl repository {0}",
        order = 2,
        help = "restore ocfl repository {0} from backup if available")
    public static void restoreRepo(String repositoryKey) throws IOException {
        ocflUtil.setRepositoryKey(repositoryKey).updateMainRepo().restoreRoot();
    }

    @MCRCommand(syntax = "purge repository backup", order = 3, help = "clear the backup of a ocfl repository")
    public static void purgeRepoBackup() throws IOException {
        ocflUtil.clearBackup();
    }

    @MCRCommand(syntax = "migrate metadata from {0} to {1}",
        order = 4,
        help = "migrate metadata from {0} to {1} with default repository")
    public static void migrateMetadata(String from, String to) throws IOException {
        migrateMetadata(from, to, CONFIGURED_REPOSITORY);
    }

    @MCRCommand(syntax = "migrate metadata from {0} to {1} with {2}",
        order = 2,
        help = "migrate metadata from {0} to {1} with repository {2}")
    public static void migrateMetadata(String from, String to, String repositoryKey) throws IOException {
        migrationUtil = new MCROCFLMigrationUtil();
        switch (from) {
            case "xml":
            case "svn": // case "xml", "svn": on Java@14+
                switch (to) {
                    case "ocfl":
                        MCROCFLMigrationUtil.migrateXMLToOcfl(repositoryKey);
                        break;
                    default:
                        throw new MCRUsageException("Invalid Command Parameter 'to'");
                }
                break;
            case "ocfl":
                switch (to) {
                    case "ocfl":
                        migrationUtil.migrateOcflToOcfl(repositoryKey);
                        break;
                    case "xml": // case "xml", "svn": on Java@14+
                    case "svn":
                        migrationUtil.migrateOcflToXML(repositoryKey);
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
