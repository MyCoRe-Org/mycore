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

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationBase;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Provides an adapter to communicate with the configured {@link MCRXMLMetadataManagerAdapter} implementation.
 *
 * @author Christoph Neidahl (OPNA2608)
 */
public class MCRXMLMetadataManager {

    /**
     * Our own singleton.
     */
    private static MCRXMLMetadataManager SINGLETON;

    /**
     * The implementation's singleton.
     */
    private static MCRXMLMetadataManagerAdapter IMPLEMENTATION;

    /**
     * Reads the MCR.Metadata.Manager.Class to instantiate and return the configured xml metadata manager.
     * If MCR.Metadata.Manager.Class is not set, an instance of {@link MCRDefaultXMLMetadataManager} is returned.
     *
     * @return an instance of the configured xml metadata manager if any is set, or MCRDefaultXMLMetadataManager
     */
    public static synchronized MCRXMLMetadataManager instance() {
        if (SINGLETON == null) {
            SINGLETON = new MCRXMLMetadataManager();
        }
        if (IMPLEMENTATION == null) {
            IMPLEMENTATION = MCRConfiguration2
                .getSingleInstanceOf("MCR.Metadata.Manager", MCRDefaultXMLMetadataManager.class).get();
        }
        return SINGLETON;
    }

    /*
     * Delegations to IMPLEMENTATION
     */

    /**
     * Delegation, see linked method for relevant documentation.
     *
     * @see MCRXMLMetadataManagerAdapter#reload()
     */
    public void reload() {
        IMPLEMENTATION.reload();
    }

    /**
     * Delegation, see linked method for relevant documentation.
     *
     * @see MCRXMLMetadataManagerAdapter#verifyStore(String)
     */
    public void verifyStore(String base) {
        IMPLEMENTATION.verifyStore(base);
    }

    /**
     * Delegation, see linked method for relevant documentation.
     *
     * @see MCRXMLMetadataManagerAdapter#create(MCRObjectID, MCRContent, Date)
     */
    public void create(MCRObjectID mcrid, MCRContent xml, Date lastModified)
        throws MCRPersistenceException {
        IMPLEMENTATION.create(mcrid, xml, lastModified);
    }

    /**
     * Delegation, see linked method for relevant documentation.
     *
     * @see MCRXMLMetadataManagerAdapter#delete(MCRObjectID)
     */
    public void delete(MCRObjectID mcrid) throws MCRPersistenceException {
        IMPLEMENTATION.delete(mcrid);
    }

    /**
     * Delegation, see linked method for relevant documentation.
     *
     * @see MCRXMLMetadataManagerAdapter#update(MCRObjectID, MCRContent, Date)
     */
    public void update(MCRObjectID mcrid, MCRContent xml, Date lastModified)
        throws MCRPersistenceException {
        IMPLEMENTATION.update(mcrid, xml, lastModified);
    }

    /**
     * Delegation, see linked method for relevant documentation.
     *
     * @see MCRXMLMetadataManagerAdapter#retrieveContent(MCRObjectID)
     */
    public MCRContent retrieveContent(MCRObjectID mcrid) throws IOException {
        return IMPLEMENTATION.retrieveContent(mcrid);
    }

    /**
     * Delegation, see linked method for relevant documentation.
     *
     * @see MCRXMLMetadataManagerAdapter#retrieveContent(MCRObjectID, String)
     */
    public MCRContent retrieveContent(MCRObjectID mcrid, String revision) throws IOException {
        return IMPLEMENTATION.retrieveContent(mcrid, revision);
    }

    /**
     * Delegation, see linked method for relevant documentation.
     *
     * @see MCRXMLMetadataManagerAdapter#listRevisions(MCRObjectID)
     */
    public List<? extends MCRAbstractMetadataVersion<?>> listRevisions(MCRObjectID id) throws IOException {
        return IMPLEMENTATION.listRevisions(id);
    }

    /**
     * Delegation, see linked method for relevant documentation.
     *
     * @see MCRXMLMetadataManagerAdapter#getHighestStoredID(String, String)
     */
    public int getHighestStoredID(String project, String type) {
        return IMPLEMENTATION.getHighestStoredID(project, type);
    }

    public int getHighestStoredID(String base) {
        return IMPLEMENTATION.getHighestStoredID(
            base.substring(0, base.indexOf("_")),
            base.substring(base.indexOf("_") + 1));
    }

