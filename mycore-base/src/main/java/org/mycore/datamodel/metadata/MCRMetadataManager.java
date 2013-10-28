/*
 * $Id$ $Revision: 5697 $ $Date: 10.09.2010 $
 * 
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 * 
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.mycore.datamodel.metadata;

import static org.mycore.access.MCRAccessManager.PERMISSION_DELETE;
import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRCache.ModifiedHandle;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFileImportExport;
import org.mycore.datamodel.metadata.inheritance.MCRInheritanceManager;
import org.mycore.datamodel.metadata.inheritance.MCRInheritanceManagerFactory;
import org.xml.sax.SAXException;

/**
 * Delivers persistence operations for {@link MCRObject} and {@link MCRDerivate}
 * .
 * 
 * @author Thomas Scheffler (yagee)
 * @since 2.0.92
 */
public final class MCRMetadataManager {

    private static final Logger LOGGER = Logger.getLogger(MCRMetadataManager.class);

    private static final MCRCache<MCRObjectID, MCRObjectID> derivateObjectMap = new MCRCache<>(10000,
        "derivate objectid cache");

    private static final MCRCache<MCRObjectID, List<MCRObjectID>> objectDerivateMap = new MCRCache<>(10000,
        "derivate objectid cache");

    private static MCRXMLMetadataManager XML_MANAGER = MCRXMLMetadataManager.instance();

    private MCRMetadataManager() {

    }

    /**
     * Returns the MCRObjectID of the object containing derivate with the given ID.
     * @param derivateID derivateID
     * @param expire when should lastModified information expire
     * @return null if derivateID has no object referenced
     * @see #getDerivateIds(MCRObjectID, long)
     */
    public static MCRObjectID getObjectId(final MCRObjectID derivateID, final long expire, TimeUnit unit) {
        ModifiedHandle modifiedHandle = XML_MANAGER.getLastModifiedHandle(derivateID, expire, unit);
        MCRObjectID mcrObjectID = null;
        try {
            mcrObjectID = derivateObjectMap.getIfUpToDate(derivateID, modifiedHandle);
        } catch (IOException e) {
            LOGGER.warn("Could not determine last modified timestamp of derivate " + derivateID);
        }
        if (mcrObjectID != null) {
            return mcrObjectID;
        }
        //one cheap db query
        Collection<String> list = MCRLinkTableManager.instance().getSourceOf(derivateID,
            MCRLinkTableManager.ENTRY_TYPE_DERIVATE);
        if (!(list == null || list.isEmpty())) {
            mcrObjectID = MCRObjectID.getInstance(list.iterator().next());
        } else {
            //one expensive process
            if (XML_MANAGER.exists(derivateID)) {
                MCRDerivate d = MCRMetadataManager.retrieveMCRDerivate(derivateID);
                mcrObjectID = d.getOwnerID();
            }
        }
        if (mcrObjectID == null) {
            return null;
        }
        derivateObjectMap.put(derivateID, mcrObjectID);
        return mcrObjectID;
    }

    /**
     * Returns a list of MCRObjectID of the derivates contained in the object with the given ID.
     * @param objectId objectId
     * @param expire when should lastModified information expire
     * @return null if object with objectId does not exist
     * @see #getObjectId(MCRObjectID, long)
     */
    public static List<MCRObjectID> getDerivateIds(final MCRObjectID objectId, final long expire, final TimeUnit unit) {
        ModifiedHandle modifiedHandle = XML_MANAGER.getLastModifiedHandle(objectId, expire, unit);
        List<MCRObjectID> derivateIds = null;
        try {
            derivateIds = objectDerivateMap.getIfUpToDate(objectId, modifiedHandle);
        } catch (IOException e) {
            LOGGER.warn("Could not determine last modified timestamp of derivate " + objectId);
        }
        if (derivateIds != null) {
            return derivateIds;
        }
        Collection<String> destinationOf = MCRLinkTableManager.instance().getDestinationOf(objectId,
            MCRLinkTableManager.ENTRY_TYPE_DERIVATE);
        if (!(destinationOf == null || destinationOf.isEmpty())) {
            derivateIds = new ArrayList<>(destinationOf.size());
            for (String strId : destinationOf) {
                derivateIds.add(MCRObjectID.getInstance(strId));
            }
        } else {
            if (XML_MANAGER.exists(objectId)) {
                MCRObject mcrObject = MCRMetadataManager.retrieveMCRObject(objectId);
                List<MCRMetaLinkID> derivates = mcrObject.getStructure().getDerivates();
                derivateIds = new ArrayList<>(derivates.size());
                for (MCRMetaLinkID der : derivates) {
                    derivateIds.add(der.getXLinkHrefID());
                }
            }
        }
        return derivateIds;
    }

