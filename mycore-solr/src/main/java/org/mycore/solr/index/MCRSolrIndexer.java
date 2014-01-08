/**
 * 
 */
package org.mycore.solr.index;

import static org.mycore.solr.MCRSolrConstants.CONFIG_PREFIX;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRShutdownHandler.Closeable;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.MCRSolrServerFactory;
import org.mycore.solr.index.handlers.MCRSolrIndexHandlerFactory;
import org.mycore.solr.index.handlers.MCRSolrOptimizeIndexHandler;
import org.mycore.solr.index.handlers.stream.MCRSolrFilesIndexHandler;
import org.mycore.solr.index.statistic.MCRSolrIndexStatistic;
import org.mycore.solr.index.statistic.MCRSolrIndexStatisticCollector;
import org.mycore.solr.search.MCRSolrSearchUtils;
import org.mycore.util.concurrent.MCRListeningPriorityExecutorService;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * @author shermann
 * @author Matthias Eichner
 */
public class MCRSolrIndexer {
    private static final Logger LOGGER = Logger.getLogger(MCRSolrIndexer.class);

    /** The executer service used for submitting the index requests. */
    final static ListeningExecutorService EXECUTOR_SERVICE;

    private final static FutureIndexHandlerCounter FUTURE_COUNTER;

    /** Specify how many documents will be submitted to solr at a time when rebuilding the metadata index. Default is 100. */
    final static int BULK_SIZE = MCRConfiguration.instance().getInt(CONFIG_PREFIX + "Indexer.BulkSize", 100);

    private static final int BATCH_AUTO_COMMIT_WITHIN_MS = 60000;

    static {
        int poolSize = MCRConfiguration.instance().getInt(CONFIG_PREFIX + "Indexer.ThreadCount", 4);
        final MCRListeningPriorityExecutorService executorService = new MCRListeningPriorityExecutorService(new ThreadPoolExecutor(
                poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<Runnable>()));
        Runnable onShutdown = new Runnable() {

            @Override
            public void run() {
                executorService.shutdown();
                String documentStats = MessageFormat.format("Solr documents: {0}, each: {1} ms.",
                        MCRSolrIndexStatisticCollector.documents.getDocuments(), MCRSolrIndexStatisticCollector.documents.reset());
                String metadataStats = MessageFormat.format("XML documents: {0}, each: {1} ms.",
                        MCRSolrIndexStatisticCollector.xml.getDocuments(), MCRSolrIndexStatisticCollector.xml.reset());
                String fileStats = MessageFormat.format("File transfers: {0}, each: {1} ms.",
                        MCRSolrIndexStatisticCollector.fileTransfer.getDocuments(), MCRSolrIndexStatisticCollector.fileTransfer.reset());
                String operationsStats = MessageFormat.format("Other index operations: {0}, each: {1} ms.",
                        MCRSolrIndexStatisticCollector.operations.getDocuments(), MCRSolrIndexStatisticCollector.operations.reset());
                String msg = MessageFormat.format("\nFinal statistics:\n{0}\n{1}\n{2}\n{3}", documentStats, metadataStats, fileStats,
                        operationsStats);
                LOGGER.info(msg);
                try {
                    MCRSolrServerFactory.getSolrServer().commit();
                } catch (SolrServerException | IOException e) {
                    LOGGER.warn("Error while closing MCRSolrIndexer executor service.", e);
                }
            }
        };
        FUTURE_COUNTER = new FutureIndexHandlerCounter(onShutdown);
        EXECUTOR_SERVICE = executorService;
    }

