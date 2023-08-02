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

package org.mycore.mcr.neo4j.frontend.cli;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.mcr.neo4j.datamodel.metadata.neo4jparser.MCRNeo4JParser;
import org.mycore.mcr.neo4j.utils.MCRNeo4JDatabaseDriver;
import org.mycore.mcr.neo4j.utils.MCRNeo4JQueryRunner;
import org.mycore.mcr.neo4j.utils.MCRNeo4JManager;

import java.util.List;

@MCRCommandGroup(name = "Commands to handle Neo4J")
@SuppressWarnings("unused")
public class MCRNeo4JCommands extends MCRAbstractCommands {

   private static final String NEO4J_MANAGER_CLASS_PROPERTY = "MCR.Neo4J.Manager.Class";
   private static final Logger LOGGER = LogManager.getLogger(MCRNeo4JCommands.class.getName());

   @MCRCommand(syntax = "clean neo4j of id {0}", help = "clean the Neo4J database for an ID {0}", order = 10)
   public static void cleanForMCRID(final String id) {
      LOGGER.info("Start clean data from Neo4J instance for an MCRID");
      String queryString = "MATCH (n {id:'" + id + "'}) DETACH DELETE n";
      LOGGER.info("Query: {}", queryString);
      MCRNeo4JQueryRunner.commitWriteOnlyQuery(queryString);
   }

   @MCRCommand(syntax = "clean neo4j of base {0}", help = "clean the Neo4J database for a MCRBase {0}", order = 20)
   public static void cleanForBase(final String baseId) {
      LOGGER.info("Start clean data from Neo4J instance for MCRBase {}", baseId);
      List<String> selectedObjectIds = MCRXMLMetadataManager
         .instance().listIDsForBase(baseId);
      for (String objectId : selectedObjectIds) {
         String queryString = "MATCH (n {id:'" + objectId + "'}) DETACH DELETE n";
         LOGGER.info("Query: {}", queryString);
         MCRNeo4JQueryRunner.commitWriteOnlyQuery(queryString);
      }
   }

   @MCRCommand(syntax = "clean neo4j of type {0}", help = "clean the Neo4J database for a MCRType {0}", order = 30)
   public static void cleanForType(final String type) {
      LOGGER.info("Start clean data from Neo4J instance for MCRType {}", type);
      String queryString = "MATCH (n:" + type + ") DETACH DELETE n";
      LOGGER.info("Query: {}", queryString);
      MCRNeo4JQueryRunner.commitWriteOnlyQuery(queryString);
   }

   @MCRCommand(syntax = "clean neo4j metadata", help = "clean the complete Neo4J database", order = 40)
   public static void cleanAll() {
      LOGGER.info("Start clean all data from Neo4J");
      String queryString = "MATCH (n) DETACH DELETE n";
      LOGGER.info("Query: {}", queryString);
      MCRNeo4JQueryRunner.commitWriteOnlyQuery(queryString);
   }

   @MCRCommand(syntax = "synchronize neo4j of id {0}",
      help = "synchronize metadata to the Neo4J database for MCRID {0}",
      order = 50)
   public static void synchronizeForMCRID(final String id) {
      LOGGER.info("Synchronize Neo4J with metadata for MCRID");
      MCRNeo4JManager clazz
         = MCRConfiguration2.getOrThrow(NEO4J_MANAGER_CLASS_PROPERTY, MCRConfiguration2::instantiateClass);
      MCRObject mcrObject = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(id));
      clazz.updateNodeByMCRObject(mcrObject);
   }

   @MCRCommand(syntax = "synchronize neo4j of base {0}",
      help = "synchronize metadata to the Neo4J database for MCRBase {0}", order = 60)
   public static void synchronizeForBase(final String baseId) {
      LOGGER.info("Synchronize Neo4J with metadata for MCRBase {}", baseId);
      MCRNeo4JManager clazz
         = MCRConfiguration2.getOrThrow(NEO4J_MANAGER_CLASS_PROPERTY, MCRConfiguration2::instantiateClass);
      List<String> selectedObjectIds = MCRXMLMetadataManager
         .instance().listIDsForBase(baseId);
      for (String objectId : selectedObjectIds) {
         MCRObject mcrObject = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(objectId));
         clazz.updateNodeByMCRObject(mcrObject);
      }
   }

   @MCRCommand(syntax = "synchronize neo4j of type {0}",
      help = "synchronize metadata to the Neo4J database for MCRType {0}", order = 70)
   public static void synchronizeForType(final String type) {
      LOGGER.info("Synchronize Neo4J with metadata for MCRType {}", type);
      MCRNeo4JManager clazz
         = MCRConfiguration2.getOrThrow(NEO4J_MANAGER_CLASS_PROPERTY, MCRConfiguration2::instantiateClass);
      List<String> selectedObjectIds = MCRXMLMetadataManager
         .instance().listIDsOfType(type);
      for (String objectId : selectedObjectIds) {
         MCRObject mcrObject = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(objectId));
         clazz.updateNodeByMCRObject(mcrObject);
      }
   }

   @MCRCommand(syntax = "synchronize neo4j metadata", help = "synchronize all metadata the Neo4J database", order = 80)
   public static void synchronizeAll() {
      LOGGER.info("Synchronize Neo4J with all metadata");
      MCRNeo4JManager clazz
         = MCRConfiguration2.getOrThrow(NEO4J_MANAGER_CLASS_PROPERTY, MCRConfiguration2::instantiateClass);
      List<String> selectedObjectIds = MCRXMLMetadataManager
         .instance()
         .listIDs();
      for (String objectId : selectedObjectIds) {
         MCRObject mcrObject = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(objectId));
         clazz.updateNodeByMCRObject(mcrObject);
      }
   }

   @MCRCommand(syntax = "test neo4j connection", help = "test connectivity with the Neo4j database", order = 90)
   public static void testConnection() {
      LOGGER.info("Test connection to Neo4j Database");
      MCRNeo4JDatabaseDriver.getInstance().getDriver();
      boolean connected = MCRNeo4JDatabaseDriver.getInstance().testConnectionSettings();
      LOGGER.info("Neo4j connected {}", connected);
   }

   @MCRCommand(syntax = "test neo4j of id {0}",
      help = "synchronize metadata to the Neo4J database for MCRID {0}",
      order = 90)
   public static void test(final String id) {
      LOGGER.info("Synchronize Neo4J with metadata for MCRID");

      MCRObject mcrObject = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(id));
      MCRNeo4JParser parser = new MCRNeo4JParser();
      String neo4JQuery = parser.createNeo4JQuery(mcrObject);

      LOGGER.info(neo4JQuery);
   }

}
