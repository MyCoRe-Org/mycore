/*
 * $Id$
 * $Revision: 5697 $ $Date: 10.09.2010 $
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

package org.mycore.datamodel.metadata;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFileImportExport;

/**
 * Delivers persistence operations for {@link MCRObject} and {@link MCRDerivate}.
 * 
 * @author Thomas Scheffler (yagee)
 * @since 2.0.92
 *
 */
public final class MCRMetadataManager {

    private static final Logger LOGGER = Logger.getLogger(MCRMetadataManager.class);

    private MCRMetadataManager() {

    }

    /**
     * Stores the derivate.
     * 
     * @param mcrDerivate derivate instance to store
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     */
    public static final void create(final MCRDerivate mcrDerivate) throws MCRPersistenceException {
        // exist the derivate?
        if (exists(mcrDerivate.getId())) {
            throw new MCRPersistenceException("The derivate " + mcrDerivate.getId() + " allready exists, nothing done.");
        }

        if (!mcrDerivate.isValid()) {
            throw new MCRPersistenceException("The derivate " + mcrDerivate.getId() + " is not valid.");
        }
        final MCRObjectID objid = mcrDerivate.getDerivate().getMetaLink().getXLinkHrefID();
        if (!MCRXMLMetadataManager.instance().exists(objid)) {
            throw new MCRPersistenceException("The derivate " + mcrDerivate.getId() + " can't find metadata object " + objid + ", nothing done.");
        }

        // prepare the derivate metadata and store under the XML table
        if (mcrDerivate.getService().getDate("createdate") == null || !mcrDerivate.isImportMode()) {
            mcrDerivate.getService().setDate("createdate");
        }
        if (mcrDerivate.getService().getDate("modifydate") == null || !mcrDerivate.isImportMode()) {
            mcrDerivate.getService().setDate("modifydate");
        }

        // handle events
        LOGGER.debug("Handling derivate CREATE event");
        final MCREvent evt = new MCREvent(MCREvent.DERIVATE_TYPE, MCREvent.CREATE_EVENT);
        evt.put("derivate", mcrDerivate);
        MCREventManager.instance().handleEvent(evt);

        // add the link to metadata
        final MCRMetaLinkID meta = mcrDerivate.getDerivate().getMetaLink();
        final MCRMetaLinkID der = new MCRMetaLinkID();
        der.setReference(mcrDerivate.getId().toString(), mcrDerivate.getLabel(), "");
        der.setSubTag("derobject");
        final byte[] backup = MCRXMLMetadataManager.instance().retrieveBLOB(meta.getXLinkHrefID());

        try {
            LOGGER.debug("adding Derivate in data store");
            MCRMetadataManager.addDerivateToObject(meta.getXLinkHrefID(), der);
        } catch (final Exception e) {
            MCRMetadataManager.restore(mcrDerivate, backup);
            // throw final exception
            throw new MCRPersistenceException("Error while creatlink to MCRObject " + meta.getXLinkHref() + ".", e);
        }

        // create data in IFS
        if (mcrDerivate.getDerivate().getInternals() != null) {
            if (mcrDerivate.getDerivate().getInternals().getSourcePath() == null) {
                final MCRDirectory difs = new MCRDirectory(mcrDerivate.getId().toString(), mcrDerivate.getId().toString());
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
                        MCRMetadataManager.restore(mcrDerivate, backup);
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
     * @param mcrObject object instance to store
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     * @throws MCRActiveLinkException
     *                if current object links to nonexistent
     */
    public static final void create(final MCRObject mcrObject) throws MCRPersistenceException, MCRActiveLinkException {
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
        MCRObject parent = null;

        if (parent_id != null) {
            LOGGER.debug("Parent ID = " + parent_id.toString());

            try {
                parent = MCRMetadataManager.retrieveMCRObject(parent_id);
                mcrObject.getMetadata().appendMetadata(parent.getMetadata().getHeritableMetadata());
            } catch (final Exception e) {
                LOGGER.error(MCRException.getStackTraceAsString(e));
                LOGGER.error("Error while merging metadata in this object.");

                return;
            }
        }

        // handle events
        final MCREvent evt = new MCREvent(MCREvent.OBJECT_TYPE, MCREvent.CREATE_EVENT);
        evt.put("object", mcrObject);
        MCREventManager.instance().handleEvent(evt);

        // add the MCRObjectID to the child list in the parent object
        if (parent_id != null) {
            try {
                parent.getStructure().addChild(
                    new MCRMetaLinkID("child", mcrObject.getId(), mcrObject.getStructure().getParent().getXLinkLabel(), mcrObject.getLabel()));
                MCRMetadataManager.fireUpdateEvent(parent);
            } catch (final Exception e) {
                LOGGER.debug(MCRException.getStackTraceAsString(e));
                LOGGER.error("Error while store child ID in parent object.");
                try {
                    MCRMetadataManager.delete(mcrObject);
                    LOGGER.error("Child object was removed.");
                } catch (final MCRActiveLinkException e1) {
                    // it shouldn't be possible to have allready links to this
                    // object
                    LOGGER.error("Error while deleting child object.", e1);
                }

                return;
            }
        }
    }

    /**
     * Deletes MCRDerivate.
     * @param mcrDerivate to be deleted
     * @throws MCRPersistenceException
     *  if persistence problem occurs
     */
    public static final void delete(final MCRDerivate mcrDerivate) throws MCRPersistenceException {
        // remove link
        MCRObjectID metaId = null;
        try {
            metaId = mcrDerivate.getDerivate().getMetaLink().getXLinkHrefID();
            MCRMetadataManager.removeDerivateFromObject(metaId, mcrDerivate.getId());
            LOGGER.info("Link in MCRObject " + metaId + " to MCRDerivate " + mcrDerivate.getId() + " is deleted.");
        } catch (final Exception e) {
            LOGGER.warn("Can't delete link for MCRDerivate " + mcrDerivate.getId() + " from MCRObject " + metaId + ". Error ignored.");
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
        final MCREvent evt = new MCREvent(MCREvent.DERIVATE_TYPE, MCREvent.DELETE_EVENT);
        evt.put("derivate", mcrDerivate);
        MCREventManager.instance().handleEvent(evt);
    }

    /**
     * Deletes MCRObject.
     * @param mcrObject to be deleted
     * @throws MCRPersistenceException
     *  if persistence problem occurs
     */
    public static void delete(final MCRObject mcrObject) throws MCRPersistenceException, MCRActiveLinkException {
        if (mcrObject.getId() == null) {
            throw new MCRPersistenceException("The MCRObjectID is null.");
        }

        // check for active links
        final Collection<String> sources = MCRLinkTableManager.instance().getSourceOf(mcrObject.mcr_id);
        LOGGER.debug("Sources size:" + sources.size());
        if (sources.size() > 0) {
            final MCRActiveLinkException activeLinks = new MCRActiveLinkException(new StringBuffer("Error while deleting object ")
                .append(mcrObject.mcr_id.toString())
                .append(". This object is still referenced by other objects and can not be removed until all links are released.")
                .toString());
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
                LOGGER.error("Error while removing child ID in parent object. The parent " + parent_id + "is now inconsistent.", e);
            }
        }

        // handle events
        final MCREvent evt = new MCREvent(MCREvent.OBJECT_TYPE, MCREvent.DELETE_EVENT);
        evt.put("object", mcrObject);
        MCREventManager.instance().handleEvent(evt, MCREventManager.BACKWARD);
    }

    /**
     * Delete the derivate. The order of delete
     * steps is:<br />
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
    public static final void deleteMCRDerivate(final MCRObjectID id) throws MCRPersistenceException {
        final MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(id);
        MCRMetadataManager.delete(derivate);
    }

    /**
     * Deletes the object.
     * 
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     * @throws MCRActiveLinkException
     *                if object is referenced by other objects
     */
    public static final void deleteMCRObject(final MCRObjectID id) throws MCRPersistenceException, MCRActiveLinkException {
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
    public final static boolean exists(final MCRObjectID id) throws MCRPersistenceException {
        return MCRXMLMetadataManager.instance().exists(id);
    }

    /**
     * Fires {@link MCREvent#REPAIR_EVENT} for given derivate.
     * @param mcrDerivate 
     */
    public static final void fireRepairEvent(final MCRDerivate mcrDerivate) throws MCRPersistenceException {
        // handle events
        final MCREvent evt = new MCREvent(MCREvent.DERIVATE_TYPE, MCREvent.REPAIR_EVENT);
        evt.put("derivate", mcrDerivate);
        MCREventManager.instance().handleEvent(evt);
    }

    /**
     * Fires {@link MCREvent#REPAIR_EVENT} for given object.
     * @param mcrObject
     */
    public static final void fireRepairEvent(final MCRObject mcrObject) throws MCRPersistenceException {
        // check derivate link
        for (MCRMetaLinkID derivate : mcrObject.getStructure().getDerivates()) {
            if (!exists(derivate.getXLinkHrefID())) {
                LOGGER.error("Can't find MCRDerivate " + derivate.getXLinkHrefID());
            }
        }
        // handle events
        final MCREvent evt = new MCREvent(MCREvent.OBJECT_TYPE, MCREvent.REPAIR_EVENT);
        evt.put("object", mcrObject);
        MCREventManager.instance().handleEvent(evt);
    }

    /**
     * Fires {@link MCREvent#UPDATE_EVENT} for given object.
     * If {@link MCRObject#isImportMode()} modifydate will not be updated.
     * @param mcrObject TODO
     */
    public static final void fireUpdateEvent(final MCRObject mcrObject) throws MCRPersistenceException {
        if (!mcrObject.isImportMode() || mcrObject.getService().getDate("modifydate") == null) {
            mcrObject.getService().setDate("modifydate");
        }
        // remove ACL if it is set from data source
        for (int i = 0; i < mcrObject.getService().getRulesSize(); i++) {
            mcrObject.getService().removeRule(i);
            i--;
        }
        // handle events
        final MCREvent evt = new MCREvent(MCREvent.OBJECT_TYPE, MCREvent.UPDATE_EVENT);
        evt.put("object", mcrObject);
        MCREventManager.instance().handleEvent(evt);
    }

    /**
     * Retrieves instance of {@link MCRDerivate} with the given {@link MCRObjectID}
     * 
     * @param id
     *            the derivate ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     */
    public static final MCRDerivate retrieveMCRDerivate(final MCRObjectID id) throws MCRPersistenceException {
        final MCRDerivate derivate = new MCRDerivate(MCRXMLMetadataManager.instance().retrieveXML(id));
        return derivate;
    }

    /**
     * Retrieves instance of {@link MCRObject} with the given {@link MCRObjectID}
     * 
     * @param id
     *            the object ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     */
    public static final MCRObject retrieveMCRObject(final MCRObjectID id) throws MCRPersistenceException {
        return new MCRObject(MCRXMLMetadataManager.instance().retrieveXML(id));
    }

    /**
     * Retrieves instance of {@link MCRObject} or {@link MCRDerivate} depending on {@link MCRObjectID#getTypeId()}
     * @param id derivate or object id
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     */
    public static final MCRBase retrieve(final MCRObjectID id) throws MCRPersistenceException {
        if (id.getTypeId().equals("derivate")) {
            return retrieveMCRDerivate(id);
        }
        return retrieveMCRObject(id);
    }

    /**
     * Updates the derivate.
     * 
     * @param mcrDerivate
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     */
    public static final void update(final MCRDerivate mcrDerivate) throws MCRPersistenceException {
        // get the old Item
        MCRDerivate old = new MCRDerivate();

        try {
            old = MCRMetadataManager.retrieveMCRDerivate(mcrDerivate.getId());
        } catch (final Exception e) {
            MCRMetadataManager.create(mcrDerivate);
            return;
        }

        // remove the old link to metadata
        MCRObjectID metaId = null;
        try {
            metaId = old.getDerivate().getMetaLink().getXLinkHrefID();
            MCRMetadataManager.removeDerivateFromObject(metaId, mcrDerivate.getId());
        } catch (final MCRException e) {
            LOGGER.warn(e.getMessage(), e);
        }

        // update to IFS
        if (mcrDerivate.getDerivate().getInternals() != null && mcrDerivate.getDerivate().getInternals().getSourcePath() != null) {
            final File f = new File(mcrDerivate.getDerivate().getInternals().getSourcePath());

            if (!f.exists()) {
                throw new MCRPersistenceException("The File or Directory " + mcrDerivate.getDerivate().getInternals().getSourcePath() + " was not found.");
            }

            try {
                final MCRDirectory difs = MCRDirectory.getRootDirectory(mcrDerivate.getId().toString());
                MCRFileImportExport.importFiles(f, difs);
                mcrDerivate.getDerivate().getInternals().setIFSID(difs.getID());
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        // update the derivate
        mcrDerivate.getService().setDate("createdate", old.getService().getDate("createdate"));
        MCRMetadataManager.updateMCRDerivateXML(mcrDerivate);

        // add the link to metadata
        metaId = mcrDerivate.getDerivate().getMetaLink().getXLinkHrefID();
        final MCRMetaLinkID der = new MCRMetaLinkID();
        der.setReference(mcrDerivate.getId().toString(), mcrDerivate.getLabel(), "");
        der.setSubTag("derobject");
        MCRMetadataManager.addDerivateToObject(metaId, der);
    }

    /**
     * Updates the object.
     * 
     * @param mcrObject
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     * @throws MCRActiveLinkException
     *             if object is created (no real update), see {@link #create(MCRObject)}
     */
    public static final void update(final MCRObject mcrObject) throws MCRPersistenceException, MCRActiveLinkException {
        // get the old Item
        MCRObject old = new MCRObject();

        try {
            old = MCRMetadataManager.retrieveMCRObject(mcrObject.getId());
        } catch (final Exception pe) {
            MCRMetadataManager.create(mcrObject);
            return;
        }

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

        if (newParentID != null) {
            LOGGER.debug("Parent ID = " + newParentID);

            try {
                final MCRObject parent = MCRMetadataManager.retrieveMCRObject(newParentID);
                // remove already embedded inherited tags
                mcrObject.getMetadata().removeInheritedMetadata();
                // insert heritable tags
                mcrObject.getMetadata().appendMetadata(parent.getMetadata().getHeritableMetadata());

            } catch (final Exception e) {
                LOGGER.error(MCRException.getStackTraceAsString(e));
                LOGGER.error("Error while merging metadata in this object.");
            }
        }

        // if not imported via cli, createdate remains unchanged
        if (!mcrObject.isImportMode() || mcrObject.getService().getDate("createdate") == null) {
            mcrObject.getService().setDate("createdate", old.getService().getDate("createdate"));
        }

        // update this dataset
        MCRMetadataManager.fireUpdateEvent(mcrObject);

        // check if the parent was new set and set them
        if (setparent) {
            try {
                final MCRObject parent = MCRMetadataManager.retrieveMCRObject(newParentID);
                parent.getStructure().addChild(new MCRMetaLinkID("child", mcrObject.getId(), mcrObject.getLabel(), mcrObject.getLabel()));
                MCRMetadataManager.fireUpdateEvent(parent);
            } catch (final Exception e) {
                LOGGER.debug(MCRException.getStackTraceAsString(e));
                LOGGER.error("Error while store child ID in parent object.");
                try {
                    MCRMetadataManager.delete(mcrObject);
                    LOGGER.error("Child object was removed.");
                } catch (final MCRActiveLinkException e1) {
                    // it shouldn't be possible to have allready links to this
                    // object
                    LOGGER.error("Error while deleting child object.", e1);
                }

                return;
            }
        }

        // update all children
        boolean updatechildren = false;
        final MCRObjectMetadata md = mcrObject.getMetadata();
        final MCRObjectMetadata mdold = old.getMetadata();
        int numheritablemd = 0;
        int numheritablemdold = 0;
        for (int i = 0; i < md.size(); i++) {
            final MCRMetaElement melm = md.getMetadataElement(i);
            if (melm.isHeritable()) {
                numheritablemd++;
                try {
                    final MCRMetaElement melmold = mdold.getMetadataElement(melm.getTag());
                    final Element jelm = melm.createXML(true);
                    final Element jelmold = melmold.createXML(true);
                    if (!MCRXMLHelper.deepEqual(jelmold, jelm)) {
                        updatechildren = true;
                        break;
                    }
                } catch (final RuntimeException e) {
                    updatechildren = true;
                }
            }
        }
        if (!updatechildren) {
            for (int i = 0; i < mdold.size(); i++) {
                final MCRMetaElement melmold = mdold.getMetadataElement(i);
                if (melmold.isHeritable()) {
                    numheritablemdold++;
                }
            }
        }
        if (numheritablemd != numheritablemdold) {
            updatechildren = true;
        }
        if (updatechildren) {
            for (MCRMetaLinkID child : mcrObject.getStructure().getChildren()) {
                MCRMetadataManager.updateInheritedMetadata(child.getXLinkHrefID());
            }
        }
    }

    /**
     * Updates only the XML part of the derivate.
     * @param mcrDerivate
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     */
    public static final void updateMCRDerivateXML(final MCRDerivate mcrDerivate) throws MCRPersistenceException {
        if (!mcrDerivate.isImportMode() || mcrDerivate.getService().getDate("modifydate") == null) {
            mcrDerivate.getService().setDate("modifydate");
        }
        final MCREvent evt = new MCREvent(MCREvent.DERIVATE_TYPE, MCREvent.UPDATE_EVENT);
        evt.put("derivate", mcrDerivate);
        MCREventManager.instance().handleEvent(evt);
    }

    /**
     * Adds a derivate MCRMetaLinkID to the structure part and updates
     * the object with the ID in the data store.
     * 
     * @param id
     *            the object ID
     * @param link
     *            a link to a derivate as MCRMetaLinkID
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     */
    public static final void addDerivateToObject(final MCRObjectID id, final MCRMetaLinkID link) throws MCRPersistenceException {
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

    public static final void removeDerivateFromObject(final MCRObjectID objectID, final MCRObjectID derivateID) throws MCRPersistenceException {
        final MCRObject object = MCRMetadataManager.retrieveMCRObject(objectID);
        Iterator<MCRMetaLinkID> derIterator = object.getStructure().getDerivates().iterator();
        while (derIterator.hasNext()) {
            MCRMetaLinkID der = derIterator.next();
            if (der.getXLinkHrefID().equals(objectID)) {
                object.getService().setDate("modifydate");
                derIterator.remove();
                break;
            }
        }
        MCRMetadataManager.fireUpdateEvent(object);
    }

    private static void restore(final MCRDerivate mcrDerivate, final byte[] backup) {
        MCREvent evt;
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
            evt = new MCREvent(MCREvent.DERIVATE_TYPE, MCREvent.DELETE_EVENT);
            evt.put("derivate", mcrDerivate);
            MCREventManager.instance().handleEvent(evt);
        }
    }

    /**
     * Updates the metadata of the stored dataset and replace the
     * inherited data from the parent.
     * 
     * @param childId
     *            the MCRObjectID of the parent as string
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     */
    private static final void updateInheritedMetadata(final MCRObjectID childId) throws MCRPersistenceException {
        LOGGER.debug("Update metadata from Child " + childId);
        final MCRObject child = MCRMetadataManager.retrieveMCRObject(childId);
        try {
            update(child);
        } catch (MCRActiveLinkException e) {
            //should never happen, as the object is unchanged
            throw new MCRPersistenceException("Error while updating inherited metadata", e);
        }
    }
}
