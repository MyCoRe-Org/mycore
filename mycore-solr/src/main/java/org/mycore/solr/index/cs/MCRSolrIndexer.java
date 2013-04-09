/**
 * 
 */
package org.mycore.solr.index.cs;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.content.MCRBaseContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.MCRSolrServerFactory;
import org.mycore.solr.index.cs.strategy.MCRSolrIndexStrategyManager;
import org.mycore.solr.logging.MCRSolrLogLevels;
import org.mycore.util.concurrent.ListeningPriorityExecutorService;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * @author shermann
 * @author Matthias Eichner
 */
public class MCRSolrIndexer extends MCREventHandlerBase {
    private static final Logger LOGGER = Logger.getLogger(MCRSolrIndexer.class);

    /** The Server used for indexing. */
    final static HttpSolrServer DEFAULT_SOLR_SERVER = MCRSolrServerFactory.getSolrServer();

    /** The executer service used for submitting the index requests. */
    final static ListeningExecutorService EXECUTOR_SERVICE = new ListeningPriorityExecutorService(new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS,
            new PriorityBlockingQueue<Runnable>()));

    /** Specify how many documents will be submitted to solr at a time when rebuilding the metadata index. Default is 100. */
    final static int BULK_SIZE = MCRConfiguration.instance().getInt("MCR.Module-solr.bulk.size", 100);

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
        MCRSolrIndexer.deleteByIdFromSolr(obj.getId().toString());
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
        MCRSolrIndexer.deleteByIdFromSolr(derivate.getId().toString());
    }

    synchronized protected void handleMCRBaseCreated(MCREvent evt, MCRBase objectOrDerivate) {
        long tStart = System.currentTimeMillis();
        try {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Solr: submitting data of\"" + objectOrDerivate.getId().toString() + "\" for indexing");
            }
            MCRContent content = (MCRContent) evt.get("content");
            if (content == null) {
                content = new MCRBaseContent(objectOrDerivate);
            }
            MCRSolrContentStream contentStream = new MCRSolrContentStream(objectOrDerivate.getId().toString(), content);
            MCRSolrDefaultIndexHandler indexHandler = new MCRSolrDefaultIndexHandler(contentStream);
            indexHandler.setCommitWithin(1000);
            submitIndexHandler(indexHandler, 10);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Solr: submitting data of\"" + objectOrDerivate.getId().toString() + "\" for indexing done in "
                        + (System.currentTimeMillis() - tStart) + "ms ");
            }
        } catch (Exception ex) {
            LOGGER.log(MCRSolrLogLevels.SOLR_ERROR, "Error creating transfer thread for object " + objectOrDerivate, ex);
        }
    }

    @Override
    protected void handleFileCreated(MCREvent evt, MCRFile file) {
        try {
            submitIndexHandler(getIndexHandler(file, DEFAULT_SOLR_SERVER));
        } catch (Exception ex) {
            LOGGER.log(MCRSolrLogLevels.SOLR_ERROR, "Error creating transfer thread for file " + file.toString(), ex);
        }
    }

    public static MCRSolrIndexHandler getIndexHandler(MCRFile file, SolrServer solrServer) throws IOException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Solr: submitting file \"" + file.getAbsolutePath() + " (" + file.getID() + ")\" for indexing");
        }
        if (MCRSolrIndexStrategyManager.checkFile(file)) {
            /* extract metadata with tika */
            return new MCRSolrFileIndexHandler(new MCRSolrFileContentStream(file), solrServer);
        } else {
            MCRSolrContentStream contentStream = new MCRSolrContentStream(file.getID(), new MCRJDOMContent(file.createXML()));
            return new MCRSolrDefaultIndexHandler(contentStream, solrServer);
        }
    }

    @Override
    protected void handleFileUpdated(MCREvent evt, MCRFile file) {
        this.handleFileCreated(evt, file);
    }

    @Override
    protected void handleFileDeleted(MCREvent evt, MCRFile file) {
        MCRSolrIndexer.deleteByIdFromSolr(file.getID());
    }

    /**
     * @param solrID
     * @return
     * 
     * @see {@link HttpSolrServer#deleteById(String)}
     */
    synchronized public static UpdateResponse deleteByIdFromSolr(String solrID) {
        UpdateResponse updateResponse = null;
        try {
            LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Deleting \"" + solrID + "\" from solr");
            updateResponse = DEFAULT_SOLR_SERVER.deleteById(solrID);
            DEFAULT_SOLR_SERVER.commit();
        } catch (Exception e) {
            LOGGER.log(MCRSolrLogLevels.SOLR_ERROR, "Error deleting document from solr", e);
        }
        return updateResponse;
    }

    /**
     * Rebuilds solr's metadata index.
     */
    public static void rebuildMetadataIndex() {
        rebuildMetadataIndex(MCRXMLMetadataManager.instance().listIDs());
    }

    /**
     * Rebuilds solr's metadata index.
     */
    public static void rebuildMetadataIndex(SolrServer solrServer) {
        rebuildMetadataIndex(MCRXMLMetadataManager.instance().listIDs(), solrServer);
    }

    /**
     * Rebuilds solr's metadata index only for objects of the given type.
     * 
     * @param type of the objects to index
     */
    public static void rebuildMetadataIndex(String type) {
        List<String> identfiersOfType = MCRXMLMetadataManager.instance().listIDsOfType(type);
        rebuildMetadataIndex(identfiersOfType);
    }

    public static void rebuildMetadataIndex(List<String> list) {
        rebuildMetadataIndex(list, MCRSolrServerFactory.getConcurrentSolrServer());
    }

    /**
     * Rebuilds solr's metadata index.
     * 
     * @param list list of identifiers of the objects to index
     * @param solrServer solr server to index
     */
    public static void rebuildMetadataIndex(List<String> list, SolrServer solrServer) {
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "=======================");
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Building Metadata Index");
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "=======================");

        if (list.isEmpty()) {
            LOGGER.info("Sorry, no documents to index");
            return;
        }

        StopWatch swatch = new StopWatch();
        swatch.start();
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Sending " + list.size() + " objects to solr for reindexing");

        MCRXMLMetadataManager metadataMgr = MCRXMLMetadataManager.instance();
        MCRSolrListElementStream contentStream = new MCRSolrListElementStream("MCRSolrObjs");
        List<Element> elementList = contentStream.getList();
        for (String id : list) {
            try {
                LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Submitting data of\"" + id + "\" for indexing");
                Document mcrObjXML = metadataMgr.retrieveXML(MCRObjectID.getInstance(id));
                elementList.add(mcrObjXML.getRootElement().detach());

                if (elementList.size() % BULK_SIZE == 0) {
                    MCRSolrIndexHandler indexHandler = new MCRSolrListElementIndexHandler(contentStream, solrServer);
                    submitIndexHandler(indexHandler);
                    contentStream = new MCRSolrListElementStream("MCRSolrObjs");
                    elementList = contentStream.getList();
                }
            } catch (Exception ex) {
                LOGGER.log(MCRSolrLogLevels.SOLR_ERROR, "Error creating index thread for object " + id, ex);
            }
        }
        /* index remaining docs*/
        int remaining = elementList.size();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Indexing almost done. Only " + remaining + " object(s) remaining");
        }
        if (remaining > 0) {
            MCRSolrIndexHandler indexHandler = new MCRSolrListElementIndexHandler(contentStream, solrServer);
            try {
                submitIndexHandler(indexHandler);
            } catch (Exception ex) {
                LOGGER.log(MCRSolrLogLevels.SOLR_ERROR, "Error submitting data to solr", ex);
            }
        }

        long durationInMilliSeconds = swatch.getTime();
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Submitted data of " + list.size() + " objects for indexing done in " + Math.ceil(durationInMilliSeconds / 1000)
                + " seconds (" + durationInMilliSeconds / list.size() + " ms/object)");
        try {
            // we wait until all index threads are finished 
            if (solrServer instanceof ConcurrentUpdateSolrServer) {
                ((ConcurrentUpdateSolrServer) solrServer).blockUntilFinished();
            }
            // one last commit before we are done
            solrServer.commit();
        } catch (SolrServerException e) {
            LOGGER.log(MCRSolrLogLevels.SOLR_ERROR, "Could not commit changes to index", e);
        } catch (IOException e) {
            LOGGER.log(MCRSolrLogLevels.SOLR_ERROR, "An " + e.getClass() + " occured", e);
        }
    }

    /**
     * Rebuilds solr's content index.
     */
    public static void rebuildContentIndex() {
        rebuildContentIndex(DEFAULT_SOLR_SERVER, MCRXMLMetadataManager.instance().listIDsOfType("derivate"));
    }

    public static void rebuildContentIndex(SolrServer solrServer) {
        rebuildContentIndex(solrServer, MCRXMLMetadataManager.instance().listIDsOfType("derivate"));
    }

    /**
     * Rebuilds the content index for the given mycore objects. You can mix derivates and
     * mcrobjects here. For each mcrobject all its derivates are indexed.
     * 
     * @param list containing mycore object id's
     */
    public static void rebuildContentIndex(List<String> list) {
        rebuildContentIndex(DEFAULT_SOLR_SERVER, list);
    }

    /**
     * Rebuilds solr's content index.
     */
    public static void rebuildContentIndex(SolrServer solrServer, List<String> list) {
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "======================");
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Building Content Index");
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "======================");

        if (list.isEmpty()) {
            LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "No objects to index");
            return;
        }
        long tStart = System.currentTimeMillis();

        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Sending content of files of " + list.size() + " to solr for reindexing");

        for (String id : list) {
            MCRSolrIndexHandler indexHandler = new MCRSolrFilesIndexHandler(id, solrServer);
            submitIndexHandler(indexHandler);
        }

        long tStop = System.currentTimeMillis();
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Submitted data of " + list.size() + " derivates for indexing done in " + (tStop - tStart) + "ms ("
                + ((float) (tStop - tStart) / list.size()) + " ms/derivate)");
    }

    /**
     * Submits the index handler to the executor service (execute as a thread) with priority zero.
     * 
     * @param indexHandler index handler to submit
     */
    protected static void submitIndexHandler(MCRSolrIndexHandler indexHandler) {
        submitIndexHandler(indexHandler, 0);
    }

    /**
     * Submits a index handler to the executor service (execute as a thread) with the given priority.
     * 
     * @param indexHandler index handler to submit
     * @param priority priority
     */
    protected static void submitIndexHandler(MCRSolrIndexHandler indexHandler, int priority) {
        ListenableFuture<List<MCRSolrIndexHandler>> future = EXECUTOR_SERVICE.submit(new MCRSolrIndexTask(indexHandler, priority));
        Futures.addCallback(future, new FutureIndexHandlerCallback());
    }

    /**
     * Callback to handle a IndexHandlers future non blocking. 
     */
    private static class FutureIndexHandlerCallback implements FutureCallback<List<MCRSolrIndexHandler>> {
        @Override
        public void onFailure(Throwable t) {
            LOGGER.log(MCRSolrLogLevels.SOLR_ERROR, "unable to submit tasks", t);
        }

        @Override
        public void onSuccess(List<MCRSolrIndexHandler> indexHandlers) {
            for (MCRSolrIndexHandler subHandler : indexHandlers) {
                try {
                    submitIndexHandler(subHandler);
                } catch (Exception exc) {
                    LOGGER.log(MCRSolrLogLevels.SOLR_ERROR, "unable to submit tasks", exc);
                }
            }
        }
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
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Dropping solr index...");
        DEFAULT_SOLR_SERVER.deleteByQuery("*:*");
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Dropping solr index...done");
    }

    /**
     * @param type
     * @throws Exception
     */
    public static void dropIndexByType(String type) throws Exception {
        if (!MCRObjectID.isValidType(type) || "data_file".equals(type)) {
            LOGGER.warn("The type " + type + " is not a valid type in the actual environment");
            return;
        }

        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Dropping solr index for type " + type + "...");
        DEFAULT_SOLR_SERVER.deleteByQuery("+objectType:" + type);
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Dropping solr index for type " + type + "...done");
    }

    /**
     * Sends a signal to the remote solr server to optimize its index. 
     */
    public static void optimize() {
        try {
            LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Sending optimize request to solr");
            DEFAULT_SOLR_SERVER.optimize();
        } catch (Exception ex) {
            LOGGER.log(MCRSolrLogLevels.SOLR_ERROR, "Could not optimize solr index", ex);
        }
    }

}