    /**
     * @param solrID
     * @return
     * 
     * @see {@link HttpSolrServer#deleteById(String)}
     */
    synchronized public static UpdateResponse deleteByIdFromSolr(String solrID) {
        SolrServer solrServer = MCRSolrServerFactory.getSolrServer();
        UpdateResponse updateResponse = null;
        long start = System.currentTimeMillis();
        try {
            LOGGER.info("Deleting \"" + solrID + "\" from solr");
            updateResponse = solrServer.deleteById(solrID);
            solrServer.commit();
        } catch (Exception e) {
            LOGGER.error("Error deleting document from solr", e);
        }
        long end = System.currentTimeMillis();
        MCRSolrIndexStatistic operations = MCRSolrIndexStatisticCollector.operations;
        operations.addDocument(1);
        operations.addTime(end - start);
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
        LOGGER.info("Re-building Metadata Index");
        if (list.isEmpty()) {
            LOGGER.info("Sorry, no documents to index");
            return;
        }

        StopWatch swatch = new StopWatch();
        swatch.start();
        int totalCount = list.size();
        LOGGER.info("Sending " + totalCount + " objects to solr for reindexing");

        MCRXMLMetadataManager metadataMgr = MCRXMLMetadataManager.instance();
        HashMap<MCRObjectID, MCRContent> contentMap = new HashMap<>((int) (BULK_SIZE * 1.4));
        MCRSolrIndexStatistic statistic = null;
        int i = 0;
        for (String id : list) {
            i++;
            try {
                LOGGER.debug("Preparing \"" + id + "\" for indexing");
                MCRObjectID objId = MCRObjectID.getInstance(id);
                MCRContent content = metadataMgr.retrieveContent(objId);
                contentMap.put(objId, content);
                if (i % BULK_SIZE == 0 || totalCount == i) {
                    MCRSolrIndexHandler indexHandler = MCRSolrIndexHandlerFactory.getInstance().getIndexHandler(contentMap);
                    indexHandler.setCommitWithin(BATCH_AUTO_COMMIT_WITHIN_MS);
                    indexHandler.setSolrServer(solrServer);
                    statistic = indexHandler.getStatistic();
                    submitIndexHandler(indexHandler);
                    contentMap.clear();
                }
            } catch (Exception ex) {
                LOGGER.error("Error creating index thread for object " + id, ex);
            }
        }
        long durationInMilliSeconds = swatch.getTime();
        if (statistic != null) {
            statistic.addTime(durationInMilliSeconds);
        }
    }

    /**
     * Rebuilds solr's content index.
     */
    public static void rebuildContentIndex() {
        rebuildContentIndex(MCRSolrServerFactory.getSolrServer(), MCRXMLMetadataManager.instance().listIDsOfType("derivate"));
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
        rebuildContentIndex(MCRSolrServerFactory.getSolrServer(), list);
    }

    /**
     * Rebuilds solr's content index.
     */
    public static void rebuildContentIndex(SolrServer solrServer, List<String> list) {
        LOGGER.info("Re-building Content Index");

        if (list.isEmpty()) {
            LOGGER.info("No objects to index");
            return;
        }
        long tStart = System.currentTimeMillis();

        int totalCount = list.size();
        LOGGER.info("Sending content of " + totalCount + " derivates to solr for reindexing");

        for (String id : list) {
            MCRSolrFilesIndexHandler indexHandler = new MCRSolrFilesIndexHandler(id, solrServer);
            indexHandler.setCommitWithin(BATCH_AUTO_COMMIT_WITHIN_MS);
            submitIndexHandler(indexHandler);
        }

        long tStop = System.currentTimeMillis();
        MCRSolrIndexStatisticCollector.fileTransfer.addTime(tStop - tStart);
    }

    /**
     * Submits the index handler to the executor service (execute as a thread) with priority zero.
     * 
     * @param indexHandler index handler to submit
     */
    public static void submitIndexHandler(MCRSolrIndexHandler indexHandler) {
        submitIndexHandler(indexHandler, 0);
    }

