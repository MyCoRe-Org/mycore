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

package org.mycore.mcr.neo4j.index;

import org.mycore.mcr.neo4j.datamodel.metadata.neo4jutil.MCRNeo4JConstants;
import org.mycore.mcr.neo4j.utils.MCRNeo4JDatabaseDriver;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.datamodel.common.MCRMarkManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.neo4j.datamodel.metadata.neo4jparser.MCRNeo4JParser;
import org.mycore.mcr.neo4j.utils.MCRNeo4JQueryRunner;
import org.mycore.util.concurrent.MCRDelayedRunnable;
import org.mycore.util.concurrent.MCRTransactionableRunnable;

/**
 * Neo4J Event Handler for Indexing
 * @author Thomas Scheffler (yagee)
 * @author Andreas Kluge
 * @author Jens Kupferschmidt
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class MCRNeo4JIndexEventHandler extends MCREventHandlerBase {

   private static final Logger LOGGER = LogManager.getLogger(MCRNeo4JIndexEventHandler.class);

   private static final long DELAY_IN_MS
      = MCRConfiguration2.getLong(MCRNeo4JConstants.NEO4J_CONFIG_PREFIX + "DelayIndexing_inMS").orElse(2000L);

   private static final DelayQueue<MCRDelayedRunnable> NEO4J_TASK_QUEUE = new DelayQueue<>();

   private static final ScheduledExecutorService NEO4J_TASK_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

   static {

      NEO4J_TASK_EXECUTOR.scheduleWithFixedDelay(() -> {
         LOGGER.debug("NEO4J Task Executor invoked: {} Nodes to process", NEO4J_TASK_QUEUE.size());
         processNeo4JTaskQueue();

      }, DELAY_IN_MS * 2, DELAY_IN_MS * 2, TimeUnit.MILLISECONDS);

      MCRShutdownHandler.getInstance().addCloseable(new MCRShutdownHandler.Closeable() {
         @Override
         public int getPriority() {
            return Integer.MIN_VALUE + 10;
         }

         @Override
         public void prepareClose() {
            NEO4J_TASK_EXECUTOR.shutdown();
            try {
               NEO4J_TASK_EXECUTOR.awaitTermination(10, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
               LOGGER.error("Could not shutdown Neo4J-Indexing", e);
            }

            if (!NEO4J_TASK_QUEUE.isEmpty()) {
               LOGGER.info("There are still {} Neo4J indexing tasks to complete before shutdown",
                  NEO4J_TASK_QUEUE.size());
               processNeo4JTaskQueue();
            }
         }

         @Override
         public void close() {
            //all work done in prepareClose phase
         }
      });

   }

   private final MCRNeo4JParser parser;

   public MCRNeo4JIndexEventHandler() {
      parser = new MCRNeo4JParser();
   }

   private static synchronized void putIntoTaskQueue(MCRDelayedRunnable task) {
      NEO4J_TASK_QUEUE.remove(task);
      NEO4J_TASK_QUEUE.add(task);
   }

   private static void processNeo4JTaskQueue() {
      while (!NEO4J_TASK_QUEUE.isEmpty()) {
         try {
            MCRDelayedRunnable processingTask = NEO4J_TASK_QUEUE.poll(DELAY_IN_MS, TimeUnit.MILLISECONDS);
            if (processingTask != null) {
               LOGGER.info("Sending {} to neo4j...", processingTask.getId());
               processingTask.run();
            }
         } catch (InterruptedException e) {
            LOGGER.error("Error in neo4j indexing", e);
         }
      }
   }

   @Override
   protected synchronized void handleObjectCreated(MCREvent evt, MCRObject obj) {
      LOGGER.info("Handle {}", obj.getId());
      addObject(evt, obj);
   }

   @Override
   protected synchronized void handleObjectUpdated(MCREvent evt, MCRObject obj) {
      updateObject(evt, obj);
   }

   @Override
   protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
      updateObject(evt, obj);
   }

   @Override
   protected synchronized void handleObjectDeleted(MCREvent evt, MCRObject obj) {
      deleteObject(obj.getId());
   }

   @Override
   protected void handleObjectIndex(MCREvent evt, MCRObject obj) {
      handleObjectUpdated(evt, obj);
   }

   @SuppressWarnings("unused")
   protected synchronized void updateObject(MCREvent evt, MCRObject mcrObject) {
      LOGGER.debug("Neo4j: update id {}", mcrObject.getId());

      // do not add objects marked for deletion
      if (MCRMarkManager.instance().isMarked(mcrObject.getId())) {
         return;
      }

      MCRSessionMgr.getCurrentSession().onCommit(() -> {

         if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Neo4j: submitting data of {} for indexing", mcrObject.getId());
         }

         putIntoTaskQueue(new MCRDelayedRunnable(mcrObject.getId().toString(), DELAY_IN_MS,
            new MCRTransactionableRunnable(() -> {
               try {
                  String updateQuery = parser.createNeo4JUpdateQuery(mcrObject);
                  LOGGER.debug("UpdateQuery: {}", updateQuery);
                  MCRNeo4JQueryRunner.commitWriteOnlyQuery(updateQuery);

                  String query = parser.createNeo4JQuery(mcrObject);
                  LOGGER.info("Query: {}", query);
                  MCRNeo4JQueryRunner.commitWriteOnlyQuery(query);
               } catch (Exception e) {
                  LOGGER.error("Error creating transfer thread for object {}", mcrObject, e);
               }
            })));
      });
   }

   @SuppressWarnings("unused")
   protected synchronized void addObject(MCREvent evt, MCRObject mcrObject) {
      LOGGER.debug("Neo4j: add id {}", mcrObject.getId());

      // do not add objects marked for deletion
      if (MCRMarkManager.instance().isMarked(mcrObject.getId())) {
         return;
      }

      MCRSessionMgr.getCurrentSession().onCommit(() -> {

         if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Neo4j: submitting data of {} for indexing", mcrObject.getId());
         }

         putIntoTaskQueue(new MCRDelayedRunnable(mcrObject.getId().toString(), DELAY_IN_MS,
            new MCRTransactionableRunnable(() -> {
               try {
                  String query = parser.createNeo4JQuery(mcrObject);
                  LOGGER.info("Query: {}", query);
                  MCRNeo4JQueryRunner.commitWriteOnlyQuery(query);
               } catch (Exception e) {
                  LOGGER.error("Error creating transfer thread for object {}", mcrObject, e);
               }
            })));
      });
   }

   protected synchronized void deleteObject(MCRObjectID id) {
      LOGGER.debug("Neo4j: delete id {}", id);
      MCRSessionMgr.getCurrentSession()
         .onCommit(() -> putIntoTaskQueue(new MCRDelayedRunnable(id.toString(), DELAY_IN_MS,
            new MCRTransactionableRunnable(() -> {
               String queryString = "MATCH (n {id:'" + id + "'}) DETACH DELETE n";
               MCRNeo4JQueryRunner.commitWriteOnlyQuery(queryString);
            }))));
   }

}
