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

package org.mycore.datamodel.metadata;

import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;
import org.mycore.common.xml.MCRXMLHelper;

/**
 * This class implements all methode for handling one metadata object. Methodes
 * of this class can read the XML metadata by using a XML parser, manipulate the
 * data in the abstract persistence data store and return the XML stream to the
 * user application. Additionally, this class provides the public user interface
 * for the linking of MCRObjects against other MCRObjects with metadata
 * inheritance.
 * 
 * @author Jens Kupferschmidt
 * @author Mathias Hegner
 * @version $Revision$ $Date$
 */
final public class MCRObject extends MCRBase {
    // the object content
    private MCRObjectStructure mcr_struct = null;

    private MCRObjectMetadata mcr_metadata = null;

    /**
     * This is the constructor of the MCRObject class. It creates an instance of
     * the parser class and the metadata class. <br>
     * The constructor reads the following information from the property file:
     * <ul>
     * <li>MCR.XMLParser.Class</li>
     * </ul>
     * 
     * @exception MCRException
     *                general Exception of MyCoRe
     * @exception MCRConfigurationException
     *                a special exception for configuartion data
     */
    public MCRObject() throws MCRException, MCRConfigurationException {
        super();

        // Metadata class
        mcr_metadata = new MCRObjectMetadata();

        // Structure class
        mcr_struct = new MCRObjectStructure(logger);
    }

    /**
     * This methode return the object metadata element selected by tag. If this
     * was not found, null was returned.
     * 
     * @return the metadata tag part as a object that extend MCRMetaElement
     */
    public final MCRMetaElement getMetadataElement(String tag) {
        return mcr_metadata.getMetadataElement(tag);
    }

    /**
     * This method returns the instance of the MCRObjectMetadata class. If there
     * was no MCRObjectMetadata found, null will be returned.
     * 
     * @return the instance of the MCRObjectMetadata class
     */
    public final MCRObjectMetadata getMetadata() {
        return mcr_metadata;
    }

    /**
     * This methode return the instance of the MCRObjectStructure class. If this
     * was not found, null was returned.
     * 
     * @return the instance of the MCRObjectStructure class
     */
    public final MCRObjectStructure getStructure() {
        return mcr_struct;
    }

    /**
     * The given DOM was convert into an internal view of metadata. This are the
     * object ID and the object label, also the blocks structure, flags and
     * metadata.
     * 
     * @exception MCRException
     *                general Exception of MyCoRe
     */
    private final void set() throws MCRException {
        if (jdom_document == null) {
            throw new MCRException("The JDOM document is null or empty.");
        }

        setRoot();

        setStructure();

        setMetadata();

        setService();
    }

    private void setRoot() {
        // get object ID from DOM
        org.jdom.Element jdom_element_root = jdom_document.getRootElement();
        mcr_id = new MCRObjectID(jdom_element_root.getAttributeValue("ID"));
        mcr_label = jdom_element_root.getAttributeValue("label").trim();

        if (mcr_label.length() > MAX_LABEL_LENGTH) {
            mcr_label = mcr_label.substring(0, MAX_LABEL_LENGTH);
        }

        mcr_version = jdom_element_root.getAttributeValue("version");
        if ((mcr_version == null) || ((mcr_version = mcr_version.trim()).length() == 0)) {
            setVersion();
        }

        mcr_schema = jdom_element_root.getAttribute("noNamespaceSchemaLocation", XSI_NAMESPACE).getValue().trim();
        logger.debug("MCRObject set schemafile: " + mcr_schema);
    }

    private void setStructure() {
        // get the structure data of the object
        org.jdom.Element jdom_element_root = jdom_document.getRootElement();
        org.jdom.Element jdom_element = jdom_element_root.getChild("structure");
        mcr_struct = new MCRObjectStructure(logger);
        mcr_struct.setFromDOM(jdom_element);
    }

    private void setMetadata() {
        // get the metadata of the object
        org.jdom.Element jdom_element_root = jdom_document.getRootElement();
        org.jdom.Element jdom_element = jdom_element_root.getChild("metadata");
        mcr_metadata = new MCRObjectMetadata();
        mcr_metadata.setFromDOM(jdom_element);
    }

