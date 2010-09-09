/*
 * 
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

import java.net.URI;
import java.util.Collection;

import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.xml.sax.SAXParseException;

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
        mcr_struct = new MCRObjectStructure(LOGGER);
    }

    /**
     * @param bytes
     * @param valid
     * @throws SAXParseException
     */
    public MCRObject(byte[] bytes, boolean valid) throws SAXParseException {
        super(bytes, valid);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param doc
     * @throws SAXParseException
     */
    public MCRObject(Document doc) throws SAXParseException {
        super(doc);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param uri
     * @throws SAXParseException
     */
    public MCRObject(URI uri) throws SAXParseException {
        super(uri);
        // TODO Auto-generated constructor stub
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
    protected final void setUp() throws MCRException {
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
        if (mcr_version == null || (mcr_version = mcr_version.trim()).length() == 0) {
            setVersion();
        }

        mcr_schema = jdom_element_root.getAttribute("noNamespaceSchemaLocation", XSI_NAMESPACE).getValue().trim();
        LOGGER.debug("MCRObject set schemafile: " + mcr_schema);
    }

    private void setStructure() {
        // get the structure data of the object
        org.jdom.Element jdom_element_root = jdom_document.getRootElement();
        org.jdom.Element jdom_element = jdom_element_root.getChild("structure");
        mcr_struct = new MCRObjectStructure(LOGGER);
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

        if (tag == null || (tag = tag.trim()).length() == 0) {
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
    @Override
    public final org.jdom.Document createXML() throws MCRException {
        if (!isValid()) {
            throw new MCRException("The content is not valid.");
        }

        org.jdom.Element elm = new org.jdom.Element("mycoreobject");
        org.jdom.Document doc = new org.jdom.Document(elm);
        elm.addNamespaceDeclaration(XSI_NAMESPACE);
        elm.addNamespaceDeclaration(XLINK_NAMESPACE);
        elm.setAttribute("noNamespaceSchemaLocation", mcr_schema, XSI_NAMESPACE);
        elm.setAttribute("ID", mcr_id.toString());
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
    @Override
    public final void createInDatastore() throws MCRPersistenceException, MCRActiveLinkException {
        // exist the object?
        if (existInDatastore(mcr_id.toString())) {
            throw new MCRPersistenceException("The object " + mcr_id.toString() + " allready exists, nothing done.");
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
            LOGGER.debug("Parent ID = " + parent_id.toString());

            try {
                parent = MCRObject.createFromDatastore(parent_id);
                mcr_metadata.appendMetadata(parent.getMetadata().getHeritableMetadata());
            } catch (Exception e) {
                LOGGER.error(MCRException.getStackTraceAsString(e));
                LOGGER.error("Error while merging metadata in this object.");

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
                LOGGER.debug(MCRException.getStackTraceAsString(e));
                LOGGER.error("Error while store child ID in parent object.");
                try {
                    deleteFromDatastore();
                    LOGGER.error("Child object was removed.");
                } catch (MCRActiveLinkException e1) {
                    // it shouldn't be possible to have allready links to this
                    // object
                    LOGGER.error("Error while deleting child object.", e1);
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
    public static final void addDerivateInDatastore(String id, MCRMetaLinkID link) throws MCRPersistenceException {
        MCRObject object = createFromDatastore(new MCRObjectID(id));
        // don't put the same derivates twice in an object!
        MCRMetaLinkID testlink = null;
        boolean checklink = false;
        for (int i = 0; i < object.mcr_struct.getDerivateSize(); i++) {
            testlink = object.mcr_struct.getDerivate(i);
            if (link.href.equals(testlink.getXLinkHref())) {
                checklink = true;
            }
        }
        if (checklink) {
            return;
        }
        // add link
        if (!object.importMode) {
            object.mcr_service.setDate("modifydate");
        }
        object.mcr_struct.addDerivate(link);
        object.updateThisInDatastore();
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
    public static final void removeDerivateInDatastore(String id, String der_id) throws MCRPersistenceException {
        MCRObject object = createFromDatastore(new MCRObjectID(id));
        object.mcr_service.setDate("modifydate");
        MCRMetaLinkID link = null;
        for (int i = 0; i < object.mcr_struct.getDerivateSize(); i++) {
            link = object.mcr_struct.getDerivate(i);
            if (link.href.equals(der_id)) {
                object.mcr_struct.removeDerivate(i);
            }
        }
        object.updateThisInDatastore();
    }

    @Override
    public void deleteFromDatastore() throws MCRPersistenceException, MCRActiveLinkException {
        if (this.mcr_id == null) {
            throw new MCRPersistenceException("The MCRObjectID is null.");
        }

        // check for active links
        Collection<String> sources = MCRLinkTableManager.instance().getSourceOf(this.mcr_id);
        LOGGER.debug("Sources size:" + sources.size());
        if (sources.size() > 0) {
            MCRActiveLinkException activeLinks = new MCRActiveLinkException(new StringBuffer("Error while deleting object ")
                .append(this.mcr_id.toString())
                .append(". This object is still referenced by other objects and can not be removed until all links are released.")
                .toString());
            for (String curSource : sources) {
                activeLinks.addLink(curSource, this.mcr_id.toString());
            }
            throw activeLinks;
        }

        for (int i = 0; i < this.mcr_struct.getDerivateSize(); i++) {

            MCRObjectID derId = new MCRObjectID(getStructure().getDerivate(i).getXLinkHref());
            try {
                MCRDerivate.deleteFromDatastore(derId);
            } catch (Exception e) {
                LOGGER.debug(MCRException.getStackTraceAsString(e));
                LOGGER.error(e.getMessage());
                LOGGER.error("Error while deleting derivate " + derId + ".");
            }
        }

        // remove all children
        for (int i = 0; i < this.mcr_struct.getChildSize(); i++) {

            try {
                MCRObjectID childID = new MCRObjectID(this.getStructure().getChild(i).getXLinkHref());
                MCRObject.deleteFromDatastore(childID);
            } catch (MCRException e) {
                LOGGER.debug(MCRException.getStackTraceAsString(e));
                LOGGER.error(e.getMessage());
                LOGGER.error("Error while deleting child.");
            }
        }

        // remove child from parent
        MCRObjectID parent_id = this.mcr_struct.getParentID();

        if (parent_id != null) {
            LOGGER.debug("Parent ID = " + parent_id.toString());

            try {
                MCRObject parent = MCRObject.createFromDatastore(parent_id);
                parent.mcr_struct.removeChild(this.mcr_id);
                parent.updateThisInDatastore();
            } catch (Exception e) {
                LOGGER.debug(MCRException.getStackTraceAsString(e));
                LOGGER.error("Error while delete child ID in parent object.");
                LOGGER.warn("Attention, the parent " + parent_id + "is now inconsist.");
            }
        }

        // handle events
        MCREvent evt = new MCREvent(MCREvent.OBJECT_TYPE, MCREvent.DELETE_EVENT);
        evt.put("object", this);
        MCREventManager.instance().handleEvent(evt, MCREventManager.BACKWARD);
    }

    /**
     * The methode delete the object from the data store.
     * 
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     * @throws MCRActiveLinkException
     */
    public static final void deleteFromDatastore(MCRObjectID id) throws MCRPersistenceException, MCRActiveLinkException {
        MCRObject object = createFromDatastore(id);
        object.deleteFromDatastore();
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
        return MCRXMLMetadataManager.instance().exists(id);
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
    public static final MCRObject createFromDatastore(MCRObjectID id) throws MCRPersistenceException {
        MCRObject obj = new MCRObject();
        obj.setFromJDOM(MCRXMLMetadataManager.instance().retrieveXML(id));
        return obj;
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
        return MCRXMLMetadataManager.instance().retrieveXML(id);
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
    @Override
    public final void updateInDatastore() throws MCRPersistenceException, MCRActiveLinkException {
        // get the old Item
        MCRObject old = new MCRObject();

        try {
            old = MCRObject.createFromDatastore(mcr_id);
        } catch (Exception pe) {
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

        if (old.mcr_struct.getParent() != null && mcr_struct.getParent() != null) {
            MCRObjectID oldparent = new MCRObjectID(old.mcr_struct.getParent().getXLinkHref());
            String newparent = mcr_struct.getParent().getXLinkHref();

            if (!newparent.equals(oldparent)) {
                // remove child from the old parent
                LOGGER.debug("Parent ID = " + oldparent);

                try {
                    MCRObject parent = MCRObject.createFromDatastore(oldparent);
                    parent.mcr_struct.removeChild(mcr_id);
                    parent.updateThisInDatastore();
                    setparent = true;
                } catch (Exception e) {
                    LOGGER.debug(MCRException.getStackTraceAsString(e));
                    LOGGER.error("Error while delete child ID in parent object.");
                    LOGGER.warn("Attention, the parent " + oldparent + "is now inconsist.");
                }
            }
        }

        if (old.mcr_struct.getParent() != null && mcr_struct.getParent() == null) {
            MCRObjectID oldparent = new MCRObjectID(old.mcr_struct.getParent().getXLinkHref());

            // remove child from the old parent
            LOGGER.debug("Parent ID = " + oldparent);

            try {
                MCRObject parent = MCRObject.createFromDatastore(oldparent);
                parent.mcr_struct.removeChild(mcr_id);
                parent.updateThisInDatastore();
                setparent = true;
            } catch (Exception e) {
                LOGGER.debug(MCRException.getStackTraceAsString(e));
                LOGGER.error("Error while delete child ID in parent object.");
                LOGGER.warn("Attention, the parent " + oldparent + "is now inconsist.");
            }
        }

        if (old.mcr_struct.getParent() == null && mcr_struct.getParent() != null) {
            setparent = true;
        }

        // set the children from the original
        for (int i = 0; i < old.mcr_struct.getChildSize(); i++) {
            mcr_struct.addChild(old.mcr_struct.getChild(i));
        }

        // import all herited matadata from the parent
        MCRObjectID parent_id = mcr_struct.getParentID();

        if (parent_id != null) {
            LOGGER.debug("Parent ID = " + parent_id.toString());

            try {
                MCRObject parent = MCRObject.createFromDatastore(parent_id);
                // remove already embedded inherited tags
                mcr_metadata.removeInheritedMetadata();
                // insert heritable tags
                mcr_metadata.appendMetadata(parent.getMetadata().getHeritableMetadata());

            } catch (Exception e) {
                LOGGER.error(MCRException.getStackTraceAsString(e));
                LOGGER.error("Error while merging metadata in this object.");
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
                MCRObject parent = MCRObject.createFromDatastore(parent_id);
                parent.getStructure().addChild(mcr_id, mcr_struct.getParent().getXLinkLabel(), mcr_label);
                parent.updateThisInDatastore();
            } catch (Exception e) {
                LOGGER.debug(MCRException.getStackTraceAsString(e));
                LOGGER.error("Error while store child ID in parent object.");
                try {
                    deleteFromDatastore();
                    LOGGER.error("Child object was removed.");
                } catch (MCRActiveLinkException e1) {
                    // it shouldn't be possible to have allready links to this
                    // object
                    LOGGER.error("Error while deleting child object.", e1);
                }

                return;
            }
        }

        // update all children
        boolean updatechildren = false;
        MCRObjectMetadata md = getMetadata();
        MCRObjectMetadata mdold = old.getMetadata();
        int numheritablemd = 0;
        int numheritablemdold = 0;
        for (int i = 0; i < md.size(); i++) {
            MCRMetaElement melm = md.getMetadataElement(i);
            if (melm.getHeritable()) {
                numheritablemd++;
                try {
                    MCRMetaElement melmold = mdold.getMetadataElement(melm.getTag());
                    Element jelm = melm.createXML(false);
                    Element jelmold = melmold.createXML(false);
                    if (!MCRXMLHelper.deepEqual(new Document(jelmold), new Document(jelm))) {
                        updatechildren = true;
                        break;
                    }
                } catch (RuntimeException e) {
                    updatechildren = true;
                }
            }
        }
        if (!updatechildren) {
            for (int i = 0; i < mdold.size(); i++) {
                MCRMetaElement melmold = mdold.getMetadataElement(i);
                if (melmold.getHeritable()) {
                    numheritablemdold++;
                }
            }
        }
        if (numheritablemd != numheritablemdold) {
            updatechildren = true;
        }
        if (updatechildren) {
            for (int i = 0; i < mcr_struct.getChildSize(); i++) {
                MCRObject.updateMetadataInDatastore(mcr_struct.getChild(i).getXLinkHrefID());
            }
        }
    }

    /**
     * The method updates this object in the persistence layer.
     */
    public final void updateThisInDatastore() throws MCRPersistenceException {
        if (!importMode || mcr_service.getDate("modifydate") == null) {
            mcr_service.setDate("modifydate");
        }
        // remove ACL if it is set from data source
        for (int i = 0; i < mcr_service.getRulesSize(); i++) {
            mcr_service.removeRule(i);
            i--;
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
    private static final void updateMetadataInDatastore(MCRObjectID child_id) throws MCRPersistenceException {
        LOGGER.debug("Update metadata from Child " + child_id.toString());
        MCRObject child = MCRObject.createFromDatastore(child_id);

        // delete the old inherited data from all metadata elements
        for (int i = 0; i < child.mcr_metadata.size(); i++) {
            child.mcr_metadata.getMetadataElement(i).removeInheritedObject();

            if (child.mcr_metadata.getMetadataElement(i).size() == 0) {
                child.mcr_metadata.removeMetadataElement(i);
                i--;
            }
        }

        // import all herited matadata from the parent
        MCRObjectID parent_id = child.mcr_struct.getParentID();

        if (parent_id != null) {
            LOGGER.debug("Parent ID = " + parent_id.toString());

            try {
                MCRObject parent = MCRObject.createFromDatastore(parent_id);
                child.mcr_metadata.appendMetadata(parent.getMetadata().getHeritableMetadata());
            } catch (Exception e) {
                LOGGER.error(MCRException.getStackTraceAsString(e));
                LOGGER.error("Error while merging metadata in this object.");
            }
        }

        // update this dataset
        child.updateThisInDatastore();

        // update all children
        for (int i = 0; i < child.mcr_struct.getChildSize(); i++) {
            MCRObject.updateMetadataInDatastore(child.mcr_struct.getChild(i).getXLinkHrefID());
        }
    }

    /**
     * The method updates the search index with the data from the XLM store.
     * Also it check the derivate links of itself.
     * 
     * @param id
     *            the MCRObjectID
     */
    public final void repairPersitenceDatastore() throws MCRPersistenceException {
        // check derivate link
        MCRMetaLinkID link = null;
        for (int i = 0; i < mcr_struct.getDerivateSize(); i++) {
            link = mcr_struct.getDerivate(i);
            if (!MCRDerivate.existInDatastore(link.getXLinkHref())) {
                LOGGER.error("Can't find MCRDerivate " + link.getXLinkHref());
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
        LOGGER.debug("MCRObject ID : " + mcr_id.toString());
        LOGGER.debug("MCRObject Label : " + mcr_label);
        LOGGER.debug("MCRObject Schema : " + mcr_schema);
        LOGGER.debug("");
        mcr_metadata.debug();
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.metadata.MCRBase#isValid()
     */
    @Override
    public boolean isValid() {
        return super.isValid() && getMetadata().isValid() && getStructure().isValid() && getService().isValid();
    }

    public void checkLinkTargets() {
        for (int i = 0; i < getMetadata().size(); i++) {
            MCRMetaElement elm = getMetadata().getMetadataElement(i);
            for (int j = 0; j < elm.size(); j++) {
                MCRMetaInterface inf = elm.getElement(j);
                if (inf instanceof MCRMetaClassification) {
                    String classID = ((MCRMetaClassification) inf).getClassId();
                    String categID = ((MCRMetaClassification) inf).getCategId();
                    boolean exists = MCRCategoryDAOFactory.getInstance().exist(new MCRCategoryID(classID, categID));
                    if (exists) {
                        continue;
                    }
                    MCRActiveLinkException activeLink = new MCRActiveLinkException("Failure while adding link!. Destination does not exist.");
                    String destination = classID + "##" + categID;
                    activeLink.addLink(getId().toString(), destination);
                    // throw activeLink;
                    // TODO: should trigger undo-Event
                }
                if (inf instanceof MCRMetaLinkID) {
                    String destination = ((MCRMetaLinkID) inf).getXLinkHref();
                    if (!MCRXMLMetadataManager.instance().exists(new MCRObjectID(destination))) {
                        continue;
                    }
                    MCRActiveLinkException activeLink = new MCRActiveLinkException("Failure while adding link!. Destination does not exist.");
                    activeLink.addLink(getId().toString(), destination);
                    // throw activeLink;
                    // TODO: should trigger undo-Event
                }
            }
        }
    }

}
