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

package org.mycore.solr.index;

import static org.mycore.solr.MCRSolrConstants.SOLR_CONFIG_PREFIX;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRShutdownHandler.Closeable;
import org.mycore.common.processing.MCRProcessableDefaultCollection;
import org.mycore.common.processing.MCRProcessableManager;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.MCRSolrCore;
import org.mycore.solr.MCRSolrUtils;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;
import org.mycore.solr.index.handlers.MCRSolrAbstractIndexHandler;
import org.mycore.solr.index.handlers.MCRSolrIndexHandlerFactory;
import org.mycore.solr.index.handlers.MCRSolrOptimizeIndexHandler;
import org.mycore.solr.index.handlers.stream.MCRSolrFilesIndexHandler;
import org.mycore.solr.index.statistic.MCRSolrIndexStatistic;
import org.mycore.solr.index.statistic.MCRSolrIndexStatisticCollector;
import org.mycore.solr.search.MCRSolrSearchUtils;
import org.mycore.util.concurrent.MCRFixedUserCallable;
import org.mycore.util.concurrent.processing.MCRProcessableExecutor;
import org.mycore.util.concurrent.processing.MCRProcessableFactory;
import org.mycore.util.concurrent.processing.MCRProcessableSupplier;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Base class for indexing with solr.
 *
 * @author shermann
 * @author Matthias Eichner
 */
public class MCRSolrIndexer {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final int LOW_PRIORITY = 0;

    public static final int HIGH_PRIORITY = 10;

    /**
     * Specify how many documents will be submitted to solr at a time when rebuilding the metadata index. Default is
     * 100.
     */
    static final int BULK_SIZE = MCRConfiguration2.getInt(SOLR_CONFIG_PREFIX + "Indexer.BulkSize").orElse(100);

    static final MCRProcessableExecutor SOLR_EXECUTOR;

    static final ExecutorService SOLR_SUB_EXECUTOR;

    static final MCRProcessableDefaultCollection SOLR_COLLECTION;

    private static final int BATCH_AUTO_COMMIT_WITHIN_MS = 60_000;

    public static final MCRSolrAuthenticationManager SOLR_AUTHENTICATION_MANAGER =
        MCRSolrAuthenticationManager.obtainInstance();

    static {
        int poolSize = MCRConfiguration2.getInt(SOLR_CONFIG_PREFIX + "Indexer.ThreadCount").orElse(4);
        final ExecutorService threadPool = new ThreadPoolExecutor(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS,
            MCRProcessableFactory.newPriorityBlockingQueue(),
            new ThreadFactoryBuilder().setNameFormat("SOLR-Indexer-#%d").build());
        SOLR_COLLECTION = new MCRProcessableDefaultCollection("Solr Indexer");
        SOLR_COLLECTION.setProperty("pool size (threads)", poolSize);
        SOLR_COLLECTION.setProperty("bulk size", BULK_SIZE);
        SOLR_COLLECTION.setProperty("commit within (ms)", BATCH_AUTO_COMMIT_WITHIN_MS);
        MCRProcessableManager.getInstance().getRegistry().register(SOLR_COLLECTION);
        SOLR_EXECUTOR = MCRProcessableFactory.newPool(threadPool, SOLR_COLLECTION);

        int poolSize2 = Math.max(1, poolSize / 2);
        SOLR_SUB_EXECUTOR = new ThreadPoolExecutor(poolSize2, poolSize2, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), new ThreadFactoryBuilder().setNameFormat("SOLR-Sub-Handler-#%d").build());