    private void setService() {
        org.jdom.Element jdom_element;
        // get the service data of the object
        org.jdom.Element jdom_element_root = jdom_document.getRootElement();
        jdom_element = jdom_element_root.getChild("service");
        mcr_service = new MCRObjectService();
        mcr_service.setFromDOM(jdom_element);
    }

    /**
     * This method read the XML input stream from an URI to build up the
     * MyCoRe-Object.
     * 
     * @param uri
     *            an URI
     * @exception MCRException
     *                general Exception of MyCoRe
     */
    public final void setFromURI(String uri) throws MCRException {
        setFromJDOM(MCRXMLHelper.parseURI(uri));
    }

    /**
     * This method read the XML input stream from a byte array to build up the
     * MyCoRe-Object.
     * 
     * @param xml
     *            a XML string
     * @exception MCRException
     *                general Exception of MyCoRe
     */
    public final void setFromXML(byte[] xml, boolean valid) throws MCRException {
        setFromJDOM(MCRXMLHelper.parseXML(xml, valid));
    }

    /**
     * This methode gets a JDOM-Document to build up the MyCoRe-Object.
     * 
     * @param doc
     *            an JDOM Object
     * @exception MCRException
     *                general Exception of MyCoRe
     */
    public final void setFromJDOM(Document doc) throws MCRException {
        jdom_document = doc;
        set();
    }

    /**
     * This methode set the object metadata part named by a tag.
     * 
     * @param obj
     *            the class object of a metadata part
     * @param tag
     *            the tag of a metadata part
     * @return true if set was succesful, otherwise false
     */
    public final boolean setMetadataElement(MCRMetaElement obj, String tag) {
        if (obj == null) {
            return false;
        }

        if ((tag == null) || ((tag = tag.trim()).length() == 0)) {
            return false;
        }

        return mcr_metadata.setMetadataElement(obj, tag);
    }

    /**
     * This methode set the object MCRObjectStructure.
     * 
     * @param structure
     *            the object MCRObjectStructure part
     */
    public final void setStructure(MCRObjectStructure structure) {
        if (structure != null) {
            mcr_struct = structure;
        }
    }

    /**
     * This methode create a XML stream for all object data.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Document with the XML data of the object as byte array
     */
    public final org.jdom.Document createXML() throws MCRException {
        if (!isValid()) {
            throw new MCRException("The content is not valid.");
        }

        org.jdom.Element elm = new org.jdom.Element("mycoreobject");
        org.jdom.Document doc = new org.jdom.Document(elm);
        elm.addNamespaceDeclaration(XSI_NAMESPACE);
        elm.addNamespaceDeclaration(XLINK_NAMESPACE);
        elm.setAttribute("noNamespaceSchemaLocation", mcr_schema, XSI_NAMESPACE);
        elm.setAttribute("ID", mcr_id.getId());
        elm.setAttribute("label", mcr_label);
        elm.setAttribute("version", mcr_version);
        elm.addContent(mcr_struct.createXML());
        elm.addContent(mcr_metadata.createXML());
        elm.addContent(mcr_service.createXML());

        return doc;
    }

    /**
     * The methode create the object in the data store.
     * 
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     * @throws MCRActiveLinkException
     */
    public final void createInDatastore() throws MCRPersistenceException, MCRActiveLinkException {
        // exist the object?
        if (existInDatastore(mcr_id.getId())) {
            throw new MCRPersistenceException("The object " + mcr_id.getId() + " allready exists, nothing done.");
        }

        // create this object in datastore
        if (mcr_service.getDate("createdate") == null) {
            mcr_service.setDate("createdate");
        }
        if (mcr_service.getDate("modifydate") == null) {
            mcr_service.setDate("modifydate");
        }

        // prepare this object with parent metadata
        MCRObjectID parent_id = mcr_struct.getParentID();
        MCRObject parent = null;

        if (parent_id != null) {
            logger.debug("Parent ID = " + parent_id.getId());

            try {
                parent = new MCRObject();
                parent.receiveFromDatastore(parent_id);
                mcr_metadata.appendMetadata(parent.getMetadata().getHeritableMetadata());
            } catch (Exception e) {
                logger.error(MCRException.getStackTraceAsString(e));
                logger.error("Error while merging metadata in this object.");

                return;
            }
        }

        // handle events
        MCREvent evt = new MCREvent(MCREvent.OBJECT_TYPE, MCREvent.CREATE_EVENT);
        evt.put("object", this);
        MCREventManager.instance().handleEvent(evt);

        // add the MCRObjectID to the child list in the parent object
        if (parent_id != null) {
            try {
                parent.getStructure().addChild(mcr_id, mcr_struct.getParent().getXLinkLabel(), mcr_label);
                parent.updateThisInDatastore();
            } catch (Exception e) {
                logger.debug(MCRException.getStackTraceAsString(e));
                logger.error("Error while store child ID in parent object.");
                try {
                    deleteFromDatastore();
                    logger.error("Child object was removed.");
                } catch (MCRActiveLinkException e1) {
                    // it shouldn't be possible to have allready links to this
                    // object
                    logger.error("Error while deleting child object.", e1);
                }

                return;
            }
        }
    }

