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

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRObjectCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.MCRSolrConstants;
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
 */
@MCRCommandGroup(
    name = "SOLR Commands")
public class MCRSolrCommands extends MCRAbstractCommands {

    private static Logger LOGGER = LogManager.getLogger();

    @MCRCommand(syntax = "list solr configuration", order = 10)
    public static void listConfig() {
        LOGGER.info("List core configuration: {}{}{}{}", System.lineSeparator(),
            MCRSolrConstants.SOLR_CONFIG_PREFIX + ".ServerURL=" + MCRSolrConstants.DEFAULT_SOLR_SERVER_URL,
            System.lineSeparator(),
            MCRSolrClientFactory.getCoreMap().entrySet().stream().map(
                (entry) -> {
                    String coreID = entry.getKey();
                    MCRSolrCore core = entry.getValue();
                    String coreNameProp = System.lineSeparator() + MCRSolrConstants.SOLR_CORE_PREFIX + coreID
                        + MCRSolrConstants.SOLR_CORE_NAME_SUFFIX + "=" + core.getName();
                    if (MCRSolrConstants.DEFAULT_SOLR_SERVER_URL.equals(core.getServerURL())) {
                        return coreNameProp;
                    } else {
                        String coreServerProp =
                            MCRSolrConstants.SOLR_CORE_PREFIX + coreID + MCRSolrConstants.SOLR_CORE_SERVER_SUFFIX + "="
                                + core.getServerURL();
                        return coreNameProp + System.lineSeparator() + coreServerProp;
                    }
                }
            ).collect(Collectors.joining(System.lineSeparator())));
    }

    @MCRCommand(syntax = "create solr core with name {0} with configset {1}", order = 20)
    public static void createSolrCoRe(String coreName, String configSet)
        throws IOException, SolrServerException {
        createSolrCoRe(coreName, MCRSolrConstants.DEFAULT_SOLR_SERVER_URL, configSet);
    }

    @MCRCommand(syntax = "create solr core with name {0} at {1} with configset {2}", order = 30)
    public static void createSolrCoRe(String coreName, String server, String configSet)
        throws IOException, SolrServerException {
        CoreAdminRequest.Create create = new CoreAdminRequest.Create();
        create.setCoreName(coreName);
        create.setConfigSet(configSet);
        create.setIsLoadOnStartup(true);

        final MCRSolrCore core = new MCRSolrCore(server, coreName);
        CoreAdminResponse response = create.process(core.getClient());
        LOGGER.info("Core Create Response: {}", response);
    }

    @MCRCommand(syntax = "register core {0} as {1}", order = 40)
    public static void registerSolrCore(String coreName, String coreID) {
        MCRSolrClientFactory.addCore(MCRSolrConstants.DEFAULT_SOLR_SERVER_URL, coreName, coreID);
    }

    @MCRCommand(syntax = "register core {0} from {1} as {2}", order = 50)
    public static void registerSolrCore(String coreName, String server, String coreID) {
        MCRSolrClientFactory.addCore(server, coreName, coreID);
    }

    @MCRCommand(syntax = "switch solr core {0} with {1}", order = 60)
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
     * @param coreType the core type of the core that should be reloaded; the MyCoRe default application
     * core type is <b>main</b>
     */
    @MCRCommand(syntax = "reload solr configuration for core {0} with type {1}",
        help = "The command reloads the schema and the configuration in solr by using the solr schema api for core type {0}",
        order = 70)
    public static void reloadSolrConfiguration(String coreID, String coreType) {
        MCRSolrSchemaReloader.clearSchema(coreID, coreType);
        MCRSolrSchemaReloader.processSchemaFiles(coreID, coreType);
        MCRSolrConfigReloader.processConfigFiles(coreID, coreType);
    }

    @MCRCommand(
        syntax = "rebuild solr metadata and content index in core {0}",
        help = "rebuilds solr's metadata and content index for the core with the id {0}",
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
        order = 130
    )
    public static void rebuildMetadataIndexType(String type, String coreID) throws Exception {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.rebuildMetadataIndex(type, core.getClient());
    }

    @MCRCommand(
        syntax = "rebuild solr metadata index for object {0} in core {1}",
        order = 120
    )
    public static void rebuildMetadataIndexObject(String object, String coreID) throws Exception {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.rebuildMetadataIndex(Stream.of(object).collect(Collectors.toList()), core.getClient());
    }

