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

package org.mycore.datamodel.common;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.mycore.common.MCRCache;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.ifs2.MCRMetadataVersion;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Provides an abstract class for persistence managers of MCRObject and MCRDerivate xml
 * metadata to extend, with methods to perform CRUD operations on object metadata.
 *
 * The default xml metadata manager is {@link MCRDefaultXMLMetadataManager}. If you wish to use
 * another manager implementation instead, change the following property accordingly:
 *
 * MCR.Metadata.Manager.Class=org.mycore.datamodel.common.MCRDefaultXMLMetadataManager
 *
 * Xml metadata managers have a default class they will instantiate for every store.
 * If you wish to use a different default class, change the following property
 * accordingly. For example, when using the MCRDefaultXMLMetadataManager:
 *
 * MCR.Metadata.Store.DefaultClass=org.mycore.datamodel.ifs2.MCRVersioningMetadataStore
 *
 * The following directory will be used by xml metadata managers to keep up-to-date
 * store contents in. This directory will be created if it does not exist yet.
 *
 * MCR.Metadata.Store.BaseDir=/path/to/metadata/dir
 *
 * For each project and type, subdirectories will be created below this path,
 * for example %MCR.Metadata.Store.BaseDir%/DocPortal/document/.
 *
 * If an SVN-based store is configured, then the following property will be used to
 * store and manage local SVN repositories:
 *
 * MCR.Metadata.Store.SVNBase=file:///path/to/local/svndir/
 *
 * It is also possible to change individual properties per project and object type
 * and overwrite the defaults, for example
 *
 * MCR.IFS2.Store.Class=org.mycore.datamodel.ifs2.MCRVersioningMetadataStore
 * MCR.IFS2.Store.SVNRepositoryURL=file:///use/other/location/for/document/versions/
 * MCR.IFS2.Store.SlotLayout=2-2-2-2
 *
 * See documentation of MCRStore, MCRMetadataStore and the MCRXMLMetadataManager
 * extensions (e.g. MCRDefaultXMLMetadataManager) for details.
 *
 * @author Christoph Neidahl (OPNA2608)
 */
public interface MCRXMLMetadataManagerAdapter {

    /**
     * Reads configuration properties, checks and creates base directories and builds the singleton.
     */
    void reload();

    /**
     * Try to validate a store.
     *
     * @param base The base ID of a to-be-validated store
     */
    void verifyStore(String base);

    /**
     * Stores metadata of a new MCRObject in the persistent store.
     *
     * @param mcrid the MCRObjectID
     * @param xml the xml metadata of the MCRObject
     * @param lastModified the date of last modification to set
     * @throws MCRPersistenceException the object couldn't be created due persistence problems
     */
    void create(MCRObjectID mcrid, MCRContent xml, Date lastModified)
        throws MCRPersistenceException;

    /**
     * Delete metadata in store.
     * 
     * @param mcrid the MCRObjectID
     * @throws MCRPersistenceException if an error occurs during the deletion
     */
    void delete(MCRObjectID mcrid) throws MCRPersistenceException;

    /**
     * Updates metadata of existing MCRObject in the persistent store.
     *
     * @param mcrid the MCRObjectID
     * @param xml the xml metadata of the MCRObject
     * @param lastModified the date of last modification to set
     */
    void update(MCRObjectID mcrid, MCRContent xml, Date lastModified)
        throws MCRPersistenceException;

    /**
     * Retrieves the (latest) content of a metadata object.
     *
     * @param mcrid
     *            the id of the object to be retrieved
     * @return a {@link MCRContent} representing the {@link MCRObject} or
     *         <code>null</code> if there is no such object
     * @throws IOException
     */
    MCRContent retrieveContent(MCRObjectID mcrid) throws IOException;

    /**
     * Retrieves the content of a specific revision of a metadata object.
     *
     * @param mcrid
     *            the id of the object to be retrieved
     * @param revision
     *            the revision to be returned, specify -1 if you want to
     *            retrieve the latest revision (includes deleted objects also)
     * @return a {@link MCRContent} representing the {@link MCRObject} of the
     *         given revision or <code>null</code> if there is no such object
     *         with the given revision
     * @throws IOException
     */
    MCRContent retrieveContent(MCRObjectID mcrid, long revision) throws IOException;

    /**
     * Lists all versions of this metadata object available in the
     * subversion repository.
     *
     * @param id
     *            the id of the object to be retrieved
     * @return {@link List} with all {@link MCRMetadataVersion} of
     *         the given object or null if the id is null or the metadata
     *         store doesn't support versioning
     */
    List<MCRMetadataVersion> listRevisions(MCRObjectID id) throws IOException;

    /**
     * This method returns the highest stored ID number for a given MCRObjectID
     * base, or 0 if no object is stored for this type and project.
     *
     * @param project
     *            the project ID part of the MCRObjectID base
     * @param type
     *            the type ID part of the MCRObjectID base
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     * @return the highest stored ID number as a String
     */
    int getHighestStoredID(String project, String type);

    /**
     * Checks if an object with the given MCRObjectID exists in the store.
     *
     * @param mcrid
     *            the MCRObjectID to check
     * @return true if the ID exists, or false if it doesn't
     * @throws MCRPersistenceException if an error occurred in the store
     */
    boolean exists(MCRObjectID mcrid) throws MCRPersistenceException;

    /**
     * Lists all MCRObjectIDs stored for the given base, which is {project}_{type}
     *
     * @param base
     *            the MCRObjectID base, e.g. DocPortal_document
     * @return List of Strings with all MyCoRe identifiers found in the metadata stores for the given base
     */
    List<String> listIDsForBase(String base);

    /**
     * Lists all MCRObjectIDs stored for the given object type, for all projects
     *
     * @param type
     *            the MCRObject type, e.g. document
     * @return List of Strings with all MyCoRe identifiers found in the metadata store for the given type
     */
    List<String> listIDsOfType(String type);

    /**
     * Lists all MCRObjectIDs of all types and projects stored in any metadata store
     *
     * @return List of Strings with all MyCoRe identifiers found in the metadata store
     */
    List<String> listIDs();

    /**
     * Returns all stored object types of MCRObjects/MCRDerivates.
     *
     * @return Collection of Strings with all object types
     * @see MCRObjectID#getTypeId()
     */
    Collection<String> getObjectTypes();

    /**
     * Returns all used base ids of MCRObjects/MCRDerivates.
     *
     * @return Collection of Strings with all object base identifier
     * @see MCRObjectID#getBase()
     */
    Collection<String> getObjectBaseIds();

    /**
     * Returns an enhanced list of object ids and their last modified date
     *
     * @param ids MCRObject ids
     * @throws IOException
     */
    List<MCRObjectIDDate> retrieveObjectDates(List<String> ids) throws IOException;

    /**
     * Returns the time when the xml data of a MCRObject was last modified.
     *
     * @param id
     *            the MCRObjectID of an object
     * @return the last modification data of the object
     * @throws IOException thrown while retrieving the object from the store
     */
    long getLastModified(MCRObjectID id) throws IOException;

    /**
     *
     *
     * @param id
     * @param expire
     * @param unit
     * @return
     */
    MCRCache.ModifiedHandle getLastModifiedHandle(MCRObjectID id, long expire, TimeUnit unit);
}
