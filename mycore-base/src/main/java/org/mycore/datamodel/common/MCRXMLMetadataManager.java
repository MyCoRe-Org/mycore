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

package org.mycore.datamodel.common;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationBase;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.ifs2.MCRMetadataVersion;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Provides an interface for persistence managers of MCRObject and MCRDerivate xml
 * metadata to extend, with methods to perform CRUD operations on object metadata.
 * <p>
 * The default xml metadata manager is {@link MCRDefaultXMLMetadataManager}. If you wish to use
 * another manager implementation instead, change the following property accordingly:
 * <p>
 * MCR.Metadata.Manager.Class=org.mycore.datamodel.common.MCRDefaultXMLMetadataManager
 * <p>
 * Xml metadata managers have a default class they will instantiate for every store.
 * If you wish to use a different default class, change the following property
 * accordingly. For example, when using the MCRDefaultXMLMetadataManager:
 * <p>
 * MCR.Metadata.Store.DefaultClass=org.mycore.datamodel.ifs2.MCRVersioningMetadataStore
 * <p>
 * The following directory will be used by xml metadata managers to keep up-to-date
 * store contents in. This directory will be created if it does not exist yet.
 * <p>
 * MCR.Metadata.Store.BaseDir=/path/to/metadata/dir
 * <p>
 * For each project and type, subdirectories will be created below this path,
 * for example %MCR.Metadata.Store.BaseDir%/DocPortal/document/.
 * <p>
 * If an SVN-based store is configured, then the following property will be used to
 * store and manage local SVN repositories:
 * <p>
 * MCR.Metadata.Store.SVNBase=file:///path/to/local/svndir/
 * <p>
 * It is also possible to change individual properties per project and object type
 * and overwrite the defaults, for example
 * <p>
 * MCR.IFS2.Store.Class=org.mycore.datamodel.ifs2.MCRVersioningMetadataStore
 * MCR.IFS2.Store.SVNRepositoryURL=file:///use/other/location/for/document/versions/
 * MCR.IFS2.Store.SlotLayout=2-2-2-2
 * <p>
 * See documentation of MCRStore, MCRMetadataStore and the MCRXMLMetadataManager
 * extensions (e.g. MCRDefaultXMLMetadataManager) for details.
 *
 * @author Christoph Neidahl (OPNA2608)
 */
public interface MCRXMLMetadataManager {

    /**
     * Returns the singleton instance of {@link MCRXMLMetadataManager}. Reads the property
     * MCR.Metadata.Manager.Class to instantiate the configured XML metadata manager adapter to be used
     * to perform the metadata operations.
     *
     * @return the XML metadata manager
     * 
     * @deprecated Use {@link MCRXMLMetadataManager#obtainInstance()} instead
     */
    @Deprecated(forRemoval = true)
    static MCRXMLMetadataManager getInstance() {
        return obtainInstance();
    }

    /**
     * Returns the singleton instance of {@link MCRXMLMetadataManager}. Reads the property
     * MCR.Metadata.Manager.Class to instantiate the configured XML metadata manager adapter to be used
     * to perform the metadata operations.
     *
     * @return the XML metadata manager
     */
    static MCRXMLMetadataManager obtainInstance() {
        return LazyInstanceHolder.SINGLETON_INSTANCE;
    }

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
     */
    MCRContent retrieveContent(MCRObjectID mcrid, String revision) throws IOException;

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
    List<? extends MCRAbstractMetadataVersion<?>> listRevisions(MCRObjectID id) throws IOException;

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

    default int getHighestStoredID(String base) {
        return getHighestStoredID(
                base.substring(0, base.indexOf('_')),
                base.substring(base.indexOf('_') + 1));
    }
    
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

    MCRCache.ModifiedHandle getLastModifiedHandle(MCRObjectID id, long expire, TimeUnit unit);

    /*
     * Redirections/wrappers to delegations
     */

    /**
     * Stores metadata of a new MCRObject in the persistent store.
     *
     * @param mcrid the MCRObjectID
     * @param xml the xml metadata of the MCRObject
     * @param lastModified the date of last modification to set
     * @throws MCRPersistenceException the object couldn't be created due persistence problems
     */
    default void create(MCRObjectID mcrid, Document xml, Date lastModified)
        throws MCRPersistenceException {
        create(mcrid, new MCRJDOMContent(xml), lastModified);
    }

