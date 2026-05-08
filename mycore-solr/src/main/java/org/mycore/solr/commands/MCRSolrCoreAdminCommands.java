/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.solr.commands;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.solr.cloud.configsets.MCRSolrConfigSetHelper;
import org.mycore.solr.cloud.configsets.MCRSolrConfigSetProvider;
import org.mycore.solr.schema.MCRSolrConfigReloader;
import org.mycore.solr.schema.MCRSolrSchemaReloader;

@MCRCommandGroup(
    name = "SOLR Core Admin Commands")
public class MCRSolrCoreAdminCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * This command recreates the managed-schema.xml and solrconfig.xml files. First it removes all
     * schema definitions, except for some MyCoRe default types and fields. Second it parses the available
     * MyCoRe modules and components and adds / updates / deletes the schema definition.
     * Finally it does the same for the solrconfig.xml definition.
     * <p>
     * see https://github.com/MyCoRe-Org/mycore_solr_configset_main
     *
     * @param coreID the core type of the core that should be reloaded; the MyCoRe default application
     * coreID is <b>main</b>
     */
    @MCRCommand(
        syntax = "reload solr configuration {0} in core {1}",
        help = "reloads the schema and the configuration in Solr "
            + "by using the Solr schema api for core with the id {0}",
        order = 70)
    public static void reloadSolrConfiguration(String configType, String coreID) {
        MCRSolrConfigReloader.reset(configType, coreID);
        MCRSolrSchemaReloader.reset(configType, coreID);
        MCRSolrSchemaReloader.processSchemaFiles(configType, coreID);
        MCRSolrConfigReloader.processConfigFiles(configType, coreID);
    }

    @MCRCommand(
        syntax = "export solr configset {0} to folder {1}",
        help = "exports a solr configset to a folder",
        order = 80)
    public static void exportSolrConfigSet(String configSet, String folder) {
        Path exportFolder = Paths.get(folder);
        if (Files.notExists(exportFolder)) {
            LOGGER.error("Folder {} does not exist", exportFolder);
            return;
        }

        if (!Files.isDirectory(exportFolder)) {
            LOGGER.error("Folder {} is not a directory", exportFolder);
            return;
        }

        Map<String, MCRSolrConfigSetProvider> configSets = MCRSolrConfigSetHelper.getLocalConfigSets();
        if (!configSets.containsKey(configSet)) {
            LOGGER.error("ConfigSet {} not found", configSet);
            return;
        }

        MCRSolrConfigSetProvider configSetProvider = configSets.get(configSet);
        Path zipFile = exportFolder.resolve(configSet + ".zip");
        try (InputStream zipStream = configSetProvider.getStreamSupplier().get()) {
            Files.copy(zipStream, zipFile, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("ConfigSet {} exported to {}", configSet, zipFile);
        } catch (Exception e) {
            LOGGER.error("Error exporting configset", e);
        }
    }

}
