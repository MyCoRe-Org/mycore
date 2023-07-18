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

package org.mycore.neo4j.utils;

import org.mycore.datamodel.metadata.MCRObject;

/**
 * @author Andreas Kluge
 * @author Jens Kupferschmidt
 */
public interface MCRNeo4JUtilsInterface {

   /**
    * Adds a node to Neo4j based on the provided MCRObject and its configuration according to the
    * MCRNeo4JUtilsConfigurationHelper class.
    *
    * @param mcrObject the MCRObject to be added as a node
    */
   void addNodeByMCRObject(MCRObject mcrObject);

   /**
    * Updates a node in Neo4j based on the provided MCRObject.
    *
    * @param mcrObject the MCRObject to be updated
    */
   void updateNodeByMCRObject(MCRObject mcrObject);

   /**
    * Deletes a node and its relation from Neo4j based on the provided MCRObject ID.
    *
    * @param id the ID of the MCRObject representing the node to be deleted
    */
   void deleteNodeByMCRObjectID(String id);

}