    /**
     * Stores metadata of a new MCRObject in the persistent store.
     *
     * @param mcrid the MCRObjectID
     * @param xml the xml metadata of the MCRObject
     * @param lastModified the date of last modification to set
     * @throws MCRPersistenceException the object couldn't be created due persistence problems
     */
    default void create(MCRObjectID mcrid, byte[] xml, Date lastModified) throws MCRPersistenceException {
        create(mcrid, new MCRByteContent(xml, lastModified.getTime()), lastModified);
    }

    default void delete(String mcrid) throws MCRPersistenceException {
        delete(MCRObjectID.getInstance(mcrid));
    }

    /**
     * Updates metadata of existing MCRObject in the persistent store.
     *
     * @param mcrid the MCRObjectID
     * @param xml the xml metadata of the MCRObject
     * @param lastModified the date of last modification to set
     * @throws MCRPersistenceException the object couldn't be updated due persistence problems
     */
    default void update(MCRObjectID mcrid, Document xml, Date lastModified)
        throws MCRPersistenceException {
        update(mcrid, new MCRJDOMContent(xml), lastModified);
    }

    /**
     * Creates or updates metadata of a MCRObject in the persistent store.
     *
     * @param mcrid the MCRObjectID
     * @param xml the xml metadata of the MCRObject
     * @param lastModified the date of last modification to set
     * @throws MCRPersistenceException the object couldn't be created or updated due persistence problems
     */
    default void createOrUpdate(MCRObjectID mcrid, Document xml, Date lastModified)
        throws MCRPersistenceException {
        if (exists(mcrid)) {
            update(mcrid, xml, lastModified);
        } else {
            create(mcrid, xml, lastModified);
        }
    }

    /**
     * Updates metadata of existing MCRObject in the persistent store.
     *
     * @param mcrid the MCRObjectID
     * @param xml the xml metadata of the MCRObject
     * @param lastModified the date of last modification to set
     * @throws MCRPersistenceException the object couldn't be updated due persistence problems
     */
    default void update(MCRObjectID mcrid, byte[] xml, Date lastModified) throws MCRPersistenceException {
        update(mcrid, new MCRByteContent(xml, lastModified.getTime()), lastModified);
    }

    /**
     * Retrieves stored metadata xml as JDOM document
     *
     * @param mcrid the MCRObjectID
     * @return null if metadata is not present
     */
    default Document retrieveXML(MCRObjectID mcrid) throws IOException, JDOMException {
        MCRContent metadata = retrieveContent(mcrid);
        return metadata == null ? null : metadata.asXML();
    }

    /**
     * Retrieves stored metadata xml as byte[] BLOB.
     *
     * @param mcrid the MCRObjectID
     * @return null if metadata is not present
     */
    default byte[] retrieveBLOB(MCRObjectID mcrid) throws IOException {
        MCRContent metadata = retrieveContent(mcrid);
        return metadata == null ? null : metadata.asByteArray();
    }

    /**
     * Lists all objects with their last modification dates.
     *
     * @return List of {@link MCRObjectIDDate}
     */
    default List<MCRObjectIDDate> listObjectDates() throws IOException {
        return retrieveObjectDates(this.listIDs());
    }

    /**
     * Lists all objects of the specified <code>type</code> and their last modified date.
     *
     * @param type type of object
     */
    default List<MCRObjectIDDate> listObjectDates(String type) throws IOException {
        return retrieveObjectDates(this.listIDsOfType(type));
    }

    /**
     * Returns the time the store's content was last modified
     *
     * @return Last modification date of the MyCoRe system
     */
    default long getLastModified() {
        return MCRConfigurationBase.getSystemLastModified();
    }

    final class LazyInstanceHolder {
        public static final MCRXMLMetadataManager SINGLETON_INSTANCE = MCRConfiguration2.getInstanceOfOrThrow(
            MCRXMLMetadataManager.class, "MCR.Metadata.Manager.Class");
    }

}
