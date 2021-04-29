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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationBase;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.ifs2.MCRMetadataStore;
import org.mycore.datamodel.ifs2.MCRMetadataVersion;
import org.mycore.datamodel.ifs2.MCRObjectIDFileSystemDate;
import org.mycore.datamodel.ifs2.MCRStoredMetadata;
import org.mycore.datamodel.ifs2.MCRVersionedMetadata;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.xml.sax.SAXException;

/**
 * Provides an interface for persistence managers of MCRObject and MCRDerivate xml metadata
 * with methods to create, retrieve, update and delete object metadata
 * using IFS2 MCRMetadataStore instances.
 * 
 * The default metadata manager is MCRXMLMetadataManager. If you wish to use
 * another manager implementation instead, change the following property accordingly.
 * 
 * MCR.Metadata.Manager.Class=full.qualified.class.name.of.manager.here
 *
 * For configuration, at least the following properties must be set:
 *
 * MCR.Metadata.Store.BaseDir=/path/to/metadata/dir
 * MCR.Metadata.Store.SVNBase=file:///path/to/local/svndir/
 *
 * Both directories will be created if they do not exist yet.
 * For each project and type, a subdirectory will be created,
 * for example %MCR.Metadata.Store.BaseDir%/DocPortal/document/.
 *
 * The default IFS2 store is MCRVersioningMetadataStore, which
 * versions metadata using SVN in local repositories below SVNBase.
 * If you do not want versioning and would like to have better
 * performance, change the following property to
 *
 * MCR.Metadata.Store.DefaultClass=org.mycore.datamodel.ifs2.MCRMetadataStore
 *
 * It is also possible to change individual properties per project and object type
 * and overwrite the defaults, for example
 *
 * MCR.IFS2.Store.Class=org.mycore.datamodel.ifs2.MCRVersioningMetadataStore
 * MCR.IFS2.Store.SVNRepositoryURL=file:///use/other/location/for/document/versions/
 * MCR.IFS2.Store.SlotLayout=2-2-2-2
 *
 * See documentation of MCRStore and MCRMetadataStore for details.
 *
 * @author Christoph Neidahl (OPNA2608)
 */
public abstract class MCRXMLMetadataManager {
    private static final Logger LOGGER = LogManager.getLogger(MCRXMLMetadataManager.class);

    private static MCRXMLMetadataManager implementation;

