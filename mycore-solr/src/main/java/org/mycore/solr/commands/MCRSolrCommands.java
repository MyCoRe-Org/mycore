/**
 * 
 */
package org.mycore.solr.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.mycore.common.MCRObjectUtils;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRCommand;
import org.mycore.frontend.cli.MCRObjectCommands;
import org.mycore.solr.MCRSolrServerFactory;
import org.mycore.solr.index.MCRSolrIndexer;

/**
 * Class provides useful solr related commands.
 * 
 * @author shermann
 *
 */
public class MCRSolrCommands extends MCRAbstractCommands {

    public MCRSolrCommands() {
        super("Solr Commands");
        MCRCommand com = null;

        String solrIndexerClassName = MCRSolrIndexer.class.getCanonicalName();
        String commandClassName = getClass().getCanonicalName();

        com = new MCRCommand("rebuild solr metadata and content index", solrIndexerClassName + ".rebuildMetadataAndContentIndex",
            "rebuilds solr's metadata and content index");
        addCommand(com);

        com = new MCRCommand("rebuild solr metadata index", solrIndexerClassName + ".rebuildMetadataIndex",
            "rebuilds solr's metadata index");
        addCommand(com);

        com = new MCRCommand("rebuild solr content index", solrIndexerClassName + ".rebuildContentIndex", "rebuilds solr's content index");
        addCommand(com);

        com = new MCRCommand("restricted rebuild solr metadata index for objecttype {0}", solrIndexerClassName
            + ".rebuildMetadataIndex String", "rebuilds solr's metadata index for the given type in {0}");
        addCommand(com);

        com = new MCRCommand("restricted rebuild solr metadata index for selected", commandClassName + ".rebuildMetadataIndexForSelected",
            "rebuilds solr's metadata index for selected objects");
        addCommand(com);

        com = new MCRCommand("restricted rebuild solr content index for selected", commandClassName + ".rebuildContentIndexForSelected",
            "rebuilds solr's content index for selected objects and or derivates");
        addCommand(com);

        com = new MCRCommand("restricted rebuild solr metadata index for object {0}", commandClassName
            + ".rebuildMetadataIndexForObject String", "rebuilds solr's metadata index for object and all its children");
        addCommand(com);

        com = new MCRCommand("optimize solr index", solrIndexerClassName + ".optimize",
            "An optimize is like a hard commit except that it forces all of the index segments to be merged into a single segment first. "
                + "Depending on the use cases, this operation should be performed infrequently (like nightly), "
                + "if at all, since it is very expensive and involves reading and re-writing the entire index");
        addCommand(com);

        com = new MCRCommand("drop solr index", solrIndexerClassName + ".dropIndex", "Deletes an existing index from solr");
        addCommand(com);

        com = new MCRCommand("delete index part for type {0}", solrIndexerClassName + ".dropIndexByType String",
            "Deletes an existing index from solr but only for the given object type.");
        addCommand(com);

        com = new MCRCommand("delete from solr index by id {0}", solrIndexerClassName + ".deleteByIdFromSolr String",
            "Deletes an document from the index by id");
        addCommand(com);

        com = new MCRCommand("create solr metadata and content index at {0}", commandClassName + ".createIndex String",
            "create solr's metadata and content index on specific solr server");
        addCommand(com);

        com = new MCRCommand("set solr server {0}", MCRSolrServerFactory.class.getCanonicalName() + ".setSolrServer String",
            "Sets the solr new server");
        addCommand(com);
    }

    public static void rebuildMetadataIndexForSelected() {
        List<String> selectedObjects = MCRObjectCommands.getSelectedObjectIDs();
        MCRSolrIndexer.rebuildMetadataIndex(selectedObjects);
    }

    public static void rebuildContentIndexForSelected() {
        List<String> selectedObjects = MCRObjectCommands.getSelectedObjectIDs();
        MCRSolrIndexer.rebuildContentIndex(selectedObjects);
    }

    public static void rebuildMetadataIndexForObject(String id) {
        MCRObject mcrObject = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(id));
        List<MCRObject> objectList = MCRObjectUtils.getDescendantsAndSelf(mcrObject);
        List<String> idList = new ArrayList<>();
        for (MCRObject obj : objectList) {
            idList.add(obj.getId().toString());
        }
        MCRSolrIndexer.rebuildMetadataIndex(idList);
    }

    public static void createIndex(String url) throws Exception {
        SolrServer cuss = MCRSolrServerFactory.createConcurrentUpdateSolrServer(url);
        SolrServer hss = MCRSolrServerFactory.createSolrServer(url);
        MCRSolrIndexer.rebuildMetadataIndex(cuss);
        MCRSolrIndexer.rebuildContentIndex(hss);
        if (cuss instanceof ConcurrentUpdateSolrServer) {
            ((ConcurrentUpdateSolrServer) cuss).blockUntilFinished();
        }
        hss.optimize();
    }

}
