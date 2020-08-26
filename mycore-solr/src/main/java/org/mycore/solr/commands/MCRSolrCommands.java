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

package org.mycore.solr.commands;

import static org.mycore.solr.MCRSolrConstants.DEFAULT_SOLR_SERVER_URL;
import static org.mycore.solr.MCRSolrConstants.SOLR_CONFIG_PREFIX;
import static org.mycore.solr.MCRSolrConstants.SOLR_CORE_NAME_SUFFIX;
import static org.mycore.solr.MCRSolrConstants.SOLR_CORE_PREFIX;
import static org.mycore.solr.MCRSolrConstants.SOLR_CORE_SERVER_SUFFIX;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRObjectCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.MCRSolrCore;
import org.mycore.solr.MCRSolrUtils;
import org.mycore.solr.classification.MCRSolrClassificationUtil;
import org.mycore.solr.index.MCRSolrIndexer;
import org.mycore.solr.schema.MCRSolrConfigReloader;
import org.mycore.solr.schema.MCRSolrSchemaReloader;
import org.mycore.solr.search.MCRSolrSearchUtils;

/**
 * Class provides useful solr related commands.
 *
 * @author shermann
 * @author Sebastian Hofmann
 */
@MCRCommandGroup(
    name = "SOLR Commands")
public class MCRSolrCommands extends MCRAbstractCommands {

    private static Logger LOGGER = LogManager.getLogger();

    /**
     * This command displays the MyCoRe properties,
     * which have to be set, to reload the current Solr / Solr Core configuration
     * on the next start of the MyCoRe application.
     */
    @MCRCommand(
        syntax = "show solr configuration",
        help = "displays MyCoRe properties for the current Solr configuration",
        order = 10)
    public static void listConfig() {
        LOGGER.info("List core configuration: {}{}{}{}", System.lineSeparator(),
            SOLR_CONFIG_PREFIX + "ServerURL=" + DEFAULT_SOLR_SERVER_URL,
            System.lineSeparator(),
            MCRSolrClientFactory.getCoreMap().entrySet().stream().map(
                (entry) -> {
                    String coreID = entry.getKey();
                    MCRSolrCore core = entry.getValue();

                    String format = "{0}{1}{2}={3}";
                    if (!DEFAULT_SOLR_SERVER_URL.equals(core.getServerURL())) {
                        format += "\n{0}{1}{5}={4}";
                    }

                    return new MessageFormat(format, Locale.ROOT).format(new String[] { SOLR_CORE_PREFIX, coreID,
                        SOLR_CORE_NAME_SUFFIX, core.getName(), core.getServerURL(), SOLR_CORE_SERVER_SUFFIX, });
                }).collect(Collectors.joining("\n")));
    }

    @MCRCommand(
        syntax = "register solr core with name {0} on server {1} as core {2}",
        help = "registers a Solr core within MyCoRe",
        order = 40)
    public static void registerSolrCore(String coreName, String server, String coreID) {
        MCRSolrClientFactory.addCore(server, coreName, coreID);
    }

    @MCRCommand(
        syntax = "register solr core with name {0} as core {1}",
        help = "registers a Solr core on the configured default Solr server within MyCoRe",
        order = 50)
    public static void registerSolrCore(String coreName, String coreID) {
        MCRSolrClientFactory.addCore(DEFAULT_SOLR_SERVER_URL, coreName, coreID);
    }

    @MCRCommand(
        syntax = "switch solr core {0} with core {1}",
        help = "switches between two Solr core configurations",
        order = 60)
    public static void switchSolrCore(String coreID1, String coreID2) {
        MCRSolrCore core1 = getCore(coreID1);
        MCRSolrCore core2 = getCore(coreID2);

        MCRSolrClientFactory.add(coreID1, core2);
        MCRSolrClientFactory.add(coreID2, core1);
    }

