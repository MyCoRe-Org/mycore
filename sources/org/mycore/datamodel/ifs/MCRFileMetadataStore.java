/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.datamodel.ifs;

import java.util.Vector;

import org.mycore.common.MCRPersistenceException;

/**
 * Implementations of this class can be used to store the metadata of all
 * MCRFilesystemNodes in a persistent datastore. While MCRContentStores hold a
 * file's content, this store holds its descriptive data like the directory
 * structure, file type, checksum and size. There can only be one instance to be
 * used in a system, that instance is configured by the property
 * <b>MCR.IFS.FileMetadataStore.Class </b>
 * 
 * @see MCRFileMetadataManager
 * @see MCRFilesystemNode
 * @see MCRContentStore
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public interface MCRFileMetadataStore {
    /**
     * Creates or updates the data of the given node in the persistent store.
     * 
     * @param node
     *            the MCRFilesystemNode to be stored
     */
    public void storeNode(MCRFilesystemNode node) throws MCRPersistenceException;

    /**
     * Retrieves the MCRFilesystemNode with that ID from the persistent store.
     * 
     * @param ID the
     *            unique ID of the MCRFilesystemNode
     * @return the node with that ID, or null if no such node exists
     */
    public MCRFilesystemNode retrieveNode(String ID) throws MCRPersistenceException;

    /**
     * Retrieves a child node of an MCRDirectory from the persistent store.
     * 
     * @param parentID
     *            the unique ID of the parent MCRDirectory
     * @param name
     *            the filename of the child node in that directory
     * @return the child MCRFilesystemNode, or null if no such node exists
     */
    public MCRFilesystemNode retrieveChild(String parentID, String name) throws MCRPersistenceException;

    /**
     * Retrieves the root MCRFilesystemNode that has no parent and is owned by
     * the object with the given owner ID.
     * 
     * @param ownerID
     *            the ID of the owner of the root node.
     * @return an MCRFilesystemNode that has no parent and this owner ID, or
     *         null if no such node exists
     */
    public String retrieveRootNodeID(String ownerID) throws MCRPersistenceException;

    /**
     * Returns a list of the IDs of all children of a given parent MCRDirectory.
     * 
     * @param parentID
     *            the ID of the parent MCRDirectory
     * @return a Vector of String objects that are the IDs of all child nodes in
     *         that directory
     */
    public Vector retrieveChildrenIDs(String parentID) throws MCRPersistenceException;

    /**
     * Deletes all data of a given MCRFilesystemNode in the persistent metadata
     * store.
     * 
     * @param ID
     *            the unique ID of the MCRFilesystemNode to delete
     */
    public void deleteNode(String ID) throws MCRPersistenceException;
}
