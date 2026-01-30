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

    /*


    @MCRCommand(
        syntax = "register solr core with name {0} as core {1}",
        help = "registers a Solr core on the configured default Solr server within MyCoRe",
        order = 50)
    public static void registerSolrCore(String remoteCoreName, String coreID) {
        MCRSolrCore core = new MCRSolrCore(DEFAULT_SOLR_SERVER_URL, remoteCoreName, null, 1, Collections.emptySet());
        MCRSolrCoreManager.addCore(coreID, core);
    }

    @MCRCommand(
        syntax = "add type {0} to solr core {1}",
        help = "Adds a type to a solr core, so that documents of this type are indexed in this core. " +
            "Type may be main or classification.",
        order = 65)
    public static void addTypeToSolrCore(String type, String coreID) {
        MCRSolrCore core = MCRSolrCoreManager.get(coreID).orElseThrow();
        core.getTypes().add(new MCRIndexType(type));
    }

    @MCRCommand(
        syntax = "switch solr core {0} with core {1}",
        help = "switches between two Solr core configurations",
        order = 60)
    public static void switchSolrCore(String coreID1, String coreID2) {

        MCRSolrCore core1 = MCRSolrCoreManager.get(coreID1).orElseThrow();
        MCRSolrCore core2 = MCRSolrCoreManager.get(coreID2).orElseThrow();

        MCRSolrCoreManager.add(coreID1, core2);
        MCRSolrCoreManager.add(coreID2, core1);
    }

    @MCRCommand(
        syntax = "remove type {0} from solr core {1}",
        help = "Removes a type from a solr core, so that documents of this type are not indexed in this core" +
            "anymore. Type may be main or classification.",
        order = 66)
    public static void removeTypeFromSolrCore(String type, String coreID) {
        MCRSolrCore core = MCRSolrCoreManager.get(coreID).orElseThrow();
        core.getTypes().remove(new MCRIndexType(type));
    }

    @MCRCommand(
        syntax = "set shard count {0} for solr core {1}",
        help = "Sets the number of shards for a solr core, the shard count is not updated in the solr server, but " +
            "used if the core is created",
        order = 70)
    public static void setShardCountForSolrCore(int shardCount, String coreID) {
        MCRSolrCore core = MCRSolrCoreManager.get(coreID).orElseThrow();
        core.setShardCount(shardCount);
    }

    @MCRCommand(syntax = "set configset {0} for solr core {1}",
        help = "Sets the configset for a solr core, the configset is not updated in the solr server, but " +
            "used if the core is created",
        order = 75)
    public static void setConfigSetForSolrCore(String configSet, String coreID) {
        MCRSolrCore core = MCRSolrCoreManager.get(coreID).orElseThrow();
        core.setConfigSet(configSet);
    }

    @MCRCommand(
        syntax = "set server {0} for solr core {1}",
        help = "Sets the server for a solr core, the server is not updated in the solr server, but " +
            "used if the core is created",
        order = 80)
    public static void setServerForSolrCore(String server, String coreID) {
        MCRSolrCore core = MCRSolrCoreManager.get(coreID).orElseThrow();
        core.setServerURL(server);
    }

    */

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