    @MCRCommand(
        syntax = "rebuild solr metadata index for selected in core {0}",
        help = "rebuilds solr's content index for selected objects and or derivates in the core with the coreID {0}",
        order = 140)
    public static void rebuildMetadataIndexForSelected(String coreID) {
        MCRSolrCore core = getCore(coreID);
        List<String> selectedObjects = MCRObjectCommands.getSelectedObjectIDs();
        MCRSolrIndexer.rebuildMetadataIndex(selectedObjects, core.getClient());
    }

    @MCRCommand(
        syntax = "rebuild solr metadata index in core {0}",
        help = "rebuilds solr's metadata index in the core with the coreID {0}",
        order = 150)
    public static void rebuildMetadataIndex(String coreID) {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.rebuildMetadataIndex(core.getClient());
    }

    @MCRCommand(
        syntax = "rebuild solr content index for object {0} in core {1}",
        help = "rebuilds solr's content index for the derivate with the id {0} and the core with the coreID {1}",
        order = 160)
    public static void rebuildContentIndexObject(String objectID, String coreID) {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.rebuildContentIndex(Stream.of(objectID).collect(Collectors.toList()), core.getClient());
    }

    @MCRCommand(
        syntax = "rebuild solr content index for selected in core {0}",
        help = "rebuilds solr's content index for selected derivates in the core with the coreID {0}",
        order = 170)
    public static void rebuildContentIndexForSelected(String coreID) {
        MCRSolrCore core = getCore(coreID);
        List<String> selectedObjects = MCRObjectCommands.getSelectedObjectIDs();
        MCRSolrIndexer.rebuildContentIndex(selectedObjects, core.getClient());
    }

    @MCRCommand(
        syntax = "rebuild solr content index in core {0}",
        help = "rebuilds solr's content index in the core with the coreID {0}",
        order = 180)
    public static void rebuildContentIndex(String coreID) {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.rebuildContentIndex(core.getClient());
    }

    @MCRCommand(
        syntax = "rebuild solr classification index in core {0}",
        help = "rebuilds solr's classification index in the core with the coreID {0}",
        order = 190)
    public static void rebuildClassificationIndex(String coreID) {
        MCRSolrCore core = getCore(coreID);
        MCRSolrClassificationUtil.rebuildIndex(core.getClient());
    }

    @MCRCommand(
        syntax = "clear solr index in core {0}",
        help = "Deletes the index from the core with the coreID {0}",
        order = 210)
    public static void dropIndex(String coreID) throws Exception {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.dropIndex(core.getClient());
    }

    @MCRCommand(
        syntax = "delete from solr index all objects of type {0} in core {1}",
        help = "Deletes an existing index from solr but only for the given object type.",
        order = 220)
    public static void dropIndexByType(String type, String coreID) throws Exception {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.dropIndexByType(type, core.getClient());
    }

    @MCRCommand(
        syntax = "delete from solr index object {0} in core {1}",
        help = "Deletes an document from the core with coreId {1} by object id {0}",
        order = 230)
    public static void deleteByIdFromSolr(String objectID, String coreID) {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.deleteById(core.getClient(), objectID);
    }

    @MCRCommand(
        syntax = "select objects with solr query {0} in core {1}",
        help = "selects mcr objects with a solr query",
        order = 310)
    public static void selectObjectsWithSolrQuery(String query, String coreID) throws Exception {
        MCRSolrCore core = getCore(coreID);
        MCRObjectCommands.setSelectedObjectIDs(MCRSolrSearchUtils.listIDs(core.getClient(), query));
    }

    @MCRCommand(
        syntax = "optimize solr index in core {0}\n",
        help =
            "An optimize is like a hard commit except that it forces all of the index segments to be merged into a single segment first. "
                + "Depending on the use cases, this operation should be performed infrequently (like nightly), "
                + "if at all, since it is very expensive and involves reading and re-writing the entire index",
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
        syntax = "restricted synchronize metadata index for objecttype {0}",
        help = "synchronizes the mycore store and solr server",
        order = 160)
    public static void synchronizeMetadataIndex(String coreID, String objectType) throws Exception {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.synchronizeMetadataIndex(core.getClient(), objectType);
    }

    @MCRCommand(
        syntax = "synchronize solr metadata index in core {0}",
        help = "synchronizes the database and solr server",
        order = 150)
    public static void synchronizeMetadataIndex(String coreID) throws Exception {
        MCRSolrCore core = getCore(coreID);
        MCRSolrIndexer.synchronizeMetadataIndex(core.getClient());
    }

}
