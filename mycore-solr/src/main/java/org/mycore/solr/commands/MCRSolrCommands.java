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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectUtils;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRObjectCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.MCRSolrConstants;
import org.mycore.solr.MCRSolrCore;
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

    @MCRCommand(
        syntax = "rebuild solr metadata and content index",
        help = "rebuilds solr's metadata and content index",
        order = 10)
    public static void rebuildMetadataAndContentIndex() throws Exception {
        MCRSolrIndexer.rebuildMetadataAndContentIndex();
    }

    @MCRCommand(
        syntax = "rebuild solr metadata index",
        help = "rebuilds solr's metadata index",
        order = 20)
    public static void rebuildMetadataIndex() {
        MCRSolrIndexer.rebuildMetadataIndex();
    }

    @MCRCommand(
        syntax = "rebuild solr content index",
        help = "rebuilds solr's content index",
        order = 30)
    public static void rebuildContentIndex() {
        MCRSolrIndexer.rebuildContentIndex();
    }

    @MCRCommand(
        syntax = "rebuild solr classification index",
        help = "rebuilds solr's classification index",
        order = 35)
    public static void rebuildClassificationIndex() {
        MCRSolrClassificationUtil.rebuildIndex();
    }

    @MCRCommand(
        syntax = "restricted rebuild solr metadata index for objecttype {0}",
        help = "rebuilds solr's metadata index for the given type in {0}",
        order = 40)
    public static void rebuildMetadataIndex(String type) {
        MCRSolrIndexer.rebuildMetadataIndex(type);
    }

    @MCRCommand(
        syntax = "optimize solr index",
        help =
            "An optimize is like a hard commit except that it forces all of the index segments to be merged into a single segment first. "
                + "Depending on the use cases, this operation should be performed infrequently (like nightly), "
                + "if at all, since it is very expensive and involves reading and re-writing the entire index",
        order = 80)
    public static void optimize() {
        MCRSolrIndexer.optimize();
    }

    @MCRCommand(
        syntax = "drop solr index",
        help = "Deletes an existing index from solr",
        order = 90)
    public static void dropIndex() throws Exception {
        MCRSolrIndexer.dropIndex();
    }

    @MCRCommand(
        syntax = "drop solr classification index",
        help = "Deletes an existing classification index from solr",
        order = 95)
    public static void dropClassificationIndex() {
        MCRSolrClassificationUtil.dropIndex();
    }

    @MCRCommand(
        syntax = "delete from solr index for type {0}",
        help = "Deletes an existing index from solr but only for the given object type.",
        order = 100)
    public static void dropIndexByType(String type) throws Exception {
        MCRSolrIndexer.dropIndexByType(type);
    }

    @MCRCommand(
        syntax = "delete from solr index by id {0}",
        help = "Deletes an document from the index by id",
        order = 110)
    public static void deleteByIdFromSolr(String solrID) {
        MCRSolrIndexer.deleteById(solrID);
    }

    @MCRCommand(
        syntax = "restricted rebuild solr metadata index for selected",
        help = "rebuilds solr's metadata index for selected objects",
        order = 50)
    public static void rebuildMetadataIndexForSelected() {
        List<String> selectedObjects = MCRObjectCommands.getSelectedObjectIDs();
        MCRSolrIndexer.rebuildMetadataIndex(selectedObjects);
    }

    @MCRCommand(
        syntax = "restricted rebuild solr content index for selected",
        help = "rebuilds solr's content index for selected objects and or derivates",
        order = 60)
    public static void rebuildContentIndexForSelected() {
        List<String> selectedObjects = MCRObjectCommands.getSelectedObjectIDs();
        MCRSolrIndexer.rebuildContentIndex(selectedObjects);
    }

    @MCRCommand(
        syntax = "restricted rebuild solr metadata index for object {0}",
        help = "rebuilds solr's metadata index for object and all its children",
        order = 70)
    public static void rebuildMetadataIndexForObject(String id) {
        MCRObject mcrObject = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(id));
        List<MCRObject> objectList = MCRObjectUtils.getDescendantsAndSelf(mcrObject);
        List<String> idList = objectList.stream().map(obj -> obj.getId().toString()).collect(Collectors.toList());
        MCRSolrIndexer.rebuildMetadataIndex(idList);
    }

    @MCRCommand(
        syntax = "create solr metadata and content index on server {0} with core name {1}",
        help = "create solr's metadata and content index on specific solr server core",
        order = 120)
    public static void createIndex(String server, String coreName) throws Exception {
        MCRSolrCore core = new MCRSolrCore(server, coreName);
        SolrClient concurrentSolrClient = core.getConcurrentClient();
        SolrClient solrClient = core.getClient();
        MCRSolrIndexer.rebuildMetadataIndex(concurrentSolrClient);
        MCRSolrIndexer.rebuildContentIndex(solrClient);
        if (concurrentSolrClient instanceof ConcurrentUpdateSolrClient) {
            ((ConcurrentUpdateSolrClient) concurrentSolrClient).blockUntilFinished();
        }
        solrClient.optimize();
    }

    @MCRCommand(
        syntax = "create solr objecttype {0} for core type {1}",
        help = "indexes all objects of an object type (e.g. document) on specific solr server core",
        order = 125)
    public static void createObjectType(String type, String coreType) throws Exception {
        MCRSolrCore core = MCRSolrClientFactory.get(coreType)
            .orElseThrow(() -> new MCRConfigurationException("The core " + coreType + " is not configured!"));
        SolrClient concurrentSolrClient = core.getConcurrentClient();
        MCRSolrIndexer.rebuildMetadataIndex(MCRXMLMetadataManager.instance().listIDsOfType(type), concurrentSolrClient);
        concurrentSolrClient.optimize();
    }

    @MCRCommand(
        syntax = "synchronize metadata index",
        help = "synchronizes the database and solr server",
        order = 150)
    public static void synchronizeMetadataIndex() throws Exception {
        MCRSolrIndexer.synchronizeMetadataIndex();
    }

    @MCRCommand(
        syntax = "restricted synchronize metadata index for objecttype {0}",
        help = "synchronizes the mycore store and solr server",
        order = 160)
    public static void synchronizeMetadataIndex(String objectType) throws Exception {
        MCRSolrIndexer.synchronizeMetadataIndex(objectType);
    }

    @MCRCommand(
        syntax = "select objects with solr query {0}",
        help = "selects mcr objects with a solr query",
        order = 180)
    public static void selectObjectsWithSolrQuery(String query) throws Exception {
        SolrClient client = MCRSolrClientFactory.getMainSolrClient();
        MCRObjectCommands.setSelectedObjectIDs(MCRSolrSearchUtils.listIDs(client, query));
    }

    /**
     * This command tries to identify MyCoRe Objects missing in SOLR and reindexes them using the
     * repair metadata search command.
     *
     * The same functionality is provided by MyCoRe's synchronize command,
     * which is more performant since it only repairs the metadata index but has a bug when executed on SOLR 4.
     *
     * @version MyCoRe 2017.06 LTS, 2018.06 LTS (should be removed in later versions)
     *
     * @return
     * @throws Exception
     */
    @MCRCommand(syntax = "synchronized repair metadata search",
        help = "synchronizes the metadata store and solr index (for SOLR 4)",
        order = 190)
    public static List<String> synchronizeAndRepairSolrIndex() throws Exception {
        List<String> result = new ArrayList<>();
        Collection<String> objectTypes = MCRXMLMetadataManager.instance().getObjectTypes();

        final SolrClient solrClient = MCRSolrClientFactory.getMainSolrClient();
            for (String objectType : objectTypes) {
                LOGGER.info("synchronize SOLR index for object type: " + objectType);
                // get ids from store
                List<String> storeList = MCRXMLMetadataManager.instance().listIDsOfType(objectType);
                LOGGER.info("there are " + storeList.size() + " mycore objects");
                List<String> solrList = MCRSolrSearchUtils.listIDs(solrClient, "objectType:" + objectType);
                LOGGER.info("there are " + solrList.size() + " solr objects");

                // documents to remove
                for (String id : solrList) {
                    if (!storeList.contains(id)) {
                        result.add("delete from solr index by id " + id);
                    }
                }
                LOGGER.info("remove " + result.size() + " zombie objects from solr");

                // documents to add
                storeList.removeAll(solrList);
                if (!storeList.isEmpty()) {
                    LOGGER.info("reindex " + storeList.size() + " mycore objects");
                    for (String id : storeList) {
                        result.add("repair metadata search of ID " + id);
                    }
                }
            }

        return result;
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
    @MCRCommand(syntax = "reload solr configuration for core of type {0}",
        help = "The command reloads the schema and the configuration in solr by using the solr schema api for core type {0}",
        order = 210)
    public static void reloadSolrConfiguration(String coreType) {
        MCRSolrSchemaReloader.clearSchema(coreType);
        MCRSolrSchemaReloader.processSchemaFiles(coreType);
        MCRSolrConfigReloader.processConfigFiles(coreType);
    }

    /**
     * The command uses the SOLR Admin API to create a new core.
     * 
     * @param coreType the core type of the core that should be reloaded; the MyCoRe default application 
     * core type is <b>main</b>
     * @param coreName the name of the core; the part following the SOLR server url
     * @param templateName the name of the configuration set, which should be used as base
     * @throws IOException
     * @throws SolrServerException
     */
    @MCRCommand(syntax = "create solr core of type {0} with name {1} from template {2}",
            help = "The command creates a new empty core with the given name based on the named core template",
            order = 220)
    public static final void createSolrCore(String coreType, String coreName, String templateName) throws IOException,
        SolrServerException {
	    CoreAdminRequest.Create create = new CoreAdminRequest.Create();
	    create.setCoreName(coreName);
	    create.setConfigSet(templateName);
	    create.setIsLoadOnStartup(true);

        SolrClient solrClient = MCRSolrClientFactory.addCore(MCRSolrConstants.DEFAULT_SOLR_SERVER_URL, coreName, coreType)
            .getClient();
	    CoreAdminResponse response = create.process(solrClient);
	    LogManager.getLogger().info("Core Create Response: {}", response);
	}
}
