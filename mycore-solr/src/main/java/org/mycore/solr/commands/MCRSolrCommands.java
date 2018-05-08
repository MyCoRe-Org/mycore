/**
 *
 */
package org.mycore.solr.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
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
import org.mycore.solr.classification.MCRSolrClassificationUtil;
import org.mycore.solr.index.MCRSolrIndexer;
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
        syntax = "rebuild solr metadata index", help = "rebuilds solr's metadata index", order = 20)
    public static void rebuildMetadataIndex() {
        MCRSolrIndexer.rebuildMetadataIndex();
    }

    @MCRCommand(
        syntax = "rebuild solr content index", help = "rebuilds solr's content index", order = 30)
    public static void rebuildContentIndex() {
        MCRSolrIndexer.rebuildContentIndex();
    }

    @MCRCommand(
        syntax = "rebuild solr classification index", help = "rebuilds solr's classification index", order = 35)
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
        help = "An optimize is like a hard commit except that it forces all of the index segments to be merged into a single segment first. "
            + "Depending on the use cases, this operation should be performed infrequently (like nightly), "
            + "if at all, since it is very expensive and involves reading and re-writing the entire index",
        order = 80)
    public static void optimize() {
        MCRSolrIndexer.optimize();
    }

    @MCRCommand(
        syntax = "drop solr index", help = "Deletes an existing index from solr", order = 90)
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
        syntax = "delete from solr index by id {0}", help = "Deletes an document from the index by id", order = 110)
    public static void deleteByIdFromSolr(String solrID) {
        MCRSolrIndexer.deleteById(solrID);
    }

    @MCRCommand(
        syntax = "set solr server {0}",
        help = "Sets a new SOLR server, {0} specifies the URL of the SOLR Server",
        order = 130)
    public static void setSolrServer(String solrClientURL) {
        MCRSolrClientFactory.setSolrClient(solrClientURL);
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
        syntax = "create solr metadata and content index at {0}",
        help = "create solr's metadata and content index on specific solr server",
        order = 120)
    public static void createIndex(String url) throws Exception {
        SolrClient cuss = MCRSolrClientFactory.getConcurrentSolrClient();
        SolrClient hss = MCRSolrClientFactory.getSolrClient();
        MCRSolrIndexer.rebuildMetadataIndex(cuss);
        MCRSolrIndexer.rebuildContentIndex(hss);
        if (cuss instanceof ConcurrentUpdateSolrClient) {
            ((ConcurrentUpdateSolrClient) cuss).blockUntilFinished();
        }
        hss.optimize();
    }

    @MCRCommand(
        syntax = "synchronize metadata index", help = "synchronizes the database and solr server", order = 150)
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
        syntax = "select objects with solr query {0}", help = "selects mcr objects with a solr query", order = 180)
    public static void selectObjectsWithSolrQuery(String query) throws Exception {
        SolrClient solrClient = MCRSolrClientFactory.getSolrClient();
        List<String> ids = MCRSolrSearchUtils.listIDs(solrClient, query);
        MCRObjectCommands.setSelectedObjectIDs(ids);
    }
    
    /**
     * This command tries to identify MyCoRe Objects missing in SOLR and reindexes them
     * using the repair metadata search command
     * The same functionality is provided by MyCoRe's synchronize command, 
     * which is more performant since it only repairs the metadata index.
     * but has a bug when executed on SOLR 4.
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
        objectTypes.remove("derivate");
        SolrClient solrClient = MCRSolrClientFactory.getSolrClient();
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
    
}