    /**
     * This command recreates the managed-schema.xml and solrconfig.xml files. First it removes all
     * schema definitions, except for some MyCoRe default types and fields. Second it parses the available
     * MyCoRe modules and components and adds / updates / deletes the schema definition.
     * Finally it does the same for the solrconfig.xml definition.
     *
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
        syntax = "rebuild solr metadata and content index in core {0}",
        help = "rebuilds metadata and content index in Solr for core with the id {0}",
        order = 110)
    public static void rebuildMetadataAndContentIndex(String coreID) throws Exception {
        MCRSolrCore core = getCore(coreID);
        HttpSolrClient client = core.getClient();
        MCRSolrIndexer.rebuildMetadataIndex(client);
        MCRSolrIndexer.rebuildContentIndex(client);
        MCRSolrIndexer.optimize(client);
    }

    @MCRCommand(
        syntax = "rebuild solr metadata index for all objects of type {0} in core {1}",
        help = "rebuilds the metadata index in Solr for all objects of type {0} in core with the id {1}",
        order = 130)
    public static void rebuildMetadataIndexType(String type, String coreID) throws Exception {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.rebuildMetadataIndex(type, core.getClient());
    }

    @MCRCommand(
        syntax = "rebuild solr metadata index for object {0} in core {1}",
        help = "rebuilds metadata index in Solr for the given object in core with the id {1}",
        order = 120)
    public static void rebuildMetadataIndexObject(String object, String coreID) throws Exception {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.rebuildMetadataIndex(Stream.of(object).collect(Collectors.toList()), core.getClient());
    }

    @MCRCommand(
        syntax = "rebuild solr metadata index for selected in core {0}",
        help = "rebuilds content index in Solr for selected objects and or derivates in core with the id {0}",
        order = 140)
    public static void rebuildMetadataIndexForSelected(String coreID) {
        MCRSolrCore core = getCore(coreID);
        List<String> selectedObjects = MCRObjectCommands.getSelectedObjectIDs();
        MCRSolrIndexer.rebuildMetadataIndex(selectedObjects, core.getClient());
    }

    @MCRCommand(
        syntax = "rebuild solr metadata index in core {0}",
        help = "rebuilds metadata index in Solr in core with the id {0}",
        order = 150)
    public static void rebuildMetadataIndex(String coreID) {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.rebuildMetadataIndex(core.getClient());
    }

    @MCRCommand(
        syntax = "rebuild solr content index for object {0} in core {1}",
        help = "rebuilds content index in Solr for the all derivates of object with the id {0} "
            + "in core with the id {1}",
        order = 160)
    public static void rebuildContentIndexObject(String objectID, String coreID) {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.rebuildContentIndex(Stream.of(objectID).collect(Collectors.toList()), core.getClient());
    }

    @MCRCommand(
        syntax = "rebuild solr content index for selected in core {0}",
        help = "rebuilds content index in Solr for alll derivates of selected objects in core with the id {0}",
        order = 170)
    public static void rebuildContentIndexForSelected(String coreID) {
        MCRSolrCore core = getCore(coreID);
        List<String> selectedObjects = MCRObjectCommands.getSelectedObjectIDs();
        MCRSolrIndexer.rebuildContentIndex(selectedObjects, core.getClient());
    }

    @MCRCommand(
        syntax = "rebuild solr content index in core {0}",
        help = "rebuilds content index in Solr in core with the id {0}",
        order = 180)
    public static void rebuildContentIndex(String coreID) {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.rebuildContentIndex(core.getClient());
    }

    @MCRCommand(
        syntax = "rebuild solr classification index in core {0}",
        help = "rebuilds classification index in Solr in the core with the id {0}",
        order = 190)
    public static void rebuildClassificationIndex(String coreID) {
        MCRSolrCore core = getCore(coreID);
        MCRSolrClassificationUtil.rebuildIndex(core.getClient());
    }

    @MCRCommand(
        syntax = "clear solr index in core {0}",
        help = "deletes all entries from index in Solr in core with the id {0}",
        order = 210)
    public static void dropIndex(String coreID) throws Exception {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.dropIndex(core.getClient());
    }

    @MCRCommand(
        syntax = "delete from solr index all objects of type {0} in core {1}",
        help = "deletes all objects of type {0} from index in Solr in core with the id {1}",
        order = 220)
    public static void dropIndexByType(String type, String coreID) throws Exception {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.dropIndexByType(type, core.getClient());
    }

    @MCRCommand(
        syntax = "delete from solr index object {0} in core {1}",
        help = "deletes an object with id {0} from index in Solr in core with the id {1}",
        order = 230)
    public static void deleteByIdFromSolr(String objectID, String coreID) {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.deleteById(core.getClient(), objectID);
    }

    @MCRCommand(
        syntax = "select objects with solr query {0} in core {1}",
        help = "selects mcr objects with a solr query {0} in core with the id {1}",
        order = 310)
    public static void selectObjectsWithSolrQuery(String query, String coreID) throws Exception {
        MCRSolrCore core = getCore(coreID);
        MCRObjectCommands.setSelectedObjectIDs(MCRSolrSearchUtils.listIDs(core.getClient(), query));
    }

    /**
     * This command optimizes the index in Solr in a given core.
     * The operation works like a hard commit and forces all of the index segments
     * to be merged into a single segment first.
     * Depending on the use cases, this operation should be performed infrequently (e.g. nightly)
     * since it is very expensive and involves reading and re-writing the entire index.
     */
    @MCRCommand(
        syntax = "optimize solr index in core {0}",
        help = "optimizes the index in Solr in core with the id {0}. "
            + "The operation works like a hard commit and forces all of the index segments "
            + "to be merged into a single segment first. "
            + "Depending on the use cases, this operation should be performed infrequently (e.g. nightly), "
            + "since it is very expensive and involves reading and re-writing the entire index",
        order = 410)
    public static void optimize(String coreID) {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.optimize(core.getClient());
    }

    private static MCRSolrCore getCore(String coreID) {
        return MCRSolrClientFactory.get(coreID)
            .orElseThrow(() -> MCRSolrUtils.getCoreConfigMissingException(coreID));
    }

    @MCRCommand(
        syntax = "synchronize solr metadata index for all objects of type {0} in core {1}",
        help = "synchronizes the MyCoRe store and index in Solr in core with the id {1} for objects of type {0}",
        order = 420)
    public static void synchronizeMetadataIndex(String objectType, String coreID) throws Exception {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.synchronizeMetadataIndex(core.getClient(), objectType);
    }

    @MCRCommand(
        syntax = "synchronize solr metadata index in core {0}",
        help = "synchronizes the MyCoRe store and index in Solr in core with the id {0}",
        order = 430)
    public static void synchronizeMetadataIndex(String coreID) throws Exception {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.synchronizeMetadataIndex(core.getClient());
    }
}