    /**
     * Delegation, see linked method for relevant documentation.
     *
     * @see MCRXMLMetadataManagerAdapter#exists(MCRObjectID)
     */
    public boolean exists(MCRObjectID mcrid) throws MCRPersistenceException {
        return IMPLEMENTATION.exists(mcrid);
    }

    /**
     * Delegation, see linked method for relevant documentation.
     *
     * @see MCRXMLMetadataManagerAdapter#listIDsForBase(String)
     */
    public List<String> listIDsForBase(String base) {
        return IMPLEMENTATION.listIDsForBase(base);
    }

    /**
     * Delegation, see linked method for relevant documentation.
     *
     * @see MCRXMLMetadataManagerAdapter#listIDsOfType(String)
     */
    public List<String> listIDsOfType(String type) {
        return IMPLEMENTATION.listIDsOfType(type);
    }

    /**
     * Delegation, see linked method for relevant documentation.
     *
     * @see MCRXMLMetadataManagerAdapter#listIDs()
     */
    public List<String> listIDs() {
        return IMPLEMENTATION.listIDs();
    }

    /**
     * Delegation, see linked method for relevant documentation.
     *
     * @see MCRXMLMetadataManagerAdapter#getObjectTypes()
     */
    public Collection<String> getObjectTypes() {
        return IMPLEMENTATION.getObjectTypes();
    }

    /**
     * Delegation, see linked method for relevant documentation.
     *
     * @see MCRXMLMetadataManagerAdapter#getObjectBaseIds()
     */
    public Collection<String> getObjectBaseIds() {
        return IMPLEMENTATION.getObjectBaseIds();
    }

    /**
     * Delegation, see linked method for relevant documentation.
     *
     * @see MCRXMLMetadataManagerAdapter#retrieveObjectDates(List)
     */
    public List<MCRObjectIDDate> retrieveObjectDates(List<String> ids) throws IOException {
        return IMPLEMENTATION.retrieveObjectDates(ids);
    }

    /**
     * Delegation, see linked method for relevant documentation.
     *
     * @see MCRXMLMetadataManagerAdapter#getLastModified(MCRObjectID)
     */
    public long getLastModified(MCRObjectID id) throws IOException {
        return IMPLEMENTATION.getLastModified(id);
    }

    /**
     * Delegation, see linked method for relevant documentation.
     *
     * @see MCRXMLMetadataManagerAdapter#getLastModifiedHandle(MCRObjectID, long, TimeUnit)
     */
    public MCRCache.ModifiedHandle getLastModifiedHandle(MCRObjectID id, long expire, TimeUnit unit) {
        return IMPLEMENTATION.getLastModifiedHandle(id, expire, unit);
    }

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
    public void create(MCRObjectID mcrid, Document xml, Date lastModified)
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
    public void create(MCRObjectID mcrid, byte[] xml, Date lastModified) throws MCRPersistenceException {
        create(mcrid, new MCRByteContent(xml, lastModified.getTime()), lastModified);
    }

    public void delete(String mcrid) throws MCRPersistenceException {
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
    public void update(MCRObjectID mcrid, Document xml, Date lastModified)
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
    public void createOrUpdate(MCRObjectID mcrid, Document xml, Date lastModified)
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
    public void update(MCRObjectID mcrid, byte[] xml, Date lastModified) throws MCRPersistenceException {
        update(mcrid, new MCRByteContent(xml, lastModified.getTime()), lastModified);
    }

    /**
     * Retrieves stored metadata xml as JDOM document
     *
     * @param mcrid the MCRObjectID
     * @return null if metadata is not present
     */
    public Document retrieveXML(MCRObjectID mcrid) throws IOException, JDOMException {
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

    /**
     * Lists all objects with their last modification dates.
     *
     * @return List of {@link MCRObjectIDDate}
     */
    public List<MCRObjectIDDate> listObjectDates() throws IOException {
        return retrieveObjectDates(this.listIDs());
    }

    /**
     * Lists all objects of the specified <code>type</code> and their last modified date.
     *
     * @param type type of object
     */
    public List<MCRObjectIDDate> listObjectDates(String type) throws IOException {
        return retrieveObjectDates(this.listIDsOfType(type));
    }

    /**
     * Returns the time the store's content was last modified
     *
     * @return Last modification date of the MyCoRe system
     */
    public long getLastModified() {
        return MCRConfigurationBase.getSystemLastModified();
    }
}