    /**
     * The methode add a derivate MCRMetaLinkID to the structure part and update
     * the object with the ID in the data store.
     * 
     * @param id
     *            the object ID
     * @param link
     *            a link to a derivate as MCRMetaLinkID
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public final void addDerivateInDatastore(String id, MCRMetaLinkID link) throws MCRPersistenceException {
        receiveFromDatastore(id);
        // don't put the same derivates twice in an object!
        MCRMetaLinkID testlink = null;
        boolean checklink = false;
        for (int i = 0; i < mcr_struct.getDerivateSize(); i++) {
            testlink = mcr_struct.getDerivate(i);
            if (link.href.equals(testlink.getXLinkHref())) {
                checklink = true;
            }
        }
        if (checklink)
            return;
        // add link
        if (!importMode) {
            mcr_service.setDate("modifydate");
        }
        mcr_struct.addDerivate(link);
        updateThisInDatastore();
    }

    /**
     * The methode remove a derivate MCRMetaLinkID from the structure part and
     * update the object with the ID in the data store.
     * 
     * @param id
     *            the object ID
     * @param der_id
     *            the derivate ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public final void removeDerivateInDatastore(String id, String der_id) throws MCRPersistenceException {
        receiveFromDatastore(id);
        mcr_service.setDate("modifydate");
        MCRMetaLinkID link = null;
        for (int i = 0; i < mcr_struct.getDerivateSize(); i++) {
            link = mcr_struct.getDerivate(i);
            if (link.href.equals(der_id)) {
                mcr_struct.removeDerivate(i);
            }
        }
        updateThisInDatastore();
    }

    /**
     * The methode delete the object for the given ID from the data store.
     * 
     * @param id
     *            the object ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     * @throws MCRActiveLinkException
     */
    public final void deleteFromDatastore(String id) throws MCRPersistenceException, MCRActiveLinkException {
        mcr_id = new MCRObjectID(id);
        deleteFromDatastore();
    }

    /**
     * The methode delete the object from the data store.
     * 
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     * @throws MCRActiveLinkException
     */
    private final void deleteFromDatastore() throws MCRPersistenceException, MCRActiveLinkException {
        if (mcr_id == null) {
            throw new MCRPersistenceException("The MCRObjectID is null.");
        }

        // check for active links
        List sources = MCRLinkTableManager.instance().getSourceOf(mcr_id);
        logger.debug("Sources size:" + sources.size());
        if (sources.size() > 0) {
            MCRActiveLinkException activeLinks = new MCRActiveLinkException(new StringBuffer("Error while deleting object ").append(mcr_id.toString()).append(". This object is still referenced by other objects and can not be removed until all links are released.").toString());
            String curSource;
            Iterator it = sources.iterator();
            while (it.hasNext()) {
                curSource = (String) it.next();
                activeLinks.addLink(curSource, mcr_id.toString());
            }
            throw activeLinks;
        }

        // get the Item
        receiveFromDatastore(mcr_id);

        // set the derivate data in structure
        MCRDerivate der = null;

        for (int i = 0; i < mcr_struct.getDerivateSize(); i++) {
            der = new MCRDerivate();

            try {
                der.deleteFromDatastore(getStructure().getDerivate(i).getXLinkHref());
            } catch (MCRException e) {
                logger.debug(MCRException.getStackTraceAsString(e));
                logger.error(e.getMessage());
                logger.error("Error while deleting derivate.");
            }
        }

        // remove all children
        MCRObject child = null;

        for (int i = 0; i < mcr_struct.getChildSize(); i++) {
            child = new MCRObject();

            try {
                child.deleteFromDatastore(getStructure().getChild(i).getXLinkHref());
            } catch (MCRException e) {
                logger.debug(MCRException.getStackTraceAsString(e));
                logger.error(e.getMessage());
                logger.error("Error while deleting child.");
            }
        }

        // remove child from parent
        MCRObjectID parent_id = mcr_struct.getParentID();

        if (parent_id != null) {
            logger.debug("Parent ID = " + parent_id.getId());

            try {
                MCRObject parent = new MCRObject();
                parent.receiveFromDatastore(parent_id);
                parent.mcr_struct.removeChild(mcr_id);
                parent.updateThisInDatastore();
            } catch (Exception e) {
                logger.debug(MCRException.getStackTraceAsString(e));
                logger.error("Error while delete child ID in parent object.");
                logger.warn("Attention, the parent " + parent_id + "is now inconsist.");
            }
        }

        // handle events
        MCREvent evt = new MCREvent(MCREvent.OBJECT_TYPE, MCREvent.DELETE_EVENT);
        evt.put("object", this);
        MCREventManager.instance().handleEvent(evt, MCREventManager.BACKWARD);
    }

