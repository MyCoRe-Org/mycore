/**
 * 
 */
package org.mycore.solr.index.cs;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.mycore.common.MCRUtils;
import org.mycore.common.content.MCRBaseContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.events.MCREvent;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSearcher;
import org.mycore.services.fieldquery.MCRSortBy;
import org.mycore.solr.MCRSolrServerFactory;
import org.mycore.solr.legacy.MCRLuceneSolrAdapter;

/**
 * @author shermann
 *
 */
public class MCRSolrIndexer extends MCRSearcher {
    private static final Logger LOGGER = Logger.getLogger(MCRSolrIndexer.class);

    static HttpSolrServer solrServer = null;

    static ExecutorService executorService = null;

    static {
        solrServer = MCRSolrServerFactory.getSolrServer();
        executorService = Executors.newFixedThreadPool(10);
    }

    @Override
    public boolean isIndexer() {
        return true;
    }

    @Override
    synchronized protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        this.handleMCRBaseCreated(evt, obj);
    }

    @Override
    synchronized protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        this.handleMCRBaseCreated(evt, obj);

    }

    @Override
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        this.handleMCRBaseCreated(evt, obj);
    }

    @Override
    synchronized protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        this.deleteByIdFromSolr(obj.getId().toString());
    }

    @Override
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate derivate) {
        this.handleMCRBaseCreated(evt, derivate);
    }

    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate derivate) {
        this.handleMCRBaseCreated(evt, derivate);
    }

    @Override
    protected void handleDerivateRepaired(MCREvent evt, MCRDerivate derivate) {
        this.handleMCRBaseCreated(evt, derivate);
    }

    @Override
    protected void handleDerivateDeleted(MCREvent evt, MCRDerivate derivate) {
        this.deleteByIdFromSolr(derivate.getId().toString());
    }

    synchronized protected void handleMCRBaseCreated(MCREvent evt, MCRBase objectOrDerivate) {
        long tStart = System.currentTimeMillis();
        try {
            LOGGER.trace("Solr: submitting data of\"" + objectOrDerivate.getId().toString() + "\" for indexing");
            MCRContent content = (MCRContent) evt.get("content");
            if (content == null) {
                content = new MCRBaseContent(objectOrDerivate);
            }
            MCRBaseContentStream contentStream = new MCRBaseContentStream(objectOrDerivate.getId().toString(), content);
            executorService.submit(contentStream);
            LOGGER.trace("Solr: submitting data of\"" + objectOrDerivate.getId().toString() + "\" for indexing done in "
                + (System.currentTimeMillis() - tStart) + "ms ");
        } catch (Exception ex) {
            LOGGER.error("Error creating transfer thread", ex);
        }
    }

    @Override
    protected void handleFileCreated(MCREvent evt, MCRFile file) {
        MCRFileContentStream contentStream = null;
        try {
            LOGGER.trace("Solr: submitting file \"" + file.getAbsolutePath() + " (" + file.getID() + ")\" for indexing");
            /* extract metadata with tika */
            contentStream = new MCRFileContentStream(file);
            executorService.submit(contentStream);
        } catch (Exception ex) {
            LOGGER.error("Error creating transfer thread", ex);
        }
    }

    @Override
    protected void handleFileUpdated(MCREvent evt, MCRFile file) {
        this.handleFileCreated(evt, file);
    }

    @Override
    protected void handleFileDeleted(MCREvent evt, MCRFile file) {
        this.deleteByIdFromSolr(file.getID());
    }

    /**
     * @param solrID
     * @return
     */
    protected UpdateResponse deleteByIdFromSolr(String solrID) {
        UpdateResponse updateResponse = null;
        try {
            LOGGER.info("Solr: deleting \"" + solrID + "\" from solr");
            updateResponse = solrServer.deleteById(solrID);
            solrServer.commit();
        } catch (Exception e) {
            LOGGER.error("Error deleting document from solr", e);
        }
        return updateResponse;
    }

    /**
     * Rebuilds solr's metadata index.
     */
    public static void rebuildMetadataIndex() {
        LOGGER.info("=======================");
        LOGGER.info("Building Metadata Index");
        LOGGER.info("=======================");

        List<String> list = MCRXMLMetadataManager.instance().listIDs();

        if (list.size() == 0) {
            LOGGER.info("No documents to index");
            return;
        }

        long tStart = System.currentTimeMillis();
        LOGGER.info("Solr: sending " + list.size() + " objects to solr for reindexing");
//        MCRXMLMetadataManager metadataMgr = MCRXMLMetadataManager.instance();
        MCRXMLContentSolrStream contentSolrStream = new MCRXMLContentSolrStream();
        for (String id : list) {
            try {
                LOGGER.info("Solr: submitting data of\"" + id + "\" for indexing");
//                MCRBaseContentStream contentStream = new MCRBaseContentStream(id, metadataMgr.retrieveContent(MCRObjectID.getInstance(id)));
//                executorService.submit(contentStream);
                contentSolrStream.addMCRObj(id);
            } catch (Exception ex) {
                LOGGER.error("Error creating transfer thread", ex);
            }
        }
        long tStop = System.currentTimeMillis();
        LOGGER.info("Solr: submitted data of " + list.size() + " objects for indexing done in " + (tStop - tStart) + "ms ("
            + ((float) (tStop - tStart) / list.size()) + " ms/object)");
    }

    /**
     * Rebuilds solr's content index.
     */
    public static void rebuildContentIndex() {
        LOGGER.info("======================");
        LOGGER.info("Building Content Index");
        LOGGER.info("======================");

        List<String> list = MCRXMLMetadataManager.instance().listIDsOfType("derivate");
        if (list.size() == 0) {
            LOGGER.info("No derivates to index");
            return;
        }

        long tStart = System.currentTimeMillis();

        LOGGER.info("Solr: sending content of files of " + list.size() + " derivates to solr for reindexing");
        for (String derivate : list) {

            List<MCRFile> files = MCRUtils.getFiles(derivate);
            LOGGER.info("Sending files (" + files.size() + ") for derivate \"" + derivate + "\"");
            MCRFileContentStream contentStream = null;
            for (MCRFile file : files) {
                try {
                    LOGGER.trace("Solr: submitting file \"" + file.getAbsolutePath() + " (" + file.getID() + ")\" for indexing");
                    contentStream = new MCRFileContentStream(file);
                    executorService.submit(contentStream);
                } catch (Exception ex) {
                    LOGGER.error("Error creating transfer thread", ex);
                }
            }
        }

        long tStop = System.currentTimeMillis();
        LOGGER.info("Solr: submitted data of " + list.size() + " derivates for indexing done in " + (tStop - tStart) + "ms ("
            + ((float) (tStop - tStart) / list.size()) + " ms/derivate)");
    }

    /**
     * Rebuilds and optimizes solr's metadata and content index. 
     */
    public static void rebuildMetadataAndContentIndex() throws Exception {
        MCRSolrIndexer.rebuildMetadataIndex();
        MCRSolrIndexer.rebuildContentIndex();
        MCRSolrIndexer.optimize();
    }

    /**
     * Drops the current solr index.
     */
    public static void dropIndex() throws Exception {
        LOGGER.info("Dropping solr index...");
        MCRSolrServerFactory.getSolrServer().deleteByQuery("*:*");
        LOGGER.info("Dropping solr index...done");
    }

    /**
     * Sends a signal to the remote solr server to optimize its index. 
     */
    public static void optimize() {
        try {
            LOGGER.info("Sending optimize request to solr");
            MCRSolrServerFactory.getSolrServer().optimize();
        } catch (Exception ex) {
            LOGGER.error("Could not optimize solr index", ex);
        }
    }

    /**
     * Handles legacy lucene searches.
     * */
    @SuppressWarnings("rawtypes")
    public MCRResults search(MCRCondition condition, int maxResults, List<MCRSortBy> sortBy, boolean addSortData) {
        LOGGER.warn("Processing legacy query \"" + condition.toString() + "\"");
        MCRResults result = MCRLuceneSolrAdapter.search(condition, maxResults, sortBy, addSortData);
        return result;
    }
}
