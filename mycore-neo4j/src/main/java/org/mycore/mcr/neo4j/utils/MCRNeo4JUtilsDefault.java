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

package org.mycore.mcr.neo4j.utils;

import static org.mycore.mcr.neo4j.utils.MCRNeo4JDatabaseDriver.commitWriteOnlyQuery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mcr.neo4j.datamodel.metadata.neo4jparser.MCRNeo4JMetaParser;
import org.mycore.mcr.neo4j.datamodel.metadata.neo4jparser.MCRNeo4JParser;

/**
 * The MCRNeo4JUtilsDefault class is an implementation of the MCRNeo4JUtilsInterface. It provides methods for adding,
 * updating, and deleting nodes in Neo4j based on MCRObject instances.
 * <p>
 * Note: This class assumes the usage of MyCoRe and its specific configuration conventions. It relies on the
 * MCRConfiguration2 and MCRCategoryDAO classes for retrieving configuration properties and category information,
 * respectively.
 * <p>
 * Example usage: MCRNeo4JUtilsDefault neo4jUtils = new MCRNeo4JUtilsDefault(); MCRObject mcrObject = // Obtain the
 * MCRObject instance neo4jUtils.addNodeByMCRObject(mcrObject); // Perform further operations
 * <p>
 * Note: The LOGGER in this class uses Log4j2 for logging.
 *
 * @author Andreas Kluge
 * @author Jens Kupferschmidt
 */
public class MCRNeo4JUtilsDefault implements MCRNeo4JUtilsInterface {

    private static final Logger LOGGER = LogManager.getLogger(MCRNeo4JUtilsDefault.class);

    private final MCRNeo4JMetaParser parser;

    public MCRNeo4JUtilsDefault() {
        parser = new MCRNeo4JParser();
    }

    /**
    * Adds a node to Neo4j based on the provided MCRObject and its configuration according to the
    * MCRNeo4JUtilsConfigurationHelper class.
    *
    * @param mcrObject the MCRObject to be added as a node
    */
    @Override
    public void addNodeByMCRObject(MCRObject mcrObject) {
        String queryString = parser.createNeo4JQuery(mcrObject);
        LOGGER.info("Query: {}", queryString);
        commitWriteOnlyQuery(queryString);
    }

    /**
    * Updates a node in Neo4j based on the provided MCRObject. This method delegates the operation to addNodeByMCRObject
    * as there is no separate update functionality.
    *
    * @param mcrObject the MCRObject to be updated
    */
    @Override
    public void updateNodeByMCRObject(MCRObject mcrObject) {
        String updateQuery = parser.createNeo4JUpdateQuery(mcrObject);
        LOGGER.info("UpdateQuery: {}", updateQuery);
        commitWriteOnlyQuery(updateQuery);
        addNodeByMCRObject(mcrObject);
    }

    /**
    * Deletes a node from Neo4j based on the provided MCRObject ID.
    *
    * @param id the ID of the MCRObject representing the node to be deleted
    */
    @Override
    public void deleteNodeByMCRObjectID(String id) {
        String queryString = "MATCH (n {id:'" + id + "'}) DETACH DELETE n;";
        commitWriteOnlyQuery(queryString);
    }

}
