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

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRBaseContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.datamodel.common.MCRLinkType;
import org.mycore.datamodel.common.MCRMarkManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.solr.MCRSolrCore;
import org.mycore.solr.MCRSolrCoreManager;
import org.mycore.solr.MCRSolrCoreType;
import org.mycore.solr.index.handlers.MCRSolrIndexHandlerFactory;
import org.mycore.util.concurrent.MCRDelayedRunnable;
import org.mycore.util.concurrent.MCRTransactionableRunnable;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRSolrIndexEventHandler extends MCREventHandlerBase {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final long DELAY_IN_MS = MCRConfiguration2.getLong("MCR.Solr.DelayIndexing_inMS").orElse(2000L);

    @SuppressWarnings("PMD.LooseCoupling")
    private static final DelayQueue<MCRDelayedRunnable> SOLR_TASK_QUEUE = new DelayQueue<>();

    private static final ScheduledExecutorService SOLR_TASK_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private static synchronized void putIntoTaskQueue(MCRDelayedRunnable task) {
        SOLR_TASK_QUEUE.remove(task);
        SOLR_TASK_QUEUE.add(task);
    }

    static {
        //MCR-2359 make sure that MCRSolrIndexer is initialized
        //and its ShutdownHandler are registred
        MCRSolrIndexer.SOLR_EXECUTOR.submit(() -> null);

        SOLR_TASK_EXECUTOR.scheduleWithFixedDelay(() -> {
            LOGGER.debug(() -> "SOLR Task Executor invoked: " + SOLR_TASK_QUEUE.size() + " Documents to process");
            processSolrTaskQueue();

        }, DELAY_IN_MS * 2, DELAY_IN_MS * 2, TimeUnit.MILLISECONDS);

        MCRShutdownHandler.getInstance().addCloseable(new MCRShutdownHandler.Closeable() {
            @Override
            public int getPriority() {
                return Integer.MIN_VALUE + 10;
            }

            @Override
            public void prepareClose() {
                //MCR-2349
                //MCRSolrIndexer requires an early stop of index jobs
                SOLR_TASK_EXECUTOR.shutdown();
                try {
                    SOLR_TASK_EXECUTOR.awaitTermination(10, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    LOGGER.error("Could not shutdown SOLR-Indexing", e);
                }

                if (!SOLR_TASK_QUEUE.isEmpty()) {
                    LOGGER.info("There are still {} solr indexing tasks to complete before shutdown",
                        SOLR_TASK_QUEUE::size);
                    processSolrTaskQueue();
                }
            }

            @Override
            public void close() {
                //all work done in prepareClose phase
            }
        });
    }

    private static void processSolrTaskQueue() {
        while (!SOLR_TASK_QUEUE.isEmpty()) {
            try {
                MCRDelayedRunnable processingTask = SOLR_TASK_QUEUE.poll(DELAY_IN_MS, TimeUnit.MILLISECONDS);
                if (processingTask != null) {
                    LOGGER.info("Sending {} to SOLR...", processingTask::getId);
                    processingTask.run();
                }
            } catch (InterruptedException e) {
                LOGGER.error("Error in SOLR indexing", e);
            }
        }
    }

    @Override
    protected synchronized void handleObjectCreated(MCREvent evt, MCRObject obj) {
        addObject(obj);
    }

    @Override
    protected synchronized void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        addObject(obj);
    }

    @Override
    protected void handleObjectLinkUpdated(MCREvent evt, MCRObject updatedObject, MCRLinkType relation,
        MCRObjectID linkedID) {
        if (relation == MCRLinkType.CHILD) {
            // this is handled by handleAncestorUpdated
            return;
        }
        addContent(linkedID, () -> new MCRBaseContent(MCRMetadataManager.retrieveMCRExpandedObject(linkedID)));
    }

    @Override
    protected void handleAncestorUpdated(MCREvent evt, MCRObject obj) {
        addObject(obj);
    }

    @Override
    protected void handleDerivateLinkUpdated(MCREvent evt, MCRDerivate updatedDerivate, MCRObjectID linkedID) {
        addContent(linkedID, () -> new MCRBaseContent(MCRMetadataManager.retrieveMCRExpandedObject(linkedID)));
    }

    @Override
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        addObject(obj);
    }

    @Override
    protected synchronized void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        solrDelete(obj.getId());
    }

    @Override
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate derivate) {
        addDerivate(derivate);
    }

    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate derivate) {
        addDerivate(derivate);
    }

    @Override
    protected void handleDerivateRepaired(MCREvent evt, MCRDerivate derivate) {
        addDerivate(derivate);
    }

    private void addObject(MCRObject obj) {
        MCRObjectID id = obj.getId();
        addContent(id, () -> new MCRBaseContent(MCRMetadataManager.retrieveMCRExpandedObject(id)));
    }

    private void addDerivate(MCRDerivate derivate) {
        final MCRObjectID id = derivate.getId();
        addContent(id, () -> new MCRBaseContent(MCRMetadataManager.retrieveMCRDerivate(id)));
    }

    @Override
    protected void handleDerivateDeleted(MCREvent evt, MCRDerivate derivate) {
        deleteDerivate(derivate);
    }

    @Override
    protected void handlePathCreated(MCREvent evt, Path path, BasicFileAttributes attrs) {
        addFile(path, attrs);
    }

    @Override
    protected void handlePathUpdated(MCREvent evt, Path path, BasicFileAttributes attrs) {
        addFile(path, attrs);
    }

    @Override
    protected void updatePathIndex(MCREvent evt, Path file, BasicFileAttributes attrs) {
        addFile(file, attrs);
    }

    @Override
    protected void handlePathDeleted(MCREvent evt, Path file, BasicFileAttributes attrs) {
        removeFile(file);
    }

    @Override
    protected void updateDerivateFileIndex(MCREvent evt, MCRDerivate derivate) {
        MCRSessionMgr.getCurrentSession().onCommit(() -> {
            //MCR-2349 initialize solr client early enough
            final List<MCRSolrCore> mainSolrCores = MCRSolrCoreManager.getCoresForType(MCRSolrCoreType.MAIN);
            putIntoTaskQueue(new MCRDelayedRunnable("updateDerivateFileIndex_" + derivate.getId().toString(),
                DELAY_IN_MS,
                new MCRTransactionableRunnable(
                    () -> MCRSolrIndexer.rebuildContentIndex(Collections.singletonList(derivate.getId().toString()),
                        mainSolrCores, MCRSolrIndexer.HIGH_PRIORITY))));
        });
    }

    @Override
    protected void handleObjectIndex(MCREvent evt, MCRObject obj) {
        handleObjectUpdated(evt, obj);
    }

    protected synchronized void addContent(MCRObjectID id, Supplier<MCRContent> indexContentSupplier) {
        // do not add objects which are marked for import or deletion
        if (MCRMarkManager.getInstance().isMarked(id)) {
            return;
        }
        MCRSessionMgr.getCurrentSession().onCommit(() -> {
            long tStart = System.currentTimeMillis();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Solr: submitting data of \"{}\" for indexing", id);
            }

            putIntoTaskQueue(new MCRDelayedRunnable(id.toString(), DELAY_IN_MS,
                new MCRTransactionableRunnable(() -> {
                    try {
                        MCRContent content = indexContentSupplier.get();

                        MCRSolrIndexHandler indexHandler = MCRSolrIndexHandlerFactory.obtainInstance()
                            .getIndexHandler(content, id);
                        indexHandler.setCommitWithin(1000);
                        MCRSolrIndexer.submitIndexHandler(indexHandler, MCRSolrIndexer.HIGH_PRIORITY);

                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Solr: submitting data of \"{}\" for indexing done in {}ms ",
                                id, System.currentTimeMillis() - tStart);
                        }

                    } catch (Exception ex) {
                        LOGGER.error("Error creating transfer thread for object {}", id, ex);
                    }
                })));
        });
    }

    protected synchronized void solrDelete(MCRObjectID id) {
        LOGGER.debug("Solr: submitting data of \"{}\" for deleting", id);
        MCRSessionMgr.getCurrentSession().onCommit(() -> {
            //MCR-2349 initialize solr client early enough
            final SolrClient mainSolrClient = MCRSolrCoreManager.getMainSolrClient();
            putIntoTaskQueue(new MCRDelayedRunnable(id.toString(), DELAY_IN_MS,
                new MCRTransactionableRunnable(() -> MCRSolrIndexer.deleteById(mainSolrClient, id.toString()))));
        });
    }

    protected synchronized void deleteDerivate(MCRDerivate derivate) {
        LOGGER.debug("Solr: submitting data of \"{}\" for derivate", derivate::getId);
        MCRSessionMgr.getCurrentSession().onCommit(() -> {
            //MCR-2349 initialize solr client early enough
            final SolrClient mainSolrClient = MCRSolrCoreManager.getMainSolrClient();
            putIntoTaskQueue(new MCRDelayedRunnable(derivate.getId().toString(), DELAY_IN_MS,
                new MCRTransactionableRunnable(
                    () -> MCRSolrIndexer.deleteDerivate(mainSolrClient, derivate.getId().toString()))));
        });
    }

    protected synchronized void addFile(Path path, BasicFileAttributes attrs) {
        if (path instanceof MCRPath mcrPath) {
            // check if the derivate is marked for deletion
            String owner = mcrPath.getOwner();
            if (MCRObjectID.isValid(owner)) {
                MCRObjectID mcrObjectID = MCRObjectID.getInstance(owner);
                if (MCRMarkManager.getInstance().isMarkedForDeletion(mcrObjectID)) {
                    return;
                }
            }
        }
        MCRSessionMgr.getCurrentSession().onCommit(() -> {
            //MCR-2349 initialize solr client early enough
            putIntoTaskQueue(
                new MCRDelayedRunnable(path.toUri().toString(), DELAY_IN_MS, new MCRTransactionableRunnable(() -> {
                    try {
                        MCRSolrIndexer
                            .submitIndexHandler(
                                MCRSolrIndexHandlerFactory.obtainInstance().getIndexHandler(path, attrs),
                                MCRSolrIndexer.HIGH_PRIORITY);
                    } catch (Exception ex) {
                        LOGGER.error("Error creating transfer thread for file {}", path, ex);
                    }
                })));
        });
    }

    protected synchronized void removeFile(Path file) {
        if (isMarkedForDeletion(file)) {
            return;
        }
        MCRSessionMgr.getCurrentSession().onCommit(() -> {
            //MCR-2349 initialize solr client early enough
            final SolrClient mainSolrClient = MCRSolrCoreManager.getMainSolrClient();
            putIntoTaskQueue(
                new MCRDelayedRunnable(file.toUri().toString(), DELAY_IN_MS, new MCRTransactionableRunnable(() -> {
                    UpdateResponse updateResponse = MCRSolrIndexer
                        .deleteById(mainSolrClient, file.toUri().toString());
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Deleted file {}. Response:{}", file, updateResponse);
                    }
                })));
        });
    }

    /**
     * Returns the derivate identifier for the given path.
     *
     * @param path the path
     * @return the derivate identifier
     */
    protected Optional<MCRObjectID> getDerivateId(Path path) {
        MCRPath mcrPath = MCRPath.ofPath(path);
        if (MCRObjectID.isValid(mcrPath.getOwner())) {
            return Optional.of(MCRObjectID.getInstance(mcrPath.getOwner()));
        }
        return Optional.empty();
    }

    /**
     * Checks if the derivate is marked for deletion.
     *
     * @param path the path to check
     */
    protected boolean isMarkedForDeletion(Path path) {
        return getDerivateId(path).map(MCRMarkManager.getInstance()::isMarkedForDeletion).orElse(false);
    }
}