    /**
     * Stores the derivate.
     * 
     * @param mcrDerivate
     *            derivate instance to store
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     */
    public static void create(final MCRDerivate mcrDerivate) throws MCRPersistenceException {
        // exist the derivate?
        if (exists(mcrDerivate.getId())) {
            throw new MCRPersistenceException("The derivate " + mcrDerivate.getId() + " allready exists, nothing done.");
        }

        if (!mcrDerivate.isValid()) {
            throw new MCRPersistenceException("The derivate " + mcrDerivate.getId() + " is not valid.");
        }
        final MCRObjectID objid = mcrDerivate.getDerivate().getMetaLink().getXLinkHrefID();
        byte[] objectBackup;
        try {
            objectBackup = MCRXMLMetadataManager.instance().retrieveBLOB(objid);
            if (objectBackup == null) {
                throw new MCRPersistenceException("Cannot find " + objid + " to attach derivate " + mcrDerivate.getId()
                    + " to it.");
            }
        } catch (IOException e) {
            throw new MCRPersistenceException("The derivate " + mcrDerivate.getId() + " can't find metadata object "
                + objid + ", nothing done.");
        }

        // prepare the derivate metadata and store under the XML table
        if (mcrDerivate.getService().getDate("createdate") == null || !mcrDerivate.isImportMode()) {
            mcrDerivate.getService().setDate("createdate");
        }
        if (mcrDerivate.getService().getDate("modifydate") == null || !mcrDerivate.isImportMode()) {
            mcrDerivate.getService().setDate("modifydate");
        }

        // handle events
        fireEvent(mcrDerivate, MCREvent.CREATE_EVENT);

        // add the link to metadata
        final MCRMetaLinkID der = new MCRMetaLinkID();
        der.setReference(mcrDerivate.getId().toString(), null, mcrDerivate.getLabel());
        der.setSubTag("derobject");

        try {
            LOGGER.debug("adding Derivate in data store");
            MCRMetadataManager.addDerivateToObject(objid, der);
        } catch (final Exception e) {
            MCRMetadataManager.restore(mcrDerivate, objectBackup);
            // throw final exception
            throw new MCRPersistenceException("Error while creatlink to MCRObject " + objid + ".", e);
        }

        // create data in IFS
        if (mcrDerivate.getDerivate().getInternals() != null) {
            if (mcrDerivate.getDerivate().getInternals().getSourcePath() == null) {
                final MCRDirectory difs = new MCRDirectory(mcrDerivate.getId().toString());
                mcrDerivate.getDerivate().getInternals().setIFSID(difs.getID());
            } else {
                final String sourcepath = mcrDerivate.getDerivate().getInternals().getSourcePath();
                final File f = new File(sourcepath);
                if (f.exists()) {
                    MCRDirectory difs = null;
                    try {
                        LOGGER.debug("Starting File-Import");
                        difs = MCRFileImportExport.importFiles(f, mcrDerivate.getId().toString());
                        mcrDerivate.getDerivate().getInternals().setIFSID(difs.getID());
                    } catch (final Exception e) {
                        if (difs != null) {
                            difs.delete();
                        }
                        MCRMetadataManager.restore(mcrDerivate, objectBackup);
                        throw new MCRPersistenceException("Can't add derivate to the IFS", e);
                    }
                } else {
                    LOGGER.warn("Empty derivate, the File or Directory -->" + sourcepath + "<--  was not found.");
                }
            }
        }
    }