        MCRShutdownHandler.getInstance().addCloseable(new Closeable() {

            @Override
            public void prepareClose() {
                while (SOLR_COLLECTION.stream().findAny().isPresent()) {
                    Thread.yield(); //wait for index handler
                }
            }

            @Override
            public int getPriority() {
                return Integer.MIN_VALUE + 6;
            }

            @Override
            public void close() {
                //MCR-2349 close after MCRSolrIndexEventHandler
                SOLR_EXECUTOR.submit(SOLR_EXECUTOR.getExecutor()::shutdown,
                    Integer.MIN_VALUE)
                    .getFuture()
                    .join();
                waitForShutdown(SOLR_EXECUTOR.getExecutor());
                String documentStats = new MessageFormat("Solr documents: {0}, each: {1} ms.", Locale.ROOT).format(
                    new Object[] { MCRSolrIndexStatisticCollector.DOCUMENTS.getDocuments(),
                        MCRSolrIndexStatisticCollector.DOCUMENTS.reset() });
                String metadataStats = new MessageFormat("XML documents: {0}, each: {1} ms.", Locale.ROOT).format(
                    new Object[] { MCRSolrIndexStatisticCollector.XML.getDocuments(),
                        MCRSolrIndexStatisticCollector.XML.reset() });
                String fileStats = new MessageFormat("File transfers: {0}, each: {1} ms.", Locale.ROOT).format(
                    new Object[] { MCRSolrIndexStatisticCollector.FILE_TRANSFER.getDocuments(),
                        MCRSolrIndexStatisticCollector.FILE_TRANSFER.reset() });
                String operationsStats = new MessageFormat("Other index operations: {0}, each: {1} ms.", Locale.ROOT)
                    .format(new Object[] { MCRSolrIndexStatisticCollector.OPERATIONS.getDocuments(),
                        MCRSolrIndexStatisticCollector.OPERATIONS.reset() });
                String msg = new MessageFormat("\nFinal statistics:\n{0}\n{1}\n{2}\n{3}", Locale.ROOT)
                    .format(new Object[] { documentStats, metadataStats, fileStats, operationsStats });
                LOGGER.info(msg);
            }

            private void waitForShutdown(ExecutorService service) {
                if (!service.isTerminated()) {
                    try {
                        LOGGER.info("Waiting for shutdown of SOLR Indexer.");
                        service.awaitTermination(10, TimeUnit.MINUTES);
                        LOGGER.info("SOLR Indexer was shut down.");
                    } catch (InterruptedException e) {
                        LOGGER.warn("Error while waiting for shutdown.", e);
                    }
                }
            }
        });
        MCRShutdownHandler.getInstance().addCloseable(new Closeable() {

            @Override
            public void prepareClose() {
                SOLR_SUB_EXECUTOR.shutdown();
            }

            @Override
            public int getPriority() {
                return Integer.MIN_VALUE + 5;
            }

            @Override
            public void close() {
                waitForShutdown(SOLR_SUB_EXECUTOR);
            }

            private void waitForShutdown(ExecutorService service) {
                if (!service.isTerminated()) {
                    try {
                        service.awaitTermination(10, TimeUnit.MINUTES);
                    } catch (InterruptedException e) {
                        LOGGER.warn("Error while waiting for shutdown.", e);
                    }
                }
            }
        });
    }

    /**
     * Deletes nested orphaned nested documents.
     *
     * <a href="https://issues.apache.org/jira/browse/SOLR-6357">SOLR-6357</a>
     *
     * @throws SolrServerException solr server exception
     * @throws IOException io exception
     */
    public static void deleteOrphanedNestedDocuments(List<MCRSolrCore> solrCores)
        throws SolrServerException, IOException {
        if (!MCRSolrUtils.useNestedDocuments()) {
            return;
        }
        UpdateRequest req = new UpdateRequest();
        req.deleteByQuery("-({!join from=id to=_root_ score=none}_root_:*) +_root_:*");
        req.setCommitWithin(0);
        SOLR_AUTHENTICATION_MANAGER.applyAuthentication(req, MCRSolrAuthenticationLevel.INDEX);
        for (MCRSolrCore solrCore : solrCores) {
            req.process(solrCore.getClient());
        }
    }

    /**
     * Deletes a list of documents by unique ID. Also removes any nested document of that ID.
     *
     * @param solrIDs
     *            the list of solr document IDs to delete
     */
    public static UpdateResponse deleteById(SolrClient client, String... solrIDs) {
        if (solrIDs == null || solrIDs.length == 0) {
            return null;
        }

        UpdateResponse updateResponse = null;
        long start = System.currentTimeMillis();
        try {
            LOGGER.debug("Deleting \"{}\" from solr", () -> Arrays.asList(solrIDs));
            UpdateRequest req = new UpdateRequest();
            SOLR_AUTHENTICATION_MANAGER.applyAuthentication(req,
                MCRSolrAuthenticationLevel.INDEX);
            //delete all documents rooted at this id
            if (MCRSolrUtils.useNestedDocuments()) {
                StringBuilder deleteQuery = new StringBuilder("_root_:(");
                for (String solrID : solrIDs) {
                    deleteQuery.append('"');
                    deleteQuery.append(MCRSolrUtils.escapeSearchValue(solrID));
                    deleteQuery.append("\" ");
                }
                deleteQuery.setCharAt(deleteQuery.length() - 1, ')');
                req.deleteByQuery(deleteQuery.toString());
            }
            //for document without nested
            req.deleteById(Arrays.asList(solrIDs));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Delete request: {}", req.getXML());
            }
            updateResponse = req.process(client);

            commit(client);
        } catch (Exception e) {
            LOGGER.error("Error deleting document from solr", e);
        }
        long end = System.currentTimeMillis();
        MCRSolrIndexStatistic operations = MCRSolrIndexStatisticCollector.OPERATIONS;
        operations.addDocument(1);
        operations.addTime(end - start);
        return updateResponse;

    }

    /**
     * Convenient method to delete a derivate and all its files at once.
     *
     * @param id the derivate id
     * @return the solr response
     */
    public static UpdateResponse deleteDerivate(SolrClient solrClient, String id) {
        if (id == null) {
            return null;
        }
        UpdateResponse updateResponse = null;
        long start = System.currentTimeMillis();
        try {
            LOGGER.debug("Deleting derivate \"{}\" from solr", id);
            UpdateRequest req = new UpdateRequest();
            SOLR_AUTHENTICATION_MANAGER.applyAuthentication(req,
                MCRSolrAuthenticationLevel.INDEX);
            StringBuilder deleteQuery = new StringBuilder();
            deleteQuery.append("id:").append(id).append(' ');
            deleteQuery.append("derivateID:").append(id);
            if (MCRSolrUtils.useNestedDocuments()) {
                deleteQuery.append(' ').append("_root_:").append(id);
            }
            req.deleteByQuery(deleteQuery.toString());
            updateResponse = req.process(solrClient);

            commit(solrClient);
        } catch (Exception e) {
            LOGGER.error("Error deleting document from solr", e);
        }
        long end = System.currentTimeMillis();
        MCRSolrIndexStatistic operations = MCRSolrIndexStatisticCollector.OPERATIONS;
        operations.addDocument(1);
        operations.addTime(end - start);
        return updateResponse;
    }

    private static void commit(SolrClient solrClient) throws SolrServerException, IOException {
        UpdateRequest commitRequest = new UpdateRequest();
        commitRequest.setAction(UpdateRequest.ACTION.COMMIT, true, true);
        SOLR_AUTHENTICATION_MANAGER.applyAuthentication(commitRequest,
            MCRSolrAuthenticationLevel.INDEX);
        commitRequest.process(solrClient);
    }

    /**
     * Checks if the application uses nested documents. Using nested documents requires
     * additional queries and slows performance.
     *
     * @return true if nested documents are used, otherwise false
     */
    protected static boolean useNestedDocuments() {
        return MCRConfiguration2.getBoolean(SOLR_CONFIG_PREFIX + "NestedDocuments").orElse(true);
    }

    /**
     * Rebuilds solr's metadata index.
     */
    public static void rebuildMetadataIndex(List<MCRSolrCore> solrCores) {
        rebuildMetadataIndex(MCRXMLMetadataManager.getInstance().listIDs(), solrCores);
    }

    /**
     * Rebuilds solr's metadata index only for objects of the given type.
     *
     * @param type
     *            of the objects to index
     */
    public static void rebuildMetadataIndex(String type, List<MCRSolrCore> solrCores) {
        List<String> identfiersOfType = MCRXMLMetadataManager.getInstance().listIDsOfType(type);
        rebuildMetadataIndex(identfiersOfType, solrCores);
    }

    /**
     * Rebuilds solr's metadata index only for objects of the given base.
     *
     * @param base
     *            of the objects to index
     */
    public static void rebuildMetadataIndexForObjectBase(String base, List<MCRSolrCore> solrCores) {
        List<String> identfiersOfBase = MCRXMLMetadataManager.getInstance().listIDsForBase(base);
        rebuildMetadataIndex(identfiersOfBase, solrCores);
    }

    /**
     * Rebuilds solr's metadata index.
     *
     * @param list
     *            list of identifiers of the objects to index
     * @param solrCores
     *            solr server to index
     */
    public static void rebuildMetadataIndex(List<String> list, List<MCRSolrCore> solrCores) {
        LOGGER.info("Re-building Metadata Index");
        if (list.isEmpty()) {
            LOGGER.info("Sorry, no documents to index");
            return;
        }

        StopWatch swatch = new StopWatch();
        swatch.start();
        int totalCount = list.size();
        LOGGER.info("Sending {} objects to solr for reindexing", totalCount);

        MCRXMLMetadataManager metadataMgr = MCRXMLMetadataManager.getInstance();
        MCRSolrIndexStatistic statistic = null;
        Map<MCRObjectID, MCRContent> contentMap = new HashMap<>((int) (BULK_SIZE * 1.4));
        int i = 0;
        for (String id : list) {
            i++;
            try {
                LOGGER.debug("Preparing \"{}\" for indexing", id);
                MCRObjectID objId = MCRObjectID.getInstance(id);
                MCRContent content = metadataMgr.retrieveContent(objId);
                contentMap.put(objId, content);
                if (i % BULK_SIZE == 0 || totalCount == i) {
                    MCRSolrIndexHandler indexHandler = MCRSolrIndexHandlerFactory.obtainInstance()
                        .getIndexHandler(contentMap);
                    if (indexHandler instanceof MCRSolrAbstractIndexHandler aie) {
                        aie.setDestinationCores(solrCores);
                    } else {
                        throw new MCRException("indexHandler " + indexHandler.getClass().getName()
                            + " does not support indexing with cores");
                    }
                    indexHandler.setCommitWithin(BATCH_AUTO_COMMIT_WITHIN_MS);
                    statistic = indexHandler.getStatistic();
                    submitIndexHandler(indexHandler);
                    contentMap = new HashMap<>((int) (BULK_SIZE * 1.4));
                }
            } catch (Exception ex) {
                LOGGER.error("Error creating index thread for object {}", id, ex);
            }
        }
        long durationInMilliSeconds = swatch.getDuration().toMillis();
        if (statistic != null) {
            statistic.addTime(durationInMilliSeconds);
        }
    }

    /**
     * Rebuilds solr's content index.
     */
    public static void rebuildContentIndex(List<MCRSolrCore> cores) {
        rebuildContentIndex(MCRXMLMetadataManager.getInstance().listIDsOfType(MCRDerivate.OBJECT_TYPE), cores);
    }

    /**
     * Rebuilds solr's content index.
     *
     * @param cores
     *            the solr cores to send the objects to
     * @param list
     *            list of mycore object id's
     */
    public static void rebuildContentIndex(List<String> list, List<MCRSolrCore> cores) {
        rebuildContentIndex(list, cores, LOW_PRIORITY);
    }

    /**
     * Rebuilds solr's content index.
     *
     * @param cores
     *            the solr cores to send the objects to
     * @param list
     *            list of mycore object id's
     * @param priority
     *            higher priority means earlier execution
     */
    public static void rebuildContentIndex(List<String> list, List<MCRSolrCore> cores, int priority) {
        LOGGER.info("Re-building Content Index");

        if (list.isEmpty()) {
            LOGGER.info("No objects to index");
            return;
        }
        long tStart = System.currentTimeMillis();
        int totalCount = list.size();
        LOGGER.info("Sending content of {} derivates to solr for reindexing", totalCount);

        for (String id : list) {
            MCRSolrFilesIndexHandler indexHandler = new MCRSolrFilesIndexHandler(id);
            indexHandler.setCommitWithin(BATCH_AUTO_COMMIT_WITHIN_MS);
            indexHandler.setDestinationCores(cores);
            submitIndexHandler(indexHandler, priority);
        }
        long tStop = System.currentTimeMillis();
        MCRSolrIndexStatisticCollector.FILE_TRANSFER.addTime(tStop - tStart);
    }

    /**
     * Submits a index handler to the executor service (execute as a thread) with the given priority.
     *
     * @param indexHandler
     *            index handler to submit
     */
    public static void submitIndexHandler(MCRSolrIndexHandler indexHandler) {
        submitIndexHandler(indexHandler, LOW_PRIORITY);
    }

    /**
     * Submits a index handler to the executor service (execute as a thread) with the given priority.
     *
     * @param indexHandler
     *            index handler to submit
     * @param priority
     *            higher priority means earlier execution
     */
    public static void submitIndexHandler(MCRSolrIndexHandler indexHandler, int priority) {
        MCRFixedUserCallable<List<MCRSolrIndexHandler>> indexTask = new MCRFixedUserCallable<>(
            new MCRSolrIndexTask(indexHandler), MCRSystemUserInformation.SYSTEM_USER);
        MCRProcessableSupplier<List<MCRSolrIndexHandler>> supplier = SOLR_EXECUTOR.submit(indexTask, priority);
        supplier.getFuture().whenCompleteAsync(afterIndex(indexHandler, priority), SOLR_SUB_EXECUTOR);
    }

    private static BiConsumer<? super List<MCRSolrIndexHandler>, ? super Throwable> afterIndex(
        final MCRSolrIndexHandler indexHandler, final int priority) {
        return (handlerList, exc) -> {
            if (exc != null) {
                LOGGER.error(() -> "Error while submitting index handler: " + indexHandler, exc);
                return;
            }
            if (handlerList == null || handlerList.isEmpty()) {
                return;
            }
            int newPriority = priority + 1;
            for (MCRSolrIndexHandler handler : handlerList) {
                submitIndexHandler(handler, newPriority);
            }
        };
    }

    /**
     * Drops the current solr index.
     */
    public static void dropIndex(SolrClient client) throws Exception {
        LOGGER.info("Dropping solr index...");
        UpdateRequest req = new UpdateRequest();
        req.deleteByQuery("*:*");
        req.setCommitWithin(BATCH_AUTO_COMMIT_WITHIN_MS);
        SOLR_AUTHENTICATION_MANAGER.applyAuthentication(req, MCRSolrAuthenticationLevel.INDEX);
        req.process(client);
        LOGGER.info("Dropping solr index...done");
    }

    public static void dropIndexByType(String type, SolrClient client) throws Exception {
        if (!MCRObjectID.isValidType(type) || Objects.equals(type, "data_file")) {
            LOGGER.warn("The type {} is not a valid type in the actual environment", type);
            return;
        }

        LOGGER.info("Dropping solr index for type {}...", type);
        String deleteQuery = new MessageFormat("objectType:{0} _root_:*_{1}_*", Locale.ROOT)
            .format(new Object[] { type, type });
        UpdateRequest req = new UpdateRequest();
        req.deleteByQuery(deleteQuery);
        req.setCommitWithin(BATCH_AUTO_COMMIT_WITHIN_MS);
        SOLR_AUTHENTICATION_MANAGER.applyAuthentication(req, MCRSolrAuthenticationLevel.INDEX);
        req.process(client);
        LOGGER.info("Dropping solr index for type {}...done", type);
    }

    public static void dropIndexByBase(String base, SolrClient client) throws Exception {
        String type = base.split("_")[1];
        if (!MCRObjectID.isValidType(type) || "data_file".equals(type)) {
            LOGGER.warn("The type {} of base {} is not a valid type in the actual environment", type, base);
            return;
        }

        LOGGER.info("Dropping solr index for base {}...", base);
        String deleteQuery = new MessageFormat("objectType:{0} _root_:{1}_*", Locale.ROOT)
            .format(new Object[] { type, base });
        UpdateRequest req = new UpdateRequest();
        req.deleteByQuery(deleteQuery);
        req.setCommitWithin(BATCH_AUTO_COMMIT_WITHIN_MS);
        SOLR_AUTHENTICATION_MANAGER.applyAuthentication(req, MCRSolrAuthenticationLevel.INDEX);
        req.process(client);
        LOGGER.info("Dropping solr index for base {}...done", base);
    }

    /**
     * Sends a signal to the remote solr server to optimize its index.
     */
    public static void optimize(List<MCRSolrCore> cores) {
        try {
            MCRSolrOptimizeIndexHandler indexHandler = new MCRSolrOptimizeIndexHandler();
            indexHandler.setDestinationCores(cores);
            indexHandler.setCommitWithin(BATCH_AUTO_COMMIT_WITHIN_MS);
            submitIndexHandler(indexHandler);
        } catch (Exception ex) {
            LOGGER.error("Could not optimize solr index", ex);
        }
    }

    /**
     * Synchronizes the solr server with the database. As a result the solr server contains the same documents as the
     * database. All solr zombie documents will be removed, and all not indexed mycore objects will be indexed.
     */
    public static void synchronizeMetadataIndex(List<MCRSolrCore> client) throws IOException, SolrServerException {
        Collection<String> objectTypes = MCRXMLMetadataManager.getInstance().getObjectTypes();
        for (String objectType : objectTypes) {
            synchronizeMetadataIndex(client, objectType);
        }
    }

    /**
     * Synchronizes the solr server with the mycore store for a given object type. As a result the solr server contains
     * the same documents as the store. All solr zombie documents will be removed, and all not indexed mycore objects
     * will be indexed.
     */
    public static void synchronizeMetadataIndex(List<MCRSolrCore> cores, String objectType)
        throws IOException, SolrServerException {
        synchronizeMetadataIndex(cores, objectType, () -> MCRXMLMetadataManager.getInstance().listIDsOfType(objectType),
            "objectType:" + objectType);
    }

    public static void synchronizeMetadataIndexForObjectBase(List<MCRSolrCore> cores, String objectBase)
        throws IOException, SolrServerException {
        final String solrQuery = "objectType:" + objectBase.split("_")[1] + " _root_:" + objectBase + "_*";
        synchronizeMetadataIndex(cores, objectBase, () -> MCRXMLMetadataManager.getInstance()
                .listIDsForBase(objectBase), solrQuery);
    }

    private static void synchronizeMetadataIndex(List<MCRSolrCore> cores, String synchBase,
        Supplier<List<String>> localIDListSupplier, String query)
        throws SolrServerException, IOException {
        LOGGER.info("synchronize {}", synchBase);
        // get ids from store
        LOGGER.info("fetching mycore store...");
        List<String> storeList = localIDListSupplier.get();
        LOGGER.info("there are {} mycore objects", storeList::size);
        // get ids from solr
        LOGGER.info("fetching solr...");
        for (MCRSolrCore core : cores) {
            List<String> solrList = MCRSolrSearchUtils.listIDs(core.getClient(), query);
            LOGGER.info("there are {} solr objects", solrList::size);
            // documents to remove
            List<String> toRemove = new ArrayList<>(solrList);
            toRemove.removeAll(storeList);
            if (!toRemove.isEmpty()) {
                LOGGER.info("remove {} zombie objects from solr", toRemove::size);
                deleteById(core.getClient(), toRemove.toArray(String[]::new));
            }
            deleteOrphanedNestedDocuments(List.of(core));
            // documents to add
            storeList.removeAll(solrList);
            if (!storeList.isEmpty()) {
                LOGGER.info("index {} mycore objects", storeList::size);
                rebuildMetadataIndex(storeList, List.of(core));
            }
        }
    }

}
