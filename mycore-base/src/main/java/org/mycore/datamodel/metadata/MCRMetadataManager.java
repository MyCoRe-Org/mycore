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
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.persistence.PersistenceException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRCache.ModifiedHandle;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.common.MCRMarkManager;
import org.mycore.datamodel.common.MCRMarkManager.Operation;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.share.MCRMetadataShareAgent;
import org.mycore.datamodel.metadata.share.MCRMetadataShareAgentFactory;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.utils.MCRRecursiveDeleter;
import org.mycore.datamodel.niofs.utils.MCRTreeCopier;
import org.xml.sax.SAXException;

/**
 * Delivers persistence operations for {@link MCRObject} and {@link MCRDerivate} .
 * 
 * @author Thomas Scheffler (yagee)
 * @since 2.0.92
 */
public final class MCRMetadataManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MCRCache<MCRObjectID, MCRObjectID> derivateObjectMap = new MCRCache<>(10000,
        "derivate objectid cache");

    private static final MCRCache<MCRObjectID, List<MCRObjectID>> objectDerivateMap = new MCRCache<>(10000,
        "derivate objectid cache");

    private static MCRXMLMetadataManager XML_MANAGER = MCRXMLMetadataManager.instance();

    private MCRMetadataManager() {

    }

    /**
     * Returns the MCRObjectID of the object containing derivate with the given ID.
     * 
     * @param derivateID
     *            derivateID
     * @param expire
     *            when should lastModified information expire
     * @return null if derivateID has no object referenced
     * @see #getDerivateIds(MCRObjectID, long, TimeUnit)
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
            if (exists(derivateID)) {
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
     * 
     * @param objectId
     *            objectId
     * @param expire
     *            when should lastModified information expire
     * @return null if object with objectId does not exist
     * @see #getObjectId(MCRObjectID, long, TimeUnit)
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
            if (exists(objectId)) {
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
     * @throws MCRAccessException if write permission to object is missing
     */
    public static void create(final MCRDerivate mcrDerivate)
        throws MCRPersistenceException, IOException, MCRAccessException {
        // exist the derivate?
        if (exists(mcrDerivate.getId())) {
            throw new MCRPersistenceException(
                "The derivate " + mcrDerivate.getId() + " allready exists, nothing done.");
        }

        try {
            mcrDerivate.validate();
        } catch (MCRException exc) {
            throw new MCRPersistenceException("The derivate " + mcrDerivate.getId() + " is not valid.", exc);
        }
        final MCRObjectID objid = mcrDerivate.getDerivate().getMetaLink().getXLinkHrefID();
        if (!MCRAccessManager.checkPermission(objid, PERMISSION_WRITE)) {
            throw MCRAccessException.missingPermission("Add derivate " + mcrDerivate.getId() + " to object.",
                objid.toString(),
                PERMISSION_WRITE);
        }
        byte[] objectBackup;
        objectBackup = MCRXMLMetadataManager.instance().retrieveBLOB(objid);
        if (objectBackup == null) {
            throw new MCRPersistenceException(
                "Cannot find " + objid + " to attach derivate " + mcrDerivate.getId() + " to it.");
        }

        // prepare the derivate metadata and store under the XML table
        if (mcrDerivate.getService().getDate("createdate") == null || !mcrDerivate.isImportMode()) {
            mcrDerivate.getService().setDate("createdate");
        }
        if (mcrDerivate.getService().getDate("modifydate") == null || !mcrDerivate.isImportMode()) {
            mcrDerivate.getService().setDate("modifydate");
        }

        // handle events
        fireEvent(mcrDerivate, null, MCREvent.CREATE_EVENT);

        // add the link to metadata
        final MCRMetaLinkID der = new MCRMetaLinkID();
        der.setReference(mcrDerivate.getId().toString(), null, mcrDerivate.getLabel());
        der.setSubTag("derobject");

        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("adding Derivate in data store");
            }
            MCRMetadataManager.addOrUpdateDerivateToObject(objid, der);
        } catch (final Exception e) {
            MCRMetadataManager.restore(mcrDerivate, objid, objectBackup);
            // throw final exception
            throw new MCRPersistenceException("Error while creatlink to MCRObject " + objid + ".", e);
        }

        // create data in IFS
        if (mcrDerivate.getDerivate().getInternals() != null) {
            MCRObjectID derId = mcrDerivate.getId();
            MCRPath rootPath = MCRPath.getPath(derId.toString(), "/");
            if (mcrDerivate.getDerivate().getInternals().getSourcePath() == null) {
                rootPath.getFileSystem().createRoot(rootPath.getOwner());
                BasicFileAttributes attrs = Files.readAttributes(rootPath, BasicFileAttributes.class);
                if (!(attrs.fileKey() instanceof String)) {
                    throw new MCRPersistenceException(
                        "Cannot get ID from newely created directory, as it is not a String." + rootPath);
                }
                mcrDerivate.getDerivate().getInternals().setIFSID(attrs.fileKey().toString());
            } else {
                final String sourcepath = mcrDerivate.getDerivate().getInternals().getSourcePath();
                final File f = new File(sourcepath);
                if (f.exists()) {
                    try {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Starting File-Import");
                        }
                        importDerivate(derId.toString(), f.toPath());
                        BasicFileAttributes attrs = Files.readAttributes(rootPath, BasicFileAttributes.class);
                        if (!(attrs.fileKey() instanceof String)) {
                            throw new MCRPersistenceException(
                                "Cannot get ID from newely created directory, as it is not a String." + rootPath);
                        }
                        mcrDerivate.getDerivate().getInternals().setIFSID(attrs.fileKey().toString());
                    } catch (final Exception e) {
                        if (Files.exists(rootPath)) {
                            deleteDerivate(derId.toString());
                        }
                        MCRMetadataManager.restore(mcrDerivate, objid, objectBackup);
                        throw new MCRPersistenceException("Can't add derivate to the IFS", e);
                    }
                } else {
                    LOGGER.warn("Empty derivate, the File or Directory -->" + sourcepath + "<--  was not found.");
                }
            }
        }
    }

    private static void deleteDerivate(String derivateID) throws IOException {
        MCRPath rootPath = MCRPath.getPath(derivateID, "/");
        if (!Files.exists(rootPath)) {
            LOGGER.info("Derivate does not exist: " + derivateID);
            return;
        }
        Files.walkFileTree(rootPath, MCRRecursiveDeleter.instance());
        rootPath.getFileSystem().removeRoot(derivateID);
    }

    private static void importDerivate(String derivateID, Path sourceDir) throws NoSuchFileException, IOException {
        MCRPath rootPath = MCRPath.getPath(derivateID, "/");
        if (Files.exists(rootPath)) {
            LOGGER.info("Derivate does already exist: " + derivateID);
        }
        rootPath.getFileSystem().createRoot(derivateID);
        Files.walkFileTree(sourceDir, new MCRTreeCopier(sourceDir, rootPath));
    }

    /**
     * Stores the object.
     * 
     * @param mcrObject
     *            object instance to store
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     * @throws MCRAccessException if "create-{objectType}" privilege is missing
     */
    public static void create(final MCRObject mcrObject) throws MCRPersistenceException, MCRAccessException {
        String createBasePrivilege = "create-" + mcrObject.getId().getBase();
        String createTypePrivilege = "create-" + mcrObject.getId().getTypeId();
        if (!MCRAccessManager.checkPermission(createBasePrivilege)
            && !MCRAccessManager.checkPermission(createTypePrivilege)) {
            throw MCRAccessException.missingPrivilege("Create object with id " + mcrObject.getId(), createBasePrivilege,
                createTypePrivilege);
        }
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
        receiveMetadata(mcrObject);

        final MCRObjectID parent_id = mcrObject.getStructure().getParentID();
        MCRObject parent = null;
        if (parent_id != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Parent ID = " + parent_id.toString());
            }
            parent = MCRMetadataManager.retrieveMCRObject(parent_id);
        }

        // handle events
        fireEvent(mcrObject, null, MCREvent.CREATE_EVENT);

        // add the MCRObjectID to the child list in the parent object
        if (parent_id != null) {
            parent.getStructure().addChild(new MCRMetaLinkID("child", mcrObject.getId(),
                mcrObject.getStructure().getParent().getXLinkLabel(), mcrObject.getLabel()));
            MCRMetadataManager.fireUpdateEvent(parent);
        }
    }

    /**
     * Deletes MCRDerivate.
     * 
     * @param mcrDerivate
     *            to be deleted
     * @throws MCRPersistenceException
     *             if persistence problem occurs
     * @throws MCRAccessException if delete permission is missing
     */
    public static void delete(final MCRDerivate mcrDerivate) throws MCRPersistenceException, MCRAccessException {
        MCRObjectID id = mcrDerivate.getId();
        if (!MCRAccessManager.checkPermission(id, PERMISSION_DELETE)) {
            throw MCRAccessException.missingPermission("Delete derivate", id.toString(), PERMISSION_DELETE);
        }
        // mark for deletion
        MCRMarkManager.instance().mark(id, Operation.DELETE);

        // remove link
        MCRObjectID metaId = null;
        try {
            metaId = mcrDerivate.getDerivate().getMetaLink().getXLinkHrefID();
            if (MCRMetadataManager.removeDerivateFromObject(metaId, id)) {
                LOGGER.info(MessageFormat.format("Link in MCRObject {0} to MCRDerivate {1} is deleted.", metaId, id));
            } else {
                LOGGER.warn(
                    MessageFormat.format("Link in MCRObject {0} to MCRDerivate {1} could not be deleted.", metaId, id));
            }
        } catch (final Exception e) {
            LOGGER.warn("Can't delete link for MCRDerivate " + id + " from MCRObject " + metaId + ". Error ignored.");
        }

        // delete data from IFS
        if (mcrDerivate.getDerivate().getInternals() != null) {
            try {
                deleteDerivate(id.toString());
                LOGGER.info("IFS entries for MCRDerivate " + id.toString() + " are deleted.");
            } catch (final Exception e) {
                throw new MCRPersistenceException("Error while delete MCRDerivate " + id.toString() + " in IFS", e);
            }
        }

        // handle events
        fireEvent(mcrDerivate, null, MCREvent.DELETE_EVENT);

        // remove mark
        MCRMarkManager.instance().remove(id);
    }

    /**
     * Deletes MCRObject.
     * 
     * @param mcrObject
     *            to be deleted
     * @throws MCRActiveLinkException 
     * @throws MCRPersistenceException
     *             if persistence problem occurs
     * @throws MCRAccessException if delete permission is missing
     */
    public static void delete(final MCRObject mcrObject)
        throws MCRPersistenceException, MCRActiveLinkException, MCRAccessException {
        delete(mcrObject, MCRMetadataManager::removeChildObject);
    }

    /**
     * Deletes the <code>mcrObject</code>.
     * 
     * @param mcrObject
     *            the object to be deleted
     * @param parentOperation
     *            function to handle the parent of the object @see {@link #removeChildObject(MCRObject, MCRObjectID)}
     * @throws MCRPersistenceException
     *            if persistence problem occurs
     * @throws MCRActiveLinkException
     *            object couldn't  be deleted because its linked somewhere
     * @throws MCRAccessException if delete permission is missing
     */
    private static void delete(final MCRObject mcrObject, BiConsumer<MCRObject, MCRObjectID> parentOperation)
        throws MCRPersistenceException, MCRActiveLinkException, MCRAccessException {
        MCRObjectID id = mcrObject.getId();
        if (id == null) {
            throw new MCRPersistenceException("The MCRObjectID is null.");
        }
        if (!MCRAccessManager.checkPermission(id, PERMISSION_DELETE)) {
            throw MCRAccessException.missingPermission("Delete object", mcrObject.getId().toString(),
                PERMISSION_DELETE);
        }

        // check for active links
        final Collection<String> sources = MCRLinkTableManager.instance().getSourceOf(mcrObject.mcr_id,
            MCRLinkTableManager.ENTRY_TYPE_REFERENCE);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Sources size:" + sources.size());
        }
        if (sources.size() > 0) {
            final MCRActiveLinkException activeLinks = new MCRActiveLinkException("Error while deleting object " + id
                + ". This object is still referenced by other objects and can not be removed until all links are released.");
            for (final String curSource : sources) {
                activeLinks.addLink(curSource, id.toString());
            }
            throw activeLinks;
        }

        // mark for deletion
        MCRMarkManager.instance().mark(id, Operation.DELETE);

        // remove child from parent
        final MCRObjectID parentId = mcrObject.getStructure().getParentID();
        if (parentId != null) {
            parentOperation.accept(mcrObject, parentId);
        }

        // remove all children
        for (MCRMetaLinkID child : mcrObject.getStructure().getChildren()) {
            MCRObjectID childId = child.getXLinkHrefID();
            if (!MCRMetadataManager.exists(childId)) {
                LOGGER.warn("Unable to remove not existing object " + childId + " of parent " + id);
                continue;
            }
            MCRMetadataManager.deleteMCRObject(childId, (MCRObject o, MCRObjectID p) -> {
                /**
                 * Do nothing with the parent, because its removed anyway.
                 */
            });
        }

        // remove all derivates
        for (MCRMetaLinkID derivate : mcrObject.getStructure().getDerivates()) {
            MCRObjectID derivateId = derivate.getXLinkHrefID();
            if (!MCRMetadataManager.exists(derivateId)) {
                LOGGER.warn("Unable to remove not existing derivate " + derivateId + " of object " + id);
                continue;
            }
            MCRMetadataManager.deleteMCRDerivate(derivateId);
        }

        // handle events
        fireEvent(mcrObject, null, MCREvent.DELETE_EVENT);

        // remove mark
        MCRMarkManager.instance().remove(id);
    }

    /**
     * Helper method to remove the <code>mcrObject</code> of the given parent. This does just
     * remove the linking between both objects.
     * 
     * @param mcrObject
     *            the object (child) to remove
     * @param
     *            parentId the parent id
     * @throws PersistenceException
     *            when the child cannot be removed due persistent problems
     */
    private static void removeChildObject(final MCRObject mcrObject, final MCRObjectID parentId)
        throws PersistenceException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Parent ID = " + parentId.toString());
        }
        try {
            if (MCRXMLMetadataManager.instance().exists(parentId)) {
                final MCRObject parent = MCRMetadataManager.retrieveMCRObject(parentId);
                parent.getStructure().removeChild(mcrObject.getId());
                MCRMetadataManager.fireUpdateEvent(parent);
            } else {
                LOGGER.warn("Unable to find parent " + parentId + " of " + mcrObject.getId());
            }
        } catch (Exception exc) {
            throw new PersistenceException(
                "Error while deleting object. Unable to remove child " + mcrObject.getId() + " from parent " + parentId
                    + ".",
                exc);
        }
    }

    /**
     * Delete the derivate. The order of delete steps is:<br>
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
     * @throws MCRAccessException if delete permission is missing
     */
    public static void deleteMCRDerivate(final MCRObjectID id) throws MCRPersistenceException, MCRAccessException {
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
     * @throws MCRAccessException if delete permission is missing
     */
    public static void deleteMCRObject(final MCRObjectID id)
        throws MCRPersistenceException, MCRActiveLinkException, MCRAccessException {
        deleteMCRObject(id, MCRMetadataManager::removeChildObject);
    }

    /**
     * Deletes the mcr object with the given <code>id</code>.
     * 
     * @param id
     *            the object to be deleted
     * @param parentOperation
     *            function to handle the parent of the object @see {@link #removeChildObject(MCRObject, MCRObjectID)}
     * @throws MCRPersistenceException
     *            if persistence problem occurs
     * @throws MCRActiveLinkException
     *            object couldn't  be deleted because its linked somewhere
     * @throws MCRAccessException if delete permission is missing
     */
    private static void deleteMCRObject(final MCRObjectID id, BiConsumer<MCRObject, MCRObjectID> parentOperation)
        throws MCRPersistenceException, MCRActiveLinkException, MCRAccessException {
        final MCRObject object = retrieveMCRObject(id);
        MCRMetadataManager.delete(object, parentOperation);
    }

    /**
     * Tells if the object or derivate with <code>id</code> exists.
     * 
     * @param id
     *            the object ID
     * @throws MCRPersistenceException 
     */
    public static boolean exists(final MCRObjectID id) {
        try {
            return MCRXMLMetadataManager.instance().exists(id);
        } catch (IOException e) {
            throw new MCRPersistenceException("Error while checking existence of " + id, e);
        }
    }

    /**
     * Fires {@link MCREvent#REPAIR_EVENT} for given derivate.
     * 
     */
    public static void fireRepairEvent(final MCRDerivate mcrDerivate) throws MCRPersistenceException {
        // handle events
        fireEvent(mcrDerivate, null, MCREvent.REPAIR_EVENT);
    }

    /**
     * Fires {@link MCREvent#REPAIR_EVENT} for given object.
     * 
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
     */
    public static void fireRepairEvent(final MCRObject mcrObject) throws MCRPersistenceException {
        // check derivate link
        for (MCRMetaLinkID derivate : mcrObject.getStructure().getDerivates()) {
            if (!exists(derivate.getXLinkHrefID())) {
                LOGGER.error("Can't find MCRDerivate " + derivate.getXLinkHrefID());
            }
        }
        // handle events
        fireEvent(mcrObject, null, MCREvent.REPAIR_EVENT);
    }

    /**
     * Fires {@link MCREvent#UPDATE_EVENT} for given object. If {@link MCRObject#isImportMode()} modifydate will not be
     * updated.
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
        fireEvent(mcrObject, retrieveMCRObject(mcrObject.getId()), MCREvent.UPDATE_EVENT);
    }

    /**
     * Retrieves instance of {@link MCRDerivate} with the given {@link MCRObjectID}
     * 
     * @param id
     *            the derivate ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     */
    public static MCRDerivate retrieveMCRDerivate(final MCRObjectID id) throws MCRPersistenceException {
        try {
            Document xml = MCRXMLMetadataManager.instance().retrieveXML(id);
            if (xml == null) {
                throw new MCRPersistenceException("Could not retrieve xml of derivate: " + id);
            }
            return new MCRDerivate(xml);
        } catch (IOException | JDOMException | SAXException e) {
            throw new MCRPersistenceException("Could not retrieve xml of derivate: " + id, e);
        }
    }

    /**
     * Retrieves instance of {@link MCRObject} with the given {@link MCRObjectID}
     * 
     * @param id
     *            the object ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     */
    public static MCRObject retrieveMCRObject(final MCRObjectID id) throws MCRPersistenceException {
        try {
            Document xml = MCRXMLMetadataManager.instance().retrieveXML(id);
            if (xml == null) {
                throw new MCRPersistenceException("Could not retrieve xml of object: " + id);
            }
            return new MCRObject(xml);
        } catch (IOException | JDOMException | SAXException e) {
            throw new MCRPersistenceException("Could not retrieve xml of object: " + id, e);
        }
    }

    /**
     * @deprecated use {@link #retrieveMCRObject(MCRObjectID)} with {@link MCRObjectID#getInstance(String)} instead.
     * @return a {@link MCRObject} if there is an object with the id given or <code>null</code> otherwise
     */
    @Deprecated
    public static MCRObject retrieveMCRObject(final String id) throws MCRPersistenceException {
        if (!MCRMetadataManager.exists(MCRObjectID.getInstance(id))) {
            return null;
        }
        return retrieveMCRObject(MCRObjectID.getInstance(id));
    }

    /**
     * Retrieves instance of {@link MCRObject} or {@link MCRDerivate} depending on {@link MCRObjectID#getTypeId()}
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
     * @param base
     * @throws MCRPersistenceException
     * @throws IOException
     * @throws MCRAccessException
     * @throws MCRActiveLinkException
     */
    public static void update(final MCRBase base)
        throws MCRPersistenceException, IOException, MCRAccessException, MCRActiveLinkException {
        if (base instanceof MCRObject) {
            MCRMetadataManager.update((MCRObject) base);
        } else if (base instanceof MCRDerivate) {
            MCRMetadataManager.update((MCRDerivate) base);
        } else
            throw new IllegalArgumentException("Type is unsupported");
    }

    /**
     * Updates the derivate or creates it if it does not exist yet.
     * 
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     * @throws MCRAccessException if write permission to object or derivate is missing
     */
    public static void update(final MCRDerivate mcrDerivate)
        throws MCRPersistenceException, IOException, MCRAccessException {
        MCRObjectID id = mcrDerivate.getId();
        // check deletion mark
        if (MCRMarkManager.instance().isMarkedForDeletion(id)) {
            return;
        }
        if (!MCRMetadataManager.exists(id)) {
            MCRMetadataManager.create(mcrDerivate);
            return;
        }
        if (!MCRAccessManager.checkPermission(id, PERMISSION_WRITE)) {
            throw MCRAccessException.missingPermission("Update derivate", id.toString(), PERMISSION_WRITE);
        }
        File fileSourceDirectory = null;
        if (mcrDerivate.getDerivate().getInternals() != null
            && mcrDerivate.getDerivate().getInternals().getSourcePath() != null) {
            fileSourceDirectory = new File(mcrDerivate.getDerivate().getInternals().getSourcePath());

            if (!fileSourceDirectory.exists()) {
                LOGGER.warn(id + ": the directory " + fileSourceDirectory + " was not found.");
                fileSourceDirectory = null;
            }
        }
        // get the old Item
        MCRDerivate old = MCRMetadataManager.retrieveMCRDerivate(id);

        // remove the old link to metadata
        MCRMetaLinkID oldLink = old.getDerivate().getMetaLink();
        MCRMetaLinkID newLink = mcrDerivate.getDerivate().getMetaLink();
        if (!oldLink.equals(newLink)) {
            MCRObjectID oldMetadataObjectID = oldLink.getXLinkHrefID();
            MCRObjectID newMetadataObjectID = newLink.getXLinkHrefID();
            if (!oldMetadataObjectID.equals(newLink.getXLinkHrefID())) {
                try {
                    MCRMetadataManager.removeDerivateFromObject(oldMetadataObjectID, id);
                } catch (final MCRException e) {
                    LOGGER.warn(e.getMessage(), e);
                }
            }
            // add the link to metadata
            final MCRMetaLinkID der = new MCRMetaLinkID("derobject", id, null, mcrDerivate.getLabel(),
                newLink.getXLinkRole());
            addOrUpdateDerivateToObject(newMetadataObjectID, der);
        }
        // update the derivate
        mcrDerivate.getService().setDate("createdate", old.getService().getDate("createdate"));
        if (!mcrDerivate.getService().isFlagTypeSet(MCRObjectService.FLAG_TYPE_CREATEDBY)) {
            for (String flagCreatedBy : old.getService().getFlags(MCRObjectService.FLAG_TYPE_CREATEDBY)) {
                mcrDerivate.getService().addFlag(MCRObjectService.FLAG_TYPE_CREATEDBY, flagCreatedBy);
            }
        }

        MCRMetadataManager.updateMCRDerivateXML(mcrDerivate);

        // update to IFS
        if (fileSourceDirectory != null) {
            MCRPath rootPath = MCRPath.getPath(id.toString(), "/");
            Files.walkFileTree(fileSourceDirectory.toPath(), new MCRTreeCopier(fileSourceDirectory.toPath(), rootPath));
        }

    }

    /**
     * Updates the object or creates it if it does not exist yet.
     * 
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     * @throws MCRActiveLinkException
     *             if object is created (no real update), see {@link #create(MCRObject)}
     * @throws MCRAccessException if write permission is missing or see {@link #create(MCRObject)}
     */
    public static void update(final MCRObject mcrObject)
        throws MCRPersistenceException, MCRActiveLinkException, MCRAccessException {
        MCRObjectID id = mcrObject.getId();
        // check deletion mark
        if (MCRMarkManager.instance().isMarkedForDeletion(id)) {
            return;
        }
        if (!MCRMetadataManager.exists(id)) {
            MCRMetadataManager.create(mcrObject);
            return;
        }
        if (!MCRAccessManager.checkPermission(id, PERMISSION_WRITE)) {
            throw MCRAccessException.missingPermission("Update object.", id.toString(), PERMISSION_WRITE);
        }
        MCRObject old = MCRMetadataManager.retrieveMCRObject(id);

        // save the order of derivates and clean the structure
        final List<String> childOrder = mcrObject.getStructure()
            .getChildren()
            .stream()
            .map(MCRMetaLinkID::getXLinkHref)
            .collect(Collectors.toList());
        mcrObject.getStructure().clearChildren();
        List<String> derOrder = mcrObject.getStructure()
            .getDerivates()
            .stream()
            .map(MCRMetaLink::getXLinkHref)
            .collect(Collectors.toList());
        HashMap<String, String> newlinkTitles = new HashMap<String, String>();
        HashMap<String, String> newlinkLabels = new HashMap<String, String>();
        for (MCRMetaLinkID newlinkID : mcrObject.getStructure().getDerivates()) {
            if (newlinkID.getXLinkTitle() != null) {
                newlinkTitles.put(newlinkID.getXLinkHref(), newlinkID.getXLinkTitle());
            }
            if (newlinkID.getXLinkLabel() != null) {
                newlinkLabels.put(newlinkID.getXLinkHref(), newlinkID.getXLinkLabel());
            }
        }
        mcrObject.getStructure().clearDerivates();

        // set the derivate data in structure
        List<MCRMetaLinkID> linkIDs = mcrObject.getStructure().getDerivates();
        List<MCRMetaLinkID> oldlinkIDs = old.getStructure().getDerivates();
        for (MCRMetaLinkID oldlinkID : oldlinkIDs) {
            if (newlinkTitles.containsKey(oldlinkID.getXLinkHref())) {
                oldlinkID.setXLinkTitle(newlinkTitles.get(oldlinkID.getXLinkHref()));
            }
            if (newlinkLabels.containsKey(oldlinkID.getXLinkHref())) {
                oldlinkID.setXLinkLabel(newlinkLabels.get(oldlinkID.getXLinkHref()));
            }
            linkIDs.add(oldlinkID);
        }

        //set the new order of derivates
        for (int newPos = 0; newPos < derOrder.size(); newPos++) {
            for (int pos = 0; pos < mcrObject.getStructure().getDerivates().size(); pos++) {
                if (derOrder.get(newPos).equals(mcrObject.getStructure().getDerivates().get(pos).getXLinkHref())) {
                    Collections.swap(mcrObject.getStructure().getDerivates(), pos, newPos);
                    break;
                }
            }
        }

        // set the parent from the original and this update
        MCRObjectID oldParentID = old.getStructure().getParentID();
        MCRObjectID newParentID = mcrObject.getStructure().getParentID();

        if (oldParentID != null && exists(oldParentID) && (newParentID == null || !newParentID.equals(oldParentID))) {
            // remove child from the old parent
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Parent ID = " + oldParentID);
            }
            final MCRObject parent = MCRMetadataManager.retrieveMCRObject(oldParentID);
            parent.getStructure().removeChild(id);
            MCRMetadataManager.fireUpdateEvent(parent);
        }

        // set the children from the original -> but with new order
        List<MCRMetaLinkID> children = old.getStructure().getChildren();
        children.sort(new Comparator<MCRMetaLinkID>() {
            @Override
            public int compare(MCRMetaLinkID o1, MCRMetaLinkID o2) {
                int i1 = childOrder.indexOf(o1.getXLinkHref());
                int i2 = childOrder.indexOf(o2.getXLinkHref());
                return Integer.compare(i1, i2);
            }
        });
        mcrObject.getStructure().getChildren().addAll(children);

        // import all herited matadata from the parent
        receiveMetadata(mcrObject);

        // if not imported via cli, createdate remains unchanged
        if (!mcrObject.isImportMode() || mcrObject.getService().getDate("createdate") == null) {
            mcrObject.getService().setDate("createdate", old.getService().getDate("createdate"));
        }
        if (!mcrObject.isImportMode() && !mcrObject.getService().isFlagTypeSet(MCRObjectService.FLAG_TYPE_CREATEDBY)) {
            for (String flagCreatedBy : old.getService().getFlags(MCRObjectService.FLAG_TYPE_CREATEDBY)) {
                mcrObject.getService().addFlag(MCRObjectService.FLAG_TYPE_CREATEDBY, flagCreatedBy);
            }
        }

        // update this dataset
        MCRMetadataManager.fireUpdateEvent(mcrObject);

        // check if the parent was new set and set them
        if (newParentID != null && !newParentID.equals(oldParentID)) {
            MCRObject newParent = retrieveMCRObject(newParentID);
            newParent.getStructure().addChild(new MCRMetaLinkID("child", id, null, mcrObject.getLabel()));
            MCRMetadataManager.fireUpdateEvent(newParent);
        }

        // update all children
        boolean updatechildren = shareableMetadataChanged(mcrObject, old);
        if (updatechildren) {
            MCRMetadataShareAgent metadataShareAgent = MCRMetadataShareAgentFactory.getAgent(id);
            metadataShareAgent.distributeMetadata(mcrObject);
        }
    }

    private static boolean shareableMetadataChanged(final MCRObject mcrObject, MCRObject old) {
        MCRMetadataShareAgent metadataShareAgent = MCRMetadataShareAgentFactory.getAgent(mcrObject.getId());
        return metadataShareAgent.shareableMetadataChanged(old, mcrObject);
    }

    private static void receiveMetadata(final MCRObject recipient) {
        MCRMetadataShareAgent metadataShareAgent = MCRMetadataShareAgentFactory.getAgent(recipient.getId());
        metadataShareAgent.receiveMetadata(recipient);
    }

    /**
     * Updates only the XML part of the derivate.
     * 
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     */
    public static void updateMCRDerivateXML(final MCRDerivate mcrDerivate) throws MCRPersistenceException {
        if (!mcrDerivate.isImportMode() || mcrDerivate.getService().getDate("modifydate") == null) {
            mcrDerivate.getService().setDate("modifydate");
        }
        fireEvent(mcrDerivate, retrieveMCRDerivate(mcrDerivate.getId()), MCREvent.UPDATE_EVENT);
    }

    /**
     * Adds or updates a derivate MCRMetaLinkID to the structure part and updates the object with the ID in the data
     * store.
     * 
     * @param id
     *            the object ID
     * @param link
     *            a link to a derivate as MCRMetaLinkID
     * @return True if the link is added or updated, false if nothing changed.
     * @throws MCRPersistenceException
     *             if a persistence problem is occurred
     */
    public static boolean addOrUpdateDerivateToObject(final MCRObjectID id, final MCRMetaLinkID link)
        throws MCRPersistenceException {
        final MCRObject object = MCRMetadataManager.retrieveMCRObject(id);
        if (!object.getStructure().addOrUpdateDerivate(link)) {
            return false;
        }
        if (!object.isImportMode()) {
            object.getService().setDate("modifydate");
        }
        MCRMetadataManager.fireUpdateEvent(object);
        return true;
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

    private static void restore(final MCRDerivate mcrDerivate, final MCRObjectID mcrObjectId, final byte[] backup) {
        try {
            final MCRObject obj = new MCRObject(backup, false);
            // If an event handler exception occurred, its crucial to restore the object first
            // before updating it again. Otherwise the exception could be thrown again and
            // the object will be in an invalid state (the not existing derivate will be
            // linked with the object).
            MCRXMLMetadataManager.instance().update(mcrObjectId, obj.createXML(), new Date());

            // update and call event handlers
            MCRMetadataManager.update(obj);
        } catch (final Exception e1) {
            LOGGER.warn("Error while restoring " + mcrObjectId, e1);
        } finally {
            // remove derivate
            fireEvent(mcrDerivate, null, MCREvent.DELETE_EVENT);
        }
    }

    private static void fireEvent(MCRBase base, MCRBase oldBase, String eventType) {
        boolean objectEvent = base instanceof MCRObject;
        String type = objectEvent ? MCREvent.OBJECT_TYPE : MCREvent.DERIVATE_TYPE;
        final MCREvent evt = new MCREvent(type, eventType);
        if (objectEvent) {
            evt.put(MCREvent.OBJECT_KEY, base);
        } else {
            evt.put(MCREvent.DERIVATE_KEY, base);
        }
        Optional.ofNullable(oldBase)
            .ifPresent(b -> evt.put(objectEvent ? MCREvent.OBJECT_OLD_KEY : MCREvent.DERIVATE_OLD_KEY, b));
        if (MCREvent.DELETE_EVENT.equals(eventType)) {
            MCREventManager.instance().handleEvent(evt, MCREventManager.BACKWARD);
        } else {
            MCREventManager.instance().handleEvent(evt);
        }
    }
}