    /**
     * Stores the object.
     * 
     * @param mcrObject
     *            object instance to store
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     * @throws MCRActiveLinkException
     *             if current object links to nonexistent
     */
    public static void create(final MCRObject mcrObject) throws MCRPersistenceException {
        // exist the object?
        if (MCRMetadataManager.exists(mcrObject.getId())) {
            throw new MCRPersistenceException("The object " + mcrObject.getId() + " allready exists, nothing done.");
        }

        // create this object in datastore
        if (mcrObject.getService().getDate("createdate") == null) {
            mcrObject.getService().setDate("createdate");
        }
        if (mcrObject.getService().getDate("modifydate") == null) {
            mcrObject.getService().setDate("modifydate");
        }

        // prepare this object with parent metadata
        final MCRObjectID parent_id = mcrObject.getStructure().getParentID();
        MCRObject parent = inheritMetadata(mcrObject, parent_id);

        if (parent_id != null) {
            LOGGER.debug("Parent ID = " + parent_id.toString());

            try {
                parent = MCRMetadataManager.retrieveMCRObject(parent_id);
            } catch (final Exception e) {
                LOGGER.error("Error while merging metadata in this object.", e);
                return;
            }
        }

        // handle events
        fireEvent(mcrObject, MCREvent.CREATE_EVENT);

        // add the MCRObjectID to the child list in the parent object
        if (parent_id != null) {
            try {
                parent.getStructure().addChild(
                    new MCRMetaLinkID("child", mcrObject.getId(), mcrObject.getStructure().getParent().getXLinkLabel(),
                        mcrObject.getLabel()));
                MCRMetadataManager.fireUpdateEvent(parent);
            } catch (final Exception e) {
                LOGGER.error("Error while store child ID in parent object.", e);
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Deletes MCRDerivate.
     * 
     * @param mcrDerivate
     *            to be deleted
     * @throws MCRPersistenceException
     *             if persistence problem occurs
     */
    public static void delete(final MCRDerivate mcrDerivate) throws MCRPersistenceException {
        if (!MCRAccessManager.checkPermission(mcrDerivate.getId(), PERMISSION_DELETE)) {
            throw new MCRPersistenceException("You do not have the permission to delete: " + mcrDerivate.getId());
        }
        // remove link
        MCRObjectID metaId = null;
        try {
            metaId = mcrDerivate.getDerivate().getMetaLink().getXLinkHrefID();
            if (MCRMetadataManager.removeDerivateFromObject(metaId, mcrDerivate.getId())) {
                LOGGER.info(MessageFormat.format("Link in MCRObject {0} to MCRDerivate {1} is deleted.", metaId,
                    mcrDerivate.getId()));
            } else {
                LOGGER.warn(MessageFormat.format("Link in MCRObject {0} to MCRDerivate {1} could not be deleted.",
                    metaId, mcrDerivate.getId()));
            }
        } catch (final Exception e) {
            LOGGER.warn("Can't delete link for MCRDerivate " + mcrDerivate.getId() + " from MCRObject " + metaId
                + ". Error ignored.");
        }

        // delete data from IFS
        try {
            final MCRDirectory difs = MCRDirectory.getRootDirectory(mcrDerivate.getId().toString());
            difs.delete();
            LOGGER.info("IFS entries for MCRDerivate " + mcrDerivate.getId().toString() + " are deleted.");
        } catch (final Exception e) {
            if (mcrDerivate.getDerivate().getInternals() != null) {
                if (LOGGER.isDebugEnabled()) {
                    e.printStackTrace();
                }
                LOGGER.warn("Error while delete for ID " + mcrDerivate.getId().toString() + " from IFS with ID "
                    + mcrDerivate.getDerivate().getInternals().getIFSID());
            }
        }

        // handle events
        fireEvent(mcrDerivate, MCREvent.DELETE_EVENT);
    }

    /**
     * Deletes MCRObject.
     * 
     * @param mcrObject
     *            to be deleted
     * @throws MCRPersistenceException
     *             if persistence problem occurs
     */
    public static void delete(final MCRObject mcrObject) throws MCRPersistenceException, MCRActiveLinkException {
        if (mcrObject.getId() == null) {
            throw new MCRPersistenceException("The MCRObjectID is null.");
        }
        if (!MCRAccessManager.checkPermission(mcrObject.getId(), PERMISSION_DELETE)) {
            throw new MCRPersistenceException("You do not have the permission to delete: " + mcrObject.getId());
        }

        // check for active links
        final Collection<String> sources = MCRLinkTableManager.instance().getSourceOf(mcrObject.mcr_id);
        LOGGER.debug("Sources size:" + sources.size());
        if (sources.size() > 0) {
            final MCRActiveLinkException activeLinks = new MCRActiveLinkException(
                "Error while deleting object "
                    + mcrObject.mcr_id.toString()
                    + ". This object is still referenced by other objects and can not be removed until all links are released.");
            for (final String curSource : sources) {
                activeLinks.addLink(curSource, mcrObject.mcr_id.toString());
            }
            throw activeLinks;
        }

        for (MCRMetaLinkID derivate : mcrObject.getStructure().getDerivates()) {
            try {
                MCRMetadataManager.deleteMCRDerivate(derivate.getXLinkHrefID());
            } catch (final Exception e) {
                LOGGER.error("Error while deleting derivate " + derivate.getXLinkHrefID() + ".", e);
            }
        }

        // remove all children
        for (MCRMetaLinkID child : mcrObject.getStructure().getChildren()) {
            try {
                MCRMetadataManager.deleteMCRObject(child.getXLinkHrefID());
            } catch (final MCRException e) {
                LOGGER.error("Error while deleting child.", e);
            }
        }

        // remove child from parent
        final MCRObjectID parent_id = mcrObject.getStructure().getParentID();

        if (parent_id != null) {
            LOGGER.debug("Parent ID = " + parent_id.toString());

            try {
                final MCRObject parent = MCRMetadataManager.retrieveMCRObject(parent_id);
                parent.getStructure().removeChild(mcrObject.getId());
                MCRMetadataManager.fireUpdateEvent(parent);
            } catch (final Exception e) {
                LOGGER.error("Error while removing child ID in parent object. The parent " + parent_id
                    + "is now inconsistent.", e);
            }
        }

        // handle events
        fireEvent(mcrObject, MCREvent.DELETE_EVENT);
    }

    /**
     * Delete the derivate. The order of delete steps is:<br />
     * <ul>
     * <li>remove link in object metadata</li>
     * <li>remove all files from IFS</li>
     * <li>remove derivate</li>
     * </ul>
     * 
     * @param id
     *            the object ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     */
    public static void deleteMCRDerivate(final MCRObjectID id) throws MCRPersistenceException {
        final MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(id);
        MCRMetadataManager.delete(derivate);
    }

    /**
     * Deletes the object.
     * 
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     * @throws MCRActiveLinkException
     *             if object is referenced by other objects
     */
    public static void deleteMCRObject(final MCRObjectID id) throws MCRPersistenceException, MCRActiveLinkException {
        final MCRObject object = retrieveMCRObject(id);
        MCRMetadataManager.delete(object);
    }

    /**
     * Tells if the object or derivate with <code>id</code> exists.
     * 
     * @param id
     *            the object ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     */
    public static boolean exists(final MCRObjectID id) throws MCRPersistenceException {
        return MCRXMLMetadataManager.instance().exists(id);
    }

    /**
     * Fires {@link MCREvent#REPAIR_EVENT} for given derivate.
     * 
     * @param mcrDerivate
     */
    public static void fireRepairEvent(final MCRDerivate mcrDerivate) throws MCRPersistenceException {
        // handle events
        fireEvent(mcrDerivate, MCREvent.REPAIR_EVENT);
    }

    /**
     * Fires {@link MCREvent#REPAIR_EVENT} for given object.
     * 
     * @param mcrBaseObj
     */
    public static void fireRepairEvent(final MCRBase mcrBaseObj) throws MCRPersistenceException {
        if (mcrBaseObj instanceof MCRDerivate) {
            MCRMetadataManager.fireRepairEvent((MCRDerivate) mcrBaseObj);
        } else if (mcrBaseObj instanceof MCRObject) {
            MCRMetadataManager.fireRepairEvent((MCRObject) mcrBaseObj);
        }
    }

    /**
     * Fires {@link MCREvent#REPAIR_EVENT} for given object.
     * 
     * @param mcrObject
     */
    public static void fireRepairEvent(final MCRObject mcrObject) throws MCRPersistenceException {
        // check derivate link
        for (MCRMetaLinkID derivate : mcrObject.getStructure().getDerivates()) {
            if (!exists(derivate.getXLinkHrefID())) {
                LOGGER.error("Can't find MCRDerivate " + derivate.getXLinkHrefID());
            }
        }
        // handle events
        fireEvent(mcrObject, MCREvent.REPAIR_EVENT);
    }

    /**
     * Fires {@link MCREvent#UPDATE_EVENT} for given object. If
     * {@link MCRObject#isImportMode()} modifydate will not be updated.
     * 
     * @param mcrObject
     *            TODO
     */
    public static void fireUpdateEvent(final MCRObject mcrObject) throws MCRPersistenceException {
        if (!mcrObject.isImportMode() || mcrObject.getService().getDate("modifydate") == null) {
            mcrObject.getService().setDate("modifydate");
        }
        // remove ACL if it is set from data source
        for (int i = 0; i < mcrObject.getService().getRulesSize(); i++) {
            mcrObject.getService().removeRule(i);
            i--;
        }
        // handle events
        fireEvent(mcrObject, MCREvent.UPDATE_EVENT);
    }

    /**
     * Retrieves instance of {@link MCRDerivate} with the given
     * {@link MCRObjectID}
     * 
     * @param id
     *            the derivate ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     */
    public static MCRDerivate retrieveMCRDerivate(final MCRObjectID id) throws MCRPersistenceException {
        try {
            return new MCRDerivate(MCRXMLMetadataManager.instance().retrieveXML(id));
        } catch (IOException | JDOMException | SAXException e) {
            throw new MCRPersistenceException("Could not retrieve xml of derivate: " + id, e);
        }
    }

    /**
     * Retrieves instance of {@link MCRObject} with the given
     * {@link MCRObjectID}
     * 
     * @param id
     *            the object ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     */
    public static MCRObject retrieveMCRObject(final MCRObjectID id) throws MCRPersistenceException {
        try {
            return new MCRObject(MCRXMLMetadataManager.instance().retrieveXML(id));
        } catch (IOException | JDOMException | SAXException e) {
            throw new MCRPersistenceException("Could not retrieve xml of object: " + id, e);
        }
    }

    /**
     * Retrieves instance of {@link MCRObject} or {@link MCRDerivate} depending
     * on {@link MCRObjectID#getTypeId()}
     * 
     * @param id
     *            derivate or object id
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     */
    public static MCRBase retrieve(final MCRObjectID id) throws MCRPersistenceException {
        if (id.getTypeId().equals("derivate")) {
            return retrieveMCRDerivate(id);
        }
        return retrieveMCRObject(id);
    }

    /**
     * Updates the derivate or creates it if it does not exist yet.
     * 
     * @param mcrDerivate
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     */
    public static void update(final MCRDerivate mcrDerivate) throws MCRPersistenceException {
        if (!MCRMetadataManager.exists(mcrDerivate.getId())) {
            MCRMetadataManager.create(mcrDerivate);
            return;
        }
        if (!MCRAccessManager.checkPermission(mcrDerivate.getId(), PERMISSION_WRITE)) {
            throw new MCRPersistenceException("You do not have the permission to update: " + mcrDerivate.getId());
        }
        File fileSourceDirectory = null;
        if (mcrDerivate.getDerivate().getInternals() != null
            && mcrDerivate.getDerivate().getInternals().getSourcePath() != null) {
            fileSourceDirectory = new File(mcrDerivate.getDerivate().getInternals().getSourcePath());

            if (!fileSourceDirectory.exists()) {
                throw new MCRPersistenceException("The directory " + fileSourceDirectory + " was not found.");
            }
        }
        // get the old Item
        MCRDerivate old = MCRMetadataManager.retrieveMCRDerivate(mcrDerivate.getId());

        // remove the old link to metadata
        MCRObjectID oldMetadataObjectID = old.getDerivate().getMetaLink().getXLinkHrefID();
        MCRObjectID newMetadataObjectID = mcrDerivate.getDerivate().getMetaLink().getXLinkHrefID();
        if (!newMetadataObjectID.equals(oldMetadataObjectID)) {
            try {
                MCRMetadataManager.removeDerivateFromObject(oldMetadataObjectID, mcrDerivate.getId());
            } catch (final MCRException e) {
                LOGGER.warn(e.getMessage(), e);
            }
            // add the link to metadata
            final MCRMetaLinkID der = new MCRMetaLinkID("derobject", mcrDerivate.getId(), null, mcrDerivate.getLabel());
            MCRMetadataManager.addDerivateToObject(newMetadataObjectID, der);
        }
        // update the derivate
        mcrDerivate.getService().setDate("createdate", old.getService().getDate("createdate"));
        MCRMetadataManager.updateMCRDerivateXML(mcrDerivate);

        // update to IFS
        if (fileSourceDirectory != null) {
            try {
                final MCRDirectory difs = MCRDirectory.getRootDirectory(mcrDerivate.getId().toString());
                MCRFileImportExport.importFiles(fileSourceDirectory, difs);
            } catch (final Exception e) {
                LOGGER.error("Exception while importing files from " + fileSourceDirectory, e);
            }
        }

    }

    /**
     * Updates the object or creates it if it does not exist yet.
     * 
     * @param mcrObject
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     * @throws MCRActiveLinkException
     *             if object is created (no real update), see
     *             {@link #create(MCRObject)}
     */
    public static void update(final MCRObject mcrObject) throws MCRPersistenceException, MCRActiveLinkException {
        if (!MCRMetadataManager.exists(mcrObject.getId())) {
            MCRMetadataManager.create(mcrObject);
            return;
        }
        if (!MCRAccessManager.checkPermission(mcrObject.getId(), PERMISSION_WRITE)) {
            throw new MCRPersistenceException("You do not have the permission to update: " + mcrObject.getId());
        }
        MCRObject old = MCRMetadataManager.retrieveMCRObject(mcrObject.getId());

        // clean the structure
        mcrObject.getStructure().clearChildren();
        mcrObject.getStructure().clearDerivates();

        // set the derivate data in structure
        mcrObject.getStructure().getDerivates().addAll(old.getStructure().getDerivates());

        // set the parent from the original and this update
        boolean setparent = false;

        MCRObjectID oldParentID = old.getStructure().getParentID();
        MCRObjectID newParentID = mcrObject.getStructure().getParentID();

        if (oldParentID != null && (newParentID == null || !newParentID.equals(oldParentID))) {
            // remove child from the old parent
            LOGGER.debug("Parent ID = " + oldParentID);
            try {
                final MCRObject parent = MCRMetadataManager.retrieveMCRObject(oldParentID);
                parent.getStructure().removeChild(mcrObject.getId());
                MCRMetadataManager.fireUpdateEvent(parent);
                setparent = true;
            } catch (final Exception e) {
                LOGGER.debug(MCRException.getStackTraceAsString(e));
                LOGGER.error("Error while delete child ID in parent object.");
                LOGGER.warn("Attention, the parent " + oldParentID + "is now inconsist.");
            }
        } else if (oldParentID == null && newParentID != null) {
            setparent = true;
        }

        // set the children from the original
        mcrObject.getStructure().getChildren().addAll(old.getStructure().getChildren());

        // import all herited matadata from the parent
        MCRObject newParent = inheritMetadata(mcrObject, newParentID);

        // if not imported via cli, createdate remains unchanged
        if (!mcrObject.isImportMode() || mcrObject.getService().getDate("createdate") == null) {
            mcrObject.getService().setDate("createdate", old.getService().getDate("createdate"));
        }

        // update this dataset
        MCRMetadataManager.fireUpdateEvent(mcrObject);

        // check if the parent was new set and set them
        if (setparent) {
            try {
                newParent.getStructure().addChild(
                    new MCRMetaLinkID("child", mcrObject.getId(), null, mcrObject.getLabel()));
                MCRMetadataManager.fireUpdateEvent(newParent);
            } catch (final Exception e) {
                LOGGER.error("Error while store child ID in parent object.", e);
                throw new RuntimeException(e);
            }
        }

        // update all children
        boolean updatechildren = inheritableMetadataChanged(mcrObject, old);
        if (updatechildren) {
            MCRInheritanceManager inheritanceManager = MCRInheritanceManagerFactory.getManager(mcrObject.getId());
            inheritanceManager.inheritMetadata(mcrObject);
        }
    }

    private static boolean inheritableMetadataChanged(final MCRObject mcrObject, MCRObject old) {
        MCRInheritanceManager inheritanceManager = MCRInheritanceManagerFactory.getManager(mcrObject.getId());
        return inheritanceManager.inheritableMetadataChanged(old, mcrObject);
    }

    private static MCRObject inheritMetadata(final MCRObject mcrObject, final MCRObjectID parentID) {
        //TODO: handle inheritance of mycore-mods in that component
        MCRObject parent = null;
        if (parentID != null) {
            LOGGER.debug("Parent ID = " + parentID);
            try {
                parent = MCRMetadataManager.retrieveMCRObject(parentID);
                MCRInheritanceManager inheritanceManager = MCRInheritanceManagerFactory.getManager(parentID);
                inheritanceManager.inheritMetadata(parent, mcrObject);
            } catch (final Exception e) {
                LOGGER.error("Error while merging metadata in this object.", e);
            }
        }
        return parent;
    }

    /**
     * Updates only the XML part of the derivate.
     * 
     * @param mcrDerivate
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     */
    public static void updateMCRDerivateXML(final MCRDerivate mcrDerivate) throws MCRPersistenceException {
        if (!mcrDerivate.isImportMode() || mcrDerivate.getService().getDate("modifydate") == null) {
            mcrDerivate.getService().setDate("modifydate");
        }
        fireEvent(mcrDerivate, MCREvent.UPDATE_EVENT);
    }

    /**
     * Adds a derivate MCRMetaLinkID to the structure part and updates the
     * object with the ID in the data store.
     * 
     * @param id
     *            the object ID
     * @param link
     *            a link to a derivate as MCRMetaLinkID
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     */
    public static void addDerivateToObject(final MCRObjectID id, final MCRMetaLinkID link)
        throws MCRPersistenceException {
        final MCRObject object = MCRMetadataManager.retrieveMCRObject(id);
        // don't put the same derivates twice in an object!
        if (!object.getStructure().addDerivate(link))
            return;
        // add link
        if (!object.isImportMode()) {
            object.getService().setDate("modifydate");
        }
        object.getStructure().addDerivate(link);
        MCRMetadataManager.fireUpdateEvent(object);
    }

    public static boolean removeDerivateFromObject(final MCRObjectID objectID, final MCRObjectID derivateID)
        throws MCRPersistenceException {
        final MCRObject object = MCRMetadataManager.retrieveMCRObject(objectID);
        if (object.getStructure().removeDerivate(derivateID)) {
            object.getService().setDate("modifydate");
            MCRMetadataManager.fireUpdateEvent(object);
            return true;
        }
        return false;
    }

    private static void restore(final MCRDerivate mcrDerivate, final byte[] backup) {
        // restore original instance of MCRObject
        final MCRObject obj = new MCRObject();
        try {
            obj.setFromXML(backup, false);
            MCRMetadataManager.update(obj);
        } catch (final Exception e1) {
            LOGGER.warn("Error while restoring " + obj.getId(), e1);
        } finally {
            // delete from the XML table
            // handle events
            fireEvent(mcrDerivate, MCREvent.DELETE_EVENT);
        }
    }

    private static void fireEvent(MCRBase base, String eventType) {
        boolean objectEvent = base instanceof MCRObject;
        String type = objectEvent ? MCREvent.OBJECT_TYPE : MCREvent.DERIVATE_TYPE;
        final MCREvent evt = new MCREvent(type, eventType);
        if (objectEvent) {
            evt.put("object", base);
        } else {
            evt.put("derivate", base);
        }
        if (MCREvent.DELETE_EVENT.equals(eventType)) {
            MCREventManager.instance().handleEvent(evt, MCREventManager.BACKWARD);
        } else {
            MCREventManager.instance().handleEvent(evt);
        }
    }
}
