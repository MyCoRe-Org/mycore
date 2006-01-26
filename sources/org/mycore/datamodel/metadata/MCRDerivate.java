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
        mcr_id = new MCRObjectID(jdom_element_root.getAttribute("ID").getValue());
        mcr_label = jdom_element_root.getAttribute("label").getValue().trim();

        if (mcr_label.length() > MAX_LABEL_LENGTH) {
            mcr_label = mcr_label.substring(0, MAX_LABEL_LENGTH);
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
     * @param service
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

        // prepare the derivate metadata and store under the XML table
        mcr_service.setDate("createdate");
        mcr_service.setDate("modifydate");
        // handle events
        MCREvent evt = new MCREvent(MCREvent.DERIVATE_TYPE, MCREvent.CREATE_EVENT);
        evt.put("derivate", this);
        MCREventManager.instance().handleEvent(evt);

        // create data in IFS
        if (getDerivate().getInternals() != null) {
            File f = new File(getDerivate().getInternals().getSourcePath());

            if ((!f.isDirectory()) && (!f.isFile())) {
                // handle events
                evt = new MCREvent(MCREvent.DERIVATE_TYPE, MCREvent.DELETE_EVENT);
                evt.put("derivate", this);
                MCREventManager.instance().handleEvent(evt);
                throw new MCRPersistenceException("The File or Directory on " + getDerivate().getInternals().getSourcePath() + " was not found.");
            }

            try {
                MCRDirectory difs = MCRFileImportExport.importFiles(f, mcr_id.getId());
                getDerivate().getInternals().setIFSID(difs.getID());
            } catch (Exception e) {
                // handle events
                evt = new MCREvent(MCREvent.DERIVATE_TYPE, MCREvent.DELETE_EVENT);
                evt.put("derivate", this);
                MCREventManager.instance().handleEvent(evt);
                throw new MCRPersistenceException("Error while creating " + getDerivate().getInternals().getSourcePath() + " in the IFS.", e);
            }
        }

        // add the link to metadata
        MCRObject obj;

        for (int i = 0; i < getDerivate().getLinkMetaSize(); i++) {
            MCRMetaLinkID meta = getDerivate().getLinkMeta(i);
            MCRMetaLinkID der = new MCRMetaLinkID();
            der.setReference(mcr_id.getId(), mcr_label, "");
            der.setSubTag("derobject");

            try {
                obj = new MCRObject();
                obj.addDerivateInDatastore(meta.getXLinkHref(), der);
            } catch (Exception e) {
                // delete from IFS
                MCRDirectory difs = MCRDirectory.getRootDirectory(mcr_id.getId());
                difs.delete();

                // delete from the XML table
                // handle events
                evt = new MCREvent(MCREvent.DERIVATE_TYPE, MCREvent.DELETE_EVENT);
                evt.put("derivate", this);
                MCREventManager.instance().handleEvent(evt);

                // throw final exception
                throw new MCRPersistenceException("Error while creatlink to MCRObject " + meta.getXLinkHref() + ".", e);
            }
        }
    }

    /**
     * The methode delete the object in the data store.
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
        receiveFromDatastore(mcr_id);

        // remove link
        for (int i = 0; i < getDerivate().getLinkMetaSize(); i++) {
            MCRMetaLinkID meta = getDerivate().getLinkMeta(i);
            MCRMetaLinkID der = new MCRMetaLinkID();
            der.setReference(mcr_id.getId(), mcr_label, "");
            der.setSubTag("derobject");

            try {
                MCRObject obj = new MCRObject();
                obj.removeDerivateInDatastore(meta.getXLinkHref(), der);
            } catch (MCRException e) {
                logger.warn("Error while delete link from MCRObject " + meta.getXLinkHref() + ".");
            }
        }

        // delete data from IFS
        try {
            MCRDirectory difs = MCRDirectory.getRootDirectory(mcr_id.getId());
            difs.delete();
        } catch (Exception e) {
            if (getDerivate().getInternals() != null) {
                logger.warn("Error while delete from IFS for ID " + getDerivate().getInternals().getIFSID());
            }
        }

        // handle events
        MCREvent evt = new MCREvent(MCREvent.DERIVATE_TYPE, MCREvent.DELETE_EVENT);
        evt.put("derivate", this);
        MCREventManager.instance().handleEvent(evt);
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
            ret = Boolean.getBoolean((String) evt.get("exist"));
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
            throw new MCRPersistenceException("The XML file for ID " + mcr_id.getId() + " was not retrieved.",e);
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

        for (int i = 0; i < old.getDerivate().getLinkMetaSize(); i++) {
            MCRMetaLinkID meta = old.getDerivate().getLinkMeta(i);
            MCRMetaLinkID der = new MCRMetaLinkID();
            der.setReference(mcr_id.getId(), mcr_label, "");
            der.setSubTag("derivate");

            try {
                obj = new MCRObject();
                obj.removeDerivateInDatastore(meta.getXLinkHref(), der);
            } catch (MCRException e) {
                System.out.println(e.getMessage());
            }
        }

        // update to IFS
        if (getDerivate().getInternals() != null) {
            File f = new File(getDerivate().getInternals().getSourcePath());

            if ((!f.isDirectory()) && (!f.isFile())) {
                throw new MCRPersistenceException("The File or Directory on " + getDerivate().getInternals().getSourcePath() + " was not found.");
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
        for (int i = 0; i < getDerivate().getLinkMetaSize(); i++) {
            MCRMetaLinkID meta = getDerivate().getLinkMeta(i);
            MCRMetaLinkID der = new MCRMetaLinkID();
            der.setReference(mcr_id.getId(), mcr_label, "");
            der.setSubTag("derobject");

            try {
                obj = new MCRObject();
                obj.addDerivateInDatastore(meta.getXLinkHref(), der);
            } catch (MCRException e) {
                throw new MCRPersistenceException("The MCRObject " + meta.getXLinkHref() + " was not found.");
            }
        }
    }

    /**
     * The methode update only the XML part of the object in the data store.
     * 
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public final void updateXMLInDatastore() throws MCRPersistenceException {
        mcr_service.setDate("modifydate");
        MCREvent evt = new MCREvent(MCREvent.DERIVATE_TYPE, MCREvent.UPDATE_EVENT);
        evt.put("derivate", this);
        MCREventManager.instance().handleEvent(evt);
    }

}