    /**
     * The methode return true if the object is in the data store, else return
     * false.
     * 
     * @param id
     *            the object ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public final static boolean existInDatastore(String id) throws MCRPersistenceException {
        return existInDatastore(new MCRObjectID(id));
    }

    /**
     * The methode return true if the object is in the data store, else return
     * false.
     * 
     * @param id
     *            the object ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public final static boolean existInDatastore(MCRObjectID id) throws MCRPersistenceException {
        return MCRXMLTableManager.instance().exist(id);
    }

    /**
     * The methode receive the object for the given MCRObjectID and stored it in
     * this MCRObject.
     * 
     * @param id
     *            the object ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public final void receiveFromDatastore(String id) throws MCRPersistenceException {
        receiveFromDatastore(new MCRObjectID(id));
    }

    /**
     * The methode receive the object for the given MCRObjectID and stored it in
     * this MCRObject.
     * 
     * @param id
     *            the object ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public final void receiveFromDatastore(MCRObjectID id) throws MCRPersistenceException {
        setFromXML(receiveXMLFromDatastore(id), false);
    }

    /**
     * The methode receive the object for the given MCRObjectID and returned it
     * as XML stream.
     * 
     * @param id
     *            the object ID
     * @return the XML stream of the object as string
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public static final byte[] receiveXMLFromDatastore(String id) throws MCRPersistenceException {
        return receiveXMLFromDatastore(new MCRObjectID(id));
    }

    /**
     * The methode receive the object for the given MCRObjectID and returned it
     * as XML stream.
     * 
     * @param id
     *            the object ID
     * @return the XML stream of the object as string
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public static final byte[] receiveXMLFromDatastore(MCRObjectID id) throws MCRPersistenceException {
        return MCRXMLTableManager.instance().retrieve(id);
    }

    /**
     * The methode receive the object for the given MCRObjectID and returned it
     * as JDOM Document.
     * 
     * @param id
     *            the object ID
     * @return the JDOM Document of the object
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public final org.jdom.Document receiveJDOMFromDatastore(String id) throws MCRPersistenceException {
        return receiveJDOMFromDatastore(new MCRObjectID(id));
    }

    /**
     * The methode receive the object for the given MCRObjectID and returned it
     * as JDOM Document.
     * 
     * @param id
     *            the object ID
     * @return the JDOM Document of the object
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public final org.jdom.Document receiveJDOMFromDatastore(MCRObjectID id) throws MCRPersistenceException {
        byte[] xml = receiveXMLFromDatastore(id);
        return MCRXMLHelper.parseXML(xml, false);
    }

    /**
     * The methode update the object in the data store.
     * 
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     * @throws MCRActiveLinkException
     *             if object is created (no real update) and references to it's
     *             id already exist
     */
    public final void updateInDatastore() throws MCRPersistenceException, MCRActiveLinkException {
        // get the old Item
        MCRObject old = new MCRObject();

        try {
            old.receiveFromDatastore(mcr_id);
        } catch (MCRPersistenceException pe) {
            createInDatastore();

            return;
        }

        // clean the structure
        mcr_struct.clearChildren();
        mcr_struct.clearDerivate();

        // set the derivate data in structure
        for (int i = 0; i < old.mcr_struct.getDerivateSize(); i++) {
            mcr_struct.addDerivate(old.mcr_struct.getDerivate(i));
        }

        // set the parent from the original and this update
        boolean setparent = false;

        if ((old.mcr_struct.getParent() != null) && (mcr_struct.getParent() != null)) {
            String oldparent = old.mcr_struct.getParent().getXLinkHref();
            String newparent = mcr_struct.getParent().getXLinkHref();

            if (!newparent.equals(oldparent)) {
                // remove child from the old parent
                logger.debug("Parent ID = " + oldparent);

                try {
                    MCRObject parent = new MCRObject();
                    parent.receiveFromDatastore(oldparent);
                    parent.mcr_struct.removeChild(mcr_id);
                    parent.updateThisInDatastore();
                    setparent = true;
                } catch (Exception e) {
                    logger.debug(MCRException.getStackTraceAsString(e));
                    logger.error("Error while delete child ID in parent object.");
                    logger.warn("Attention, the parent " + oldparent + "is now inconsist.");
                }
            }
        }

        if ((old.mcr_struct.getParent() != null) && (mcr_struct.getParent() == null)) {
            String oldparent = old.mcr_struct.getParent().getXLinkHref();

            // remove child from the old parent
            logger.debug("Parent ID = " + oldparent);

            try {
                MCRObject parent = new MCRObject();
                parent.receiveFromDatastore(oldparent);
                parent.mcr_struct.removeChild(mcr_id);
                parent.updateThisInDatastore();
                setparent = true;
            } catch (Exception e) {
                logger.debug(MCRException.getStackTraceAsString(e));
                logger.error("Error while delete child ID in parent object.");
                logger.warn("Attention, the parent " + oldparent + "is now inconsist.");
            }
        }

        if ((old.mcr_struct.getParent() == null) && (mcr_struct.getParent() != null)) {
            setparent = true;
        }

        // set the children from the original
        for (int i = 0; i < old.mcr_struct.getChildSize(); i++) {
            mcr_struct.addChild(old.mcr_struct.getChild(i));
        }

        // import all herited matadata from the parent
        MCRObjectID parent_id = mcr_struct.getParentID();

        if (parent_id != null) {
            logger.debug("Parent ID = " + parent_id.getId());

            try {
                MCRObject parent = new MCRObject();
                parent.receiveFromDatastore(parent_id);
                mcr_metadata.appendMetadata(parent.getMetadata().getHeritableMetadata());
            } catch (Exception e) {
                logger.error(MCRException.getStackTraceAsString(e));
                logger.error("Error while merging metadata in this object.");
            }
        }

        // if not imported via cli, createdate remains unchanged
        if (!importMode || mcr_service.getDate("createdate") == null) {
            mcr_service.setDate("createdate", old.getService().getDate("createdate"));
        }

        // update this dataset
        updateThisInDatastore();

        // check if the parent was new set and set them
        if (setparent) {
            try {
                MCRObject parent = new MCRObject();
                parent.receiveFromDatastore(parent_id);
                parent.getStructure().addChild(mcr_id, mcr_struct.getParent().getXLinkLabel(), mcr_label);
                parent.updateThisInDatastore();
            } catch (Exception e) {
                logger.debug(MCRException.getStackTraceAsString(e));
                logger.error("Error while store child ID in parent object.");
                try {
                    deleteFromDatastore();
                    logger.error("Child object was removed.");
                } catch (MCRActiveLinkException e1) {
                    // it shouldn't be possible to have allready links to this
                    // object
                    logger.error("Error while deleting child object.", e1);
                }

                return;
            }
        }

        // update all children
        boolean updatechildren = false;
        MCRObjectMetadata md = getMetadata();
        MCRObjectMetadata mdold = old.getMetadata();
        for (int i = 0; i < md.size(); i++) {
            MCRMetaElement melm = md.getMetadataElement(i);
            if (melm.getHeritable()) {
                try {
                    MCRMetaElement melmold = mdold.getMetadataElement(melm.getTag());
                    Element jelm = melm.createXML(false);
                    Element jelmold = melmold.createXML(false);
                    if (!MCRXMLHelper.deepEqual(new Document(jelmold), new Document(jelm)))
                    	updatechildren = true;
                } catch (RuntimeException e) {
                    updatechildren = true;
                }
            }
        }
        if (updatechildren) {
            for (int i = 0; i < mcr_struct.getChildSize(); i++) {
                MCRObject child = new MCRObject();
                child.updateMetadataInDatastore(mcr_struct.getChild(i).getXLinkHrefID());
            }
        }
    }

