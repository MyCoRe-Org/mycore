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

import java.io.File;

import org.apache.log4j.Logger;

import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRDefaults;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFileImportExport;

/**
 * This class implements all methode for handling one derivate object. Methodes
 * of this class can read the XML metadata by using a XML parser, manipulate the
 * data in the abstract persistence data store and return the XML stream to the
 * user application.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
final public class MCRDerivate extends MCRBase {
    /**
     * constant value for the object id length
     */

    // the object content
    private MCRObjectDerivate mcr_derivate = null;

    private static final Logger LOGGER = Logger.getLogger(MCRDerivate.class);

    /**
     * This is the constructor of the MCRDerivate class. It make an instance of
     * the parser class and the metadata class.
     * 
     * @exception MCRException
     *                general Exception of MyCoRe
     * @exception MCRConfigurationException
     *                a special exception for configuartion data
     */
    public MCRDerivate() throws MCRException, MCRConfigurationException {
        super();

        // Derivate class
        mcr_derivate = new MCRObjectDerivate();
    }

    /**
     * This methode return the instance of the MCRObjectDerivate class. If this
     * was not found, null was returned.
     * 
     * @return the instance of the MCRObjectDerivate class
     */
    public final MCRObjectDerivate getDerivate() {
        return mcr_derivate;
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

        mcr_schema = jdom_element_root.getAttribute("noNamespaceSchemaLocation", org.jdom.Namespace.getNamespace("xsi", MCRDefaults.XSI_URL)).getValue().trim();

        org.jdom.Element jdom_element;

        // get the derivate data of the object
        jdom_element = jdom_element_root.getChild("derivate");
        mcr_derivate.setFromDOM(jdom_element);

        // get the service data of the object
        jdom_element = jdom_element_root.getChild("service");
        mcr_service.setFromDOM(jdom_element);
    }

    /**
     * This methode read the XML input stream from an URI into a temporary DOM
     * and check it with XSchema file.
     * 
     * @param uri
     *            an URI
     * @exception MCRException
     *                general Exception of MyCoRe
     */
    public final void setFromURI(String uri) throws MCRException {
        try {
            jdom_document = MCRXMLHelper.parseURI(uri);
        } catch (Exception e) {
            throw new MCRException(e.getMessage());
        }

        set();
    }

    /**
     * This methode read the XML input stream from a byte array into JDOM and
     * check it with XSchema file.
     * 
     * @param xml
     *            a XML string
     * @exception MCRException
     *                general Exception of MyCoRe
     */
    public final void setFromXML(byte[] xml, boolean valid) throws MCRException {
        try {
            jdom_document = MCRXMLHelper.parseXML(xml, false);
        } catch (Exception e) {
            throw new MCRException(e.getMessage());
        }

        set();
    }

    /**
     * This methode set the object MCRObjectDerivate.
     * 
     * @param derivate
     *            the object MCRObjectDerivate part
     */
    public final void setDerivate(MCRObjectDerivate derivate) {
        if (derivate != null) {
            mcr_derivate = derivate;
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

        org.jdom.Element elm = new org.jdom.Element("mycorederivate");
        org.jdom.Document doc = new org.jdom.Document(elm);
        elm.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xsi", MCRDefaults.XSI_URL));
        elm.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
        elm.setAttribute("noNamespaceSchemaLocation", mcr_schema, org.jdom.Namespace.getNamespace("xsi", MCRDefaults.XSI_URL));
        elm.setAttribute("ID", mcr_id.getId());
        elm.setAttribute("label", mcr_label);
        elm.setAttribute("version", mcr_version);
        elm.addContent(mcr_derivate.createXML());
        elm.addContent(mcr_service.createXML());

        return doc;
    }

    /**
     * The methode create the object in the data store.
     * 
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public final void createInDatastore() throws MCRPersistenceException {
        // exist the derivate?
        if (existInDatastore(mcr_id.getId())) {
            throw new MCRPersistenceException("The derivate " + mcr_id.getId() + " allready exists, nothing done.");
        }

        if (!isValid()) {
            throw new MCRPersistenceException("The derivate " + mcr_id.getId() + " is not valid.");
        }

        // prepare the derivate metadata and store under the XML table
        if (mcr_service.getDate("createdate") == null || !importMode) {
            mcr_service.setDate("createdate");
        }
        if (mcr_service.getDate("modifydate") == null || !importMode) {
            mcr_service.setDate("modifydate");
        }

        // handle events
        LOGGER.debug("Handling derivate CREATE event");
        MCREvent evt = new MCREvent(MCREvent.DERIVATE_TYPE, MCREvent.CREATE_EVENT);
        evt.put("derivate", this);
        MCREventManager.instance().handleEvent(evt);

        // add the link to metadata
        MCRMetaLinkID meta = getDerivate().getMetaLink();
        MCRMetaLinkID der = new MCRMetaLinkID();
        der.setReference(mcr_id.getId(), mcr_label, "");
        der.setSubTag("derobject");
        byte[] backup = MCRXMLTableManager.instance().retrieve(meta.getXLinkHrefID());

        try {
            MCRObject obj = new MCRObject();
            LOGGER.debug("adding Derivate in data store");
            obj.addDerivateInDatastore(meta.getXLinkHref(), der);
        } catch (Exception e) {
            restoreMCRObject(backup);
            // throw final exception
            throw new MCRPersistenceException("Error while creatlink to MCRObject " + meta.getXLinkHref() + ".", e);
        }

        // create data in IFS
        if (getDerivate().getInternals() != null) {
            if (getDerivate().getInternals().getSourcePath() == null) {
                MCRDirectory difs = new MCRDirectory(mcr_id.getId(), mcr_id.getId());
                getDerivate().getInternals().setIFSID(difs.getID());
            } else {
                String sourcepath = getDerivate().getInternals().getSourcePath();
                File f = new File(sourcepath);
                if (f.exists()) {
                    MCRDirectory difs = null;
                    try {
                        LOGGER.debug("Starting File-Import");
                        difs = MCRFileImportExport.importFiles(f, mcr_id.getId());
                        getDerivate().getInternals().setIFSID(difs.getID());
                    } catch (Exception e) {
                        if (difs != null) {
                            difs.delete();
                        }
                        restoreMCRObject(backup);
                        throw new MCRPersistenceException("Can't add derivate to the IFS", e);
                    }
                } else {
                    LOGGER.warn("Empty derivate, the File or Directory -->" + sourcepath + "<--  was not found.");
                }
            }
        }
    }

    private void restoreMCRObject(byte[] backup) {
        MCREvent evt;
        // restore original instance of MCRObject
        MCRObject obj = new MCRObject();
        obj.setFromXML(backup, false);
        try {
            obj.updateInDatastore();
        } catch (MCRActiveLinkException e1) {
            LOGGER.warn("Error while restoring " + obj.getId(), e1);
        } finally {
            // delete from the XML table
            // handle events
            evt = new MCREvent(MCREvent.DERIVATE_TYPE, MCREvent.DELETE_EVENT);
            evt.put("derivate", this);
            MCREventManager.instance().handleEvent(evt);
        }
    }

    /**
     * The methode delete the object in the data store. The order of delete
     * steps is:<br />
     * <ul>
     * <li>remove link in object metadata</li>
     * <li>remove all files from IFS</li>
     * <li>remive itself</li>
     * </ul>
     * 
     * @param id
     *            the object ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public final void deleteFromDatastore(String id) throws MCRPersistenceException {
        // get the derivate
        try {
            mcr_id = new MCRObjectID(id);
        } catch (MCRException e) {
            throw new MCRPersistenceException("ID error in deleteFromDatastore for " + id + ".", e);
        }

        // receive derivate
        boolean receivetest = false;
        try {
            receiveFromDatastore(mcr_id);
            LOGGER.info("Dataset for MCRDerivate " + mcr_id.getId() + " was received.");
            receivetest = true;
        } catch (MCRPersistenceException e) {
            LOGGER.warn("Error while delete MCRDerivate, can't receive data for " + mcr_id.getId() + ".");
        }

        // remove link
        if (receivetest) {
            String meta = "?";
            try {
                meta = getDerivate().getMetaLink().getXLinkHref();
                MCRObject obj = new MCRObject();
                obj.removeDerivateInDatastore(meta, mcr_id.getId());
                LOGGER.info("Link in MCRObject " + meta + " to MCRDerivate " + mcr_id.getId() + " is deleted.");
            } catch (MCRException e) {
                LOGGER.warn("Error while delete link for MCRDerivate " + mcr_id.getId() + "from MCRObject " + meta + ".");
            }
        }

        // delete data from IFS
        try {
            MCRDirectory difs = MCRDirectory.getRootDirectory(mcr_id.getId());
            difs.delete();
            LOGGER.info("IFS entries for MCRDerivate " + mcr_id.getId() + " are deleted.");
        } catch (Exception e) {
            if (getDerivate().getInternals() != null) {
                LOGGER.warn("Error while delete for ID " + mcr_id.getId() + " from IFS with ID " + getDerivate().getInternals().getIFSID());
            }
        }

        // handle events
        if (receivetest) {
            MCREvent evt = new MCREvent(MCREvent.DERIVATE_TYPE, MCREvent.DELETE_EVENT);
            evt.put("derivate", this);
            MCREventManager.instance().handleEvent(evt);
        }
    }

    /**
     * The methode return true if the derivate is in the data store, else return
     * false.
     * 
     * @param id
     *            the derivate ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public final static boolean existInDatastore(String id) throws MCRPersistenceException {
        return existInDatastore(new MCRObjectID(id));
    }

    /**
     * The methode return true if the derivate is in the data store, else return
     * false.
     * 
     * @param id
     *            the derivate ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public final static boolean existInDatastore(MCRObjectID id) throws MCRPersistenceException {
        // handle events
        MCREvent evt = new MCREvent(MCREvent.DERIVATE_TYPE, MCREvent.EXIST_EVENT);
        evt.put("objectID", id);
        MCREventManager.instance().handleEvent(evt);
        boolean ret = false;
        try {
            ret = Boolean.valueOf((String) evt.get("exist")).booleanValue();
        } catch (RuntimeException e) {
        }
        return ret;
    }

    /**
     * The methode receive the derivate for the given MCRObjectID and stored it
     * in this MCRDerivate
     * 
     * @param id
     *            the derivate ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public final void receiveFromDatastore(String id) throws MCRPersistenceException {
        receiveFromDatastore(new MCRObjectID(id));
    }

    /**
     * The methode receive the derivate for the given MCRObjectID and stored it
     * in this MCRDerivate
     * 
     * @param id
     *            the derivate ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public final void receiveFromDatastore(MCRObjectID id) throws MCRPersistenceException {
        byte xml[] = receiveXMLFromDatastore(id);
        setFromXML(xml, false);
    }

    /**
     * The methode receive the derivate for the given MCRObjectID and returned
     * it as XML stream.
     * 
     * @param id
     *            the derivate ID
     * @return the XML stream of the object as string
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public final byte[] receiveXMLFromDatastore(String id) throws MCRPersistenceException {
        return receiveXMLFromDatastore(new MCRObjectID(id));
    }

    /**
     * The methode receive the derivate for the given MCRObjectID and returned
     * it as XML stream.
     * 
     * @param id
     *            the derivate ID
     * @return the XML stream of the object as string
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public final byte[] receiveXMLFromDatastore(MCRObjectID id) throws MCRPersistenceException {
        // handle events
        MCREvent evt = new MCREvent(MCREvent.DERIVATE_TYPE, MCREvent.RECEIVE_EVENT);
        evt.put("objectID", id);
        MCREventManager.instance().handleEvent(evt);
        byte[] xml = null;
        try {
            xml = (byte[]) evt.get("xml");
        } catch (RuntimeException e) {
            throw new MCRPersistenceException("The XML file for ID " + mcr_id.getId() + " was not retrieved.", e);
        }
        return xml;
    }

    /**
     * The methode receive the multimedia object(s) for the given MCRObjectID
     * and returned it as MCRDirectory.
     * 
     * @param id
     *            the object ID
     * @return the MCRDirectory of the multimedia object
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public final MCRDirectory receiveDirectoryFromIFS(String id) throws MCRPersistenceException {
        // check the ID
        mcr_id = new MCRObjectID(id);

        // receive the IFS informations
        MCRDirectory difs = MCRDirectory.getRootDirectory(mcr_id.getId());

        if (difs == null) {
            throw new MCRPersistenceException("Error while receiving derivate with " + "ID " + mcr_id.getId() + " from IFS.");
        }

        return difs;
    }

    /**
     * The methode update the object in the data store.
     * 
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public final void updateInDatastore() throws MCRPersistenceException {
        // get the old Item
        MCRDerivate old = new MCRDerivate();

        try {
            old.receiveFromDatastore(mcr_id.getId());
        } catch (MCRException e) {
            createInDatastore();
            return;
        }

        // remove the old link to metadata
        MCRObject obj;
        String meta_id = "";
        try {
            meta_id = old.getDerivate().getMetaLink().getXLinkHref();
            obj = new MCRObject();
            obj.removeDerivateInDatastore(meta_id, mcr_id.getId());
        } catch (MCRException e) {
            System.out.println(e.getMessage());
        }

        // update to IFS
        if ((getDerivate().getInternals() != null) && (getDerivate().getInternals().getSourcePath() != null)) {
            File f = new File(getDerivate().getInternals().getSourcePath());

            if (!f.exists()) {
                throw new MCRPersistenceException("The File or Directory " + getDerivate().getInternals().getSourcePath() + " was not found.");
            }

            try {
                MCRDirectory difs = MCRDirectory.getRootDirectory(mcr_id.getId());
                MCRFileImportExport.importFiles(f, difs);
                getDerivate().getInternals().setIFSID(difs.getID());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // update the derivate
        mcr_service.setDate("createdate", old.getService().getDate("createdate"));
        updateXMLInDatastore();

        // add the link to metadata
        String meta = "";
        try {
            meta = getDerivate().getMetaLink().getXLinkHref();
            MCRMetaLinkID der = new MCRMetaLinkID();
            der.setReference(mcr_id.getId(), mcr_label, "");
            der.setSubTag("derobject");
            obj = new MCRObject();
            obj.addDerivateInDatastore(meta, der);
        } catch (MCRException e) {
            throw new MCRPersistenceException("The MCRObject " + meta + " was not found.");
        }
    }

    /**
     * The methode update only the XML part of the object in the data store.
     * 
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public final void updateXMLInDatastore() throws MCRPersistenceException {
        if (!importMode || mcr_service.getDate("modifydate") == null) {
            mcr_service.setDate("modifydate");
        }
        MCREvent evt = new MCREvent(MCREvent.DERIVATE_TYPE, MCREvent.UPDATE_EVENT);
        evt.put("derivate", this);
        MCREventManager.instance().handleEvent(evt);
    }

    /**
     * The method updates the indexer of content.
     * 
     * @param id
     *            the MCRObjectID as string
     */
    public final void repairPersitenceDatastore(String id) throws MCRPersistenceException {
        repairPersitenceDatastore(new MCRObjectID(id));
    }

    /**
     * The method updates the indexer of content.
     * 
     * @param id
     *            the MCRObjectID
     */
    public final void repairPersitenceDatastore(MCRObjectID id) throws MCRPersistenceException {
        receiveFromDatastore(id);
        // handle events
        MCREvent evt = new MCREvent(MCREvent.DERIVATE_TYPE, MCREvent.REPAIR_EVENT);
        evt.put("derivate", this);
        MCREventManager.instance().handleEvent(evt);
    }

    /**
     * The method print all informations about this MCRObject.
     */
    public final void debug() {
        LOGGER.debug("MCRDerivate ID : " + mcr_id.getId());
        LOGGER.debug("MCRDerivate Label : " + mcr_label);
        LOGGER.debug("MCRDerivate Schema : " + mcr_schema);
        LOGGER.debug("");
    }

    public boolean isValid() {
        if (!super.isValid()) {
            LOGGER.warn("MCRBase.isValid() == false;");
            return false;
        }
        if (!getDerivate().isValid()) {
            LOGGER.warn("MCRObjectDerivate.isValid() == false;");
            return false;
        }
        return true;
    }
}