    public static synchronized MCRXMLMetadataManager instance() {
        try {
            if (implementation == null) {
                implementation = MCRConfiguration2
                    .getSingleInstanceOf("MCR.Metadata.Manager.Class", MCRDefaultXMLMetadataManager.class).get();
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }

        return implementation;
    }

    /**
     * Reads configuration properties, checks and creates base directories and builds the singleton
     */
    public abstract void reload();

    /**
     * Try to validate a store.
     * @param base The base ID of a to-be-validated store
     */
    public abstract void verifyStore(String base);

    public abstract MCRStoredMetadata create(MCRObjectID mcrid, Document xml, Date lastModified)
        throws MCRPersistenceException;

    /**
     * Stores metadata of a new MCRObject in the persistent store.
     *
     * @param mcrid the MCRObjectID
     * @param xml the xml metadata of the MCRObject
     * @param lastModified the date of last modification to set
     * @return the stored metadata as IFS2 object
     * @throws MCRPersistenceException the object couldn't be created due persistence problems
     */
    public abstract MCRStoredMetadata create(MCRObjectID mcrid, byte[] xml, Date lastModified)
        throws MCRPersistenceException;

    /**
     * Stores metadata of a new MCRObject in the persistent store.
     *
     * @param mcrid the MCRObjectID
     * @param xml the xml metadata of the MCRObject
     * @param lastModified the date of last modification to set
     * @return the stored metadata as IFS2 object
     * @throws MCRPersistenceException the object couldn't be created due persistence problems
     */
    public abstract MCRStoredMetadata create(MCRObjectID mcrid, MCRContent xml, Date lastModified)
        throws MCRPersistenceException;

    public void delete(String mcrid) throws MCRPersistenceException {
        delete(MCRObjectID.getInstance(mcrid));
    }

    public abstract void delete(MCRObjectID mcrid) throws MCRPersistenceException;

    /**
     * Updates metadata of existing MCRObject in the persistent store.
     *
     * @param mcrid the MCRObjectID
     * @param xml the xml metadata of the MCRObject
     * @param lastModified the date of last modification to set
     * @return the stored metadata as IFS2 object
     * @throws MCRPersistenceException the object couldn't be updated due persistence problems
     */
    public abstract MCRStoredMetadata update(MCRObjectID mcrid, Document xml, Date lastModified)
        throws MCRPersistenceException;

    /**
     * Creates or updates metadata of a MCRObject in the persistent store.
     *
     * @param mcrid the MCRObjectID
     * @param xml the xml metadata of the MCRObject
     * @param lastModified the date of last modification to set
     * @return the stored metadata as IFS2 object
     * @throws MCRPersistenceException the object couldn't be created or updated due persistence problems
     */
    public MCRStoredMetadata createOrUpdate(MCRObjectID mcrid, Document xml, Date lastModified)
        throws MCRPersistenceException {
        if (exists(mcrid)) {
            return update(mcrid, xml, lastModified);
        } else {
            return create(mcrid, xml, lastModified);
        }
    }

    /**
     * Updates metadata of existing MCRObject in the persistent store.
     *
     * @param mcrid the MCRObjectID
     * @param xml the xml metadata of the MCRObject
     * @param lastModified the date of last modification to set
     * @return the stored metadata as IFS2 object
     */
    public MCRStoredMetadata update(MCRObjectID mcrid, byte[] xml, Date lastModified) throws MCRPersistenceException {
        return update(mcrid, new MCRByteContent(xml, lastModified.getTime()), lastModified);
    }

    /**
     * Updates metadata of existing MCRObject in the persistent store.
     *
     * @param mcrid the MCRObjectID
     * @param xml the xml metadata of the MCRObject
     * @param lastModified the date of last modification to set
     * @return the stored metadata as IFS2 object
     */
    public abstract MCRStoredMetadata update(MCRObjectID mcrid, MCRContent xml, Date lastModified)
        throws MCRPersistenceException;

    /**
     * Retrieves stored metadata xml as JDOM document
     *
     * @param mcrid the MCRObjectID
     * @return null if metadata is not present
     */
    public Document retrieveXML(MCRObjectID mcrid) throws IOException, JDOMException, SAXException {
        MCRContent metadata = retrieveContent(mcrid);
        return metadata == null ? null : metadata.asXML();
    }

    /**
     * Retrieves stored metadata xml as byte[] BLOB.
     *
     * @param mcrid the MCRObjectID
     * @return null if metadata is not present
     */
    public byte[] retrieveBLOB(MCRObjectID mcrid) throws IOException {
        MCRContent metadata = retrieveContent(mcrid);
        return metadata == null ? null : metadata.asByteArray();
    }

    public abstract MCRContent retrieveContent(MCRObjectID mcrid) throws IOException;

    /**
     * @param mcrid
     *            the id of the object to be retrieved
     * @param revision
     *            the revision to be returned, specify -1 if you want to
     *            retrieve the latest revision (includes deleted objects also)
     * @return a {@link MCRContent} representing the {@link MCRObject} of the
     *         given revision or <code>null</code> if there is no such object
     *         with the given revision
     */
    public abstract MCRContent retrieveContent(MCRObjectID mcrid, long revision) throws IOException;

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
    public abstract List<MCRMetadataVersion> listRevisions(MCRObjectID id) throws IOException;

    public abstract MCRVersionedMetadata getVersionedMetaData(MCRObjectID id) throws IOException;

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
    public abstract int getHighestStoredID(String project, String type);

    /**
     * Checks if an object with the given MCRObjectID exists in the store.
     */
    public abstract boolean exists(MCRObjectID mcrid) throws MCRPersistenceException;

    /**
     * Lists all MCRObjectIDs stored for the given base, which is {project}_{type}
     *
     * @param base the MCRObjectID base, e.g. DocPortal_document
     */
    public abstract List<String> listIDsForBase(String base);

    /**
     * Lists all MCRObjectIDs stored for the given object type, for all projects
     *
     * @param type the MCRObject type, e.g. document
     * @return list of all mycore identifiers found in the metadata store for the given type
     */
    public abstract List<String> listIDsOfType(String type);

    /**
     * Lists all MCRObjectIDs of all types and projects stored in any metadata store
     *
     * @return list of all mycore identifiers found in the metadata store
     */
    public abstract List<String> listIDs();

    /**
     * Returns all stored object types of MCRObjects/MCRDerivates.
     *
     * @return collection of object types
     * @see MCRObjectID#getTypeId()
     */
    public abstract Collection<String> getObjectTypes();

    /**
     * Returns all used base ids of MCRObjects/MCRDerivates.
     *
     * @return collection of object types
     * @see MCRObjectID#getBase()
     */
    public abstract Collection<String> getObjectBaseIds();

    /**
     * lists objects and their last modified date.
     */
    public List<MCRObjectIDDate> listObjectDates() throws IOException {
        return retrieveObjectDates(this.listIDs());
    }

    /**
     * lists objects of the specified <code>type</code> and their last modified date.
     * @param type type of object
     */
    public List<MCRObjectIDDate> listObjectDates(String type) throws IOException {
        return retrieveObjectDates(this.listIDsOfType(type));
    }

    /**
     * returns an enhanced list of object ids and their last modified date
     * @param ids MCRObject ids
     * @throws IOException thrown by {@link MCRObjectIDFileSystemDate}
     */
    public abstract List<MCRObjectIDDate> retrieveObjectDates(List<String> ids) throws IOException;

    /**
     * Returns the time the store's content was last modified
     */
    public long getLastModified() {
        return MCRConfigurationBase.getSystemLastModified();
    }

    /**
     * Returns the time when the xml data of a MCRObject was last modified.
     * @return output of {@link MCRStoredMetadata#getLastModified()}
     * @throws IOException thrown by {@link MCRMetadataStore#retrieve(int)}
     */
    public abstract long getLastModified(MCRObjectID id) throws IOException;

    public abstract MCRCache.ModifiedHandle getLastModifiedHandle(MCRObjectID id, long expire, TimeUnit unit);
}