    /**
     * The method updates this object in the persistence layer.
     */
    private final void updateThisInDatastore() throws MCRPersistenceException {
        if (!importMode || mcr_service.getDate("modifydate") == null) {
            mcr_service.setDate("modifydate");
        }
        // handle events
        MCREvent evt = new MCREvent(MCREvent.OBJECT_TYPE, MCREvent.UPDATE_EVENT);
        evt.put("object", this);
        MCREventManager.instance().handleEvent(evt);
    }

    /**
     * The method update the metadata of the stored dataset and replace the
     * inherited data from the parent.
     * 
     * @param child_id
     *            the MCRObjectID of the parent as string
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    private final void updateMetadataInDatastore(MCRObjectID child_id) throws MCRPersistenceException {
        logger.debug("Update metadata from Child " + child_id.getId());

        // get the XML Stream for the child_id
        receiveFromDatastore(child_id);

        // delete the old inherited data from all metadata elements
        for (int i = 0; i < mcr_metadata.size(); i++) {
            mcr_metadata.getMetadataElement(i).removeInheritedObject();

            if (mcr_metadata.getMetadataElement(i).size() == 0) {
                mcr_metadata.removeMetadataElement(i);
                i--;
            }
        }

        // import all herited matadata from the parent
        MCRObjectID parent_id = mcr_struct.getParentID();

        if (parent_id != null) {
            logger.debug("Parent ID = " + parent_id.getId());

            try {
                MCRObject parent = new MCRObject();
                parent.receiveFromDatastore(parent_id);
                mcr_metadata.appendMetadata(parent.getMetadata().getHeritableMetadata());
            } catch (Exception e) {
                logger.error(MCRException.getStackTraceAsString(e));
                logger.error("Error while merging metadata in this object.");
            }
        }

        // update this dataset
        updateThisInDatastore();

        // update all children
        for (int i = 0; i < mcr_struct.getChildSize(); i++) {
            MCRObject child = new MCRObject();
            child.updateMetadataInDatastore(mcr_struct.getChild(i).getXLinkHrefID());
        }
    }

    /**
     * The method updates the search index with the data from the XLM store.
     * Also it check the derivate links of itself.
     * 
     * @param id
     *            the MCRObjectID as string
     */
    public final void repairPersitenceDatastore(String id) throws MCRPersistenceException {
        repairPersitenceDatastore(new MCRObjectID(id));
    }

    /**
     * The method updates the search index with the data from the XLM store.
     * Also it check the derivate links of itself.
     * 
     * @param id
     *            the MCRObjectID
     */
    public final void repairPersitenceDatastore(MCRObjectID id) throws MCRPersistenceException {
        // receive metadata for ID
        receiveFromDatastore(id);
        // check derivate link
        MCRMetaLinkID link = null;
        for (int i = 0; i < mcr_struct.getDerivateSize(); i++) {
            link = mcr_struct.getDerivate(i);
            if (!MCRDerivate.existInDatastore(link.getXLinkHref())) {
                logger.error("Can't find MCRDerivate " + link.getXLinkHref());
            }
        }
        // handle events
        MCREvent evt = new MCREvent(MCREvent.OBJECT_TYPE, MCREvent.REPAIR_EVENT);
        evt.put("object", this);
        MCREventManager.instance().handleEvent(evt);
    }

    /**
     * The method print all informations about this MCRObject.
     */
    public final void debug() {
        logger.debug("MCRObject ID : " + mcr_id.getId());
        logger.debug("MCRObject Label : " + mcr_label);
        logger.debug("MCRObject Schema : " + mcr_schema);
        logger.debug("");
    }
}