    /**
     * Submits a index handler to the executor service (execute as a thread) with the given priority.
     * 
     * @param indexHandler index handler to submit
     * @param priority priority
     */
    public static void submitIndexHandler(MCRSolrIndexHandler indexHandler, int priority) {
        ListenableFuture<List<MCRSolrIndexHandler>> future = EXECUTOR_SERVICE.submit(new MCRSolrIndexTask(indexHandler, priority));
        Futures.addCallback(future, new FutureIndexHandlerCallback());
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
     * @param type
     * @throws Exception
     */
    public static void dropIndexByType(String type) throws Exception {
        if (!MCRObjectID.isValidType(type) || "data_file".equals(type)) {
            LOGGER.warn("The type " + type + " is not a valid type in the actual environment");
            return;
        }

        LOGGER.info("Dropping solr index for type " + type + "...");
        MCRSolrServerFactory.getSolrServer().deleteByQuery("+objectType:" + type);
        LOGGER.info("Dropping solr index for type " + type + "...done");
    }

    /**
     * Sends a signal to the remote solr server to optimize its index. 
     */
    public static void optimize() {
        try {
            MCRSolrOptimizeIndexHandler indexHandler = new MCRSolrOptimizeIndexHandler();
            indexHandler.setCommitWithin(BATCH_AUTO_COMMIT_WITHIN_MS);
            submitIndexHandler(indexHandler);
        } catch (Exception ex) {
            LOGGER.error("Could not optimize solr index", ex);
        }
    }

    /**
     * Synchronizes the solr server with the database. As a result the
     * solr server contains the same documents as the database. All solr
     * zombie documents will be removed, and all not indexed mycore
     * objects will be indexed.
     * 
     * @throws IOException
     * @throws SolrServerException
     */
    public static void synchronizeMetadataIndex() throws IOException, SolrServerException {
        Collection<String> objectTypes = MCRXMLMetadataManager.instance().listObjectTypes();
        for (String objectType : objectTypes) {
            synchronizeMetadataIndex(objectType);
        }
    }

    /**
     * Synchronizes the solr server with the mycore store for a given object type.
     * As a result the solr server contains the same documents as the store.
     * All solr zombie documents will be removed, and all not indexed mycore
     * objects will be indexed.
     * 
     * @throws IOException
     * @throws SolrServerException
     */
    public static void synchronizeMetadataIndex(String objectType) throws IOException, SolrServerException {
        LOGGER.info("synchronize " + objectType);
        // get ids from store
        LOGGER.info("fetching mycore store...");
        List<String> storeList = MCRXMLMetadataManager.instance().listIDsOfType(objectType);
        LOGGER.info("there are " + storeList.size() + " mycore objects");
        // get ids from solr
        LOGGER.info("fetching solr...");
        SolrServer solrServer = MCRSolrServerFactory.getSolrServer();
        List<String> solrList = MCRSolrSearchUtils.listIDs(solrServer, "objectType:" + objectType);
        LOGGER.info("there are " + solrList.size() + " solr objects");

        // documents to remove
        List<String> toRemove = new ArrayList<>(1000);
        for (String id : solrList) {
            if (!storeList.contains(id)) {
                toRemove.add(id);
            }
        }
        if (!toRemove.isEmpty()) {
            LOGGER.info("remove " + toRemove.size() + " zombie objects from solr");
            solrServer.deleteById(toRemove, 5000);
        }
        // documents to add
        storeList.removeAll(solrList);
        if (!storeList.isEmpty()) {
            LOGGER.info("index " + storeList.size() + " mycore objects");
            rebuildMetadataIndex(storeList);
        }
    }

    /**
     * Callback to handle a IndexHandlers future non blocking. 
     */
    private static class FutureIndexHandlerCallback implements FutureCallback<List<MCRSolrIndexHandler>> {
        public FutureIndexHandlerCallback() {
            FUTURE_COUNTER.add();
        }

        @Override
        public void onFailure(Throwable t) {
            LOGGER.error("unable to submit tasks", t);
            FUTURE_COUNTER.remove();
        }

        @Override
        public void onSuccess(List<MCRSolrIndexHandler> indexHandlers) {
            try {
                for (MCRSolrIndexHandler subHandler : indexHandlers) {
                    try {
                        submitIndexHandler(subHandler);
                    } catch (Exception exc) {
                        LOGGER.error("unable to submit tasks", exc);
                    }
                }
            } finally {
                FUTURE_COUNTER.remove();
            }
        }

    }

    private static class FutureIndexHandlerCounter implements Closeable {
        AtomicLong awaitingEvents = new AtomicLong();

        Runnable onShutdown;

        public FutureIndexHandlerCounter(Runnable onShutdown) {
            this.onShutdown = onShutdown;
            MCRShutdownHandler.getInstance().addCloseable(this);
        }

        @Override
        public void prepareClose() {
        }

        @Override
        public void close() {
            while (awaitingEvents.get() != 0) {
                LOGGER.info("Waiting for " + awaitingEvents.get() + " task to complete.");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.yield();
                }
            }
            if (onShutdown != null) {
                onShutdown.run();
            }
        }

        @Override
        public int getPriority() {
            return Closeable.DEFAULT_PRIORITY;
        }

        public void add() {
            awaitingEvents.incrementAndGet();
        }

        public void remove() {
            awaitingEvents.decrementAndGet();
        }

    }

}
