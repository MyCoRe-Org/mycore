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

package org.mycore.datamodel.classifications;

import static org.jdom.Namespace.XML_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;
import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;

import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.datamodel.common.MCRXMLTableManager;

/**
 * This class implements all methods for a classification and extended the
 * MCRClassificationObject class.
 * 
 * @author Frank Luetzenkirchen
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRClassification extends MCRClassificationItem {

    private static final long serialVersionUID = 4754441996528345601L;

    // logger
    static Logger LOGGER = Logger.getLogger(MCRClassification.class);

    // the classification SQL manager
    static MCRClassificationManager CM = MCRClassificationManager.instance();

    // the service part of MCRObjects
    private MCRObjectService mcr_service = null;

    /**
     * The constructor
     */
    public MCRClassification() {
    }

    /**
     * This methode return the instance of the MCRObjectService class. If this
     * was not found, null was returned.
     * 
     * @return the instance of the MCRObjectService class
     */
    public final MCRObjectService getService() {
        return mcr_service;
    }

    /**
     * This method fill this instance with data from store for a given ID.
     * 
     * @param classID the classification ID
     */
    public final void setFromStore(MCRObjectID classid) {
        byte[] xml = MCRXMLTableManager.instance().retrieve(classid);
        setFromXML(xml);
    }
    
    /**
     * The method create a MCRClassification from the given URI.
     * 
     * @param uri
     *            the classification URI
     * @exception MCRException
     *                if the parser can't build a JDOM tree
     */
    public final void setFromURI(String uri) throws MCRException {
        try {
            org.jdom.Document jdom = MCRXMLHelper.parseURI(uri);
            setFromJDOM(jdom);
        } catch (Exception e) {
            throw new MCRException(e.getMessage(), e);
        }
    }

    /**
     * The method create a MCRClassification from the given XML array.
     * 
     * @param xml
     *            the classification as byte array XML tree
     * @exception MCRException
     *                if the parser can't build a JDOM tree
     */
    public final void setFromXML(byte[] xml) throws MCRException {
        try {
            org.jdom.input.SAXBuilder bulli = new org.jdom.input.SAXBuilder(false);
            org.jdom.Document jdom = bulli.build(new ByteArrayInputStream(xml));
            setFromJDOM(jdom);
        } catch (Exception e) {
            throw new MCRException(e.getMessage());
        }
    }

    /**
     * The method fill the instance of this class with a given JODM tree.
     * 
     * @param jdom
     *            the classification as jdom tree
     */
    public final void setFromJDOM(org.jdom.Document jdom) {
        // get ID
        Element xmlblob = jdom.getRootElement();
        xmlblob.detach();
        setId(xmlblob.getAttributeValue("ID"));
        LOGGER.debug("processing read Classification:" + getId());
        setCounterEnabled(false);
        // get labels
        List tagList = xmlblob.getChildren("label");
        Element tag;
        for (int i = 0; i < tagList.size(); i++) {
            tag = (Element) tagList.get(i);
            MCRLabel label = new MCRLabel(tag.getAttributeValue("lang", XML_NAMESPACE), tag.getAttributeValue("text"), tag.getAttributeValue("description"));
            addLabel(label);
        }
        // get categories
        MCRClassificationTransformer.addChildren(getId(), this, xmlblob.getChild("categories"), -1, false);
        // read service part
        Element service = xmlblob.getChild("service");
        mcr_service = new MCRObjectService();
        if (service != null) {
            mcr_service.setFromDOM(service);
        }
        LOGGER.debug("processing read Classification:" + getId() + " finished.");
    }

    /**
     * This methode create a XML stream for all object data.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Document with the XML data of the object as byte array
     */
    public final org.jdom.Document createXML() throws MCRException {
        Document jdom = MCRClassificationTransformer.getMetaDataDocument(this);
        jdom.getRootElement().addContent(mcr_service.createXML());
        return jdom;
    }

    /**
     * The method create a MCRClassification from these instance.
     * 
     */
    public final void createInDatastore() {
        // exist the object?
        if (existInDatastore(getId())) {
            LOGGER.warn("The classification " + getId() + " allready exists, nothing done.");
            return;
        }

        // set date values in service part
        if (mcr_service.getDate("createdate") == null) {
            mcr_service.setDate("createdate");
        }
        if (mcr_service.getDate("modifydate") == null) {
            mcr_service.setDate("modifydate");
        }

        // Call event handler
        MCREvent evt = new MCREvent(MCREvent.CLASS_TYPE, MCREvent.CREATE_EVENT);
        evt.put("class", this);
        MCREventManager.instance().handleEvent(evt);

        // store in SQL tables
        /*
         * CM.createClassificationItem(cl); for (int i = 0; i < cat.size(); i++) {
         * CM.createCategoryItem(((MCRCategoryItem) cat.get(i))); }
         */

        return;
    }

    /**
     * The method delete the MCRClassification for the given ID.
     * 
     * @param ID
     *            the classification ID to delete
     * @throws MCRActiveLinkException
     */
    public static final void deleteFromDatastore(String ID) throws MCRActiveLinkException {
        // exist the object?
        if (!existInDatastore(ID)) {
            LOGGER.warn("The classification with ID " + ID + " does not exist.");
            return;
        }

        // save old classification for later reference and roll back
        Document oldClass = MCRClassification.receiveClassificationAsJDOM(ID);

        // SQL delete
        /*
         * CM.deleteAllCategoryItems(ID); CM.deleteClassificationItem(ID);
         */
        // handle events
        MCREvent evt = new MCREvent(MCREvent.CLASS_TYPE, MCREvent.DELETE_EVENT);
        MCRClassification oldcl = new MCRClassification();
        oldcl.setFromJDOM(oldClass);
        evt.put("class", oldcl);
        MCREventManager.instance().handleEvent(evt, MCREventManager.BACKWARD);
    }

    /**
     * The methode return true if the Classification is in the data store, else
     * return false.
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
     * The methode return true if the Classification is in the data store, else
     * return false.
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
     * The methode return true if the category of a classification is in the
     * data store, else return false.
     * 
     * @param classID
     *            the classification ID
     * @param categID
     *            the category ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public final static boolean existInDatastore(String classID, String categID) {
        try {
            if (CM.retrieveCategoryItem(classID, categID) == null) {
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * The method return the classification as XML byte array.
     * 
     * @param classID
     *            the classification ID
     * @return the classification as XML
     */
    public static final byte[] receiveClassificationAsXML(String classID) {
        MCRObjectID mcrid = new MCRObjectID(classID);
        return MCRXMLTableManager.instance().retrieve(mcrid);
    }

    /**
     * The method return the classification as JDOM tree.
     * 
     * @param classID
     *            the classification ID to delete
     * @return the classification as JDOM
     */
    public static final Document receiveClassificationAsJDOM(String classID) {
        return receiveClassificationAsJDOM(classID,false);
    }
    
    /**
     * The method return the classification as JDOM tree.
     * 
     * @param classID
     *            the classification ID to delete
     * @param validator 
     *            the boolean flag for the validator
     * @return the classification as JDOM
     */
    public static final Document receiveClassificationAsJDOM(String classID, boolean flag) {
        byte[] xml = receiveClassificationAsXML(classID);
        Document classification = MCRXMLHelper.parseXML(xml,flag);

        Map map = MCRLinkTableManager.instance().countReferenceCategory(classID);
        // in map we've got the sharp number for every categoryID (without
        // children)
        // now we add to every categoryID the numbers of the children
        for (Iterator it = classification.getDescendants(new ElementFilter("category")); it.hasNext();) {
            Element category = (Element) it.next();
            String mapKey = classID + "##" + category.getAttributeValue("ID");
            int count = (map.get(mapKey) != null) ? ((Integer) map.get(mapKey)).intValue() : 0;
            category.setAttribute("counter", Integer.toString(count));
        }
        return classification;
    }

    /**
     * This methor return a MCRCategoryItem from the SQL store via
     * MCRClassificationManager for a given classification and category ID.
     * 
     * @param classifID
     *            the classification ID
     * @param categID
     *            the category ID
     * @return an instance of a MCRCategoryItem
     */
    public static final MCRCategoryItem receiveCategoryItem(String classifID, String categID) {
        return CM.retrieveCategoryItem(classifID, categID);
    }

    /**
     * The method return the category as XML byte array.
     * 
     * @param classID
     *            the classification ID
     * @param categID
     *            the category ID
     * @return the classification as XML
     */
    public static final org.jdom.Document receiveCategoryAsJDOM(String classID, String categID, boolean withCounter) {
        MCRCategoryItem item = CM.retrieveCategoryItem(classID, categID);
        org.jdom.Element elm = new org.jdom.Element("mycoreclass");
        org.jdom.Document doc = new org.jdom.Document(elm);
        elm.addNamespaceDeclaration(XSI_NAMESPACE);
        elm.addNamespaceDeclaration(XLINK_NAMESPACE);
        elm.setAttribute("ID", classID);
        org.jdom.Element cats = new org.jdom.Element("categories");
        elm.addContent(cats);
        org.jdom.Element cat = MCRClassificationTransformer.getMetaDataElement(item,true);
        cats.addContent(cat);
        return doc;
     }
    
    public static final String[] receiveAllClassificationID() {
        return CM.getAllClassificationID();
    }
    
    /**
     * This method call the repair event handler for this classification.
     */
    public final void repairInDatastore() {
        // Call event handler
        MCREvent evt = new MCREvent(MCREvent.CLASS_TYPE, MCREvent.REPAIR_EVENT);
        evt.put("class", this);
        MCREventManager.instance().handleEvent(evt);
    }

    /**
     * The method update a MCRClassification from the given JDOM tree.
     * 
     */
    public final void updateInDatastore() throws MCRActiveLinkException {
        // check exist
        if (!existInDatastore(getId())) {
            LOGGER.warn("The classification " + getId() + " does not exists, create it.");
            createInDatastore();
            return;
        }

        // set date
        if (mcr_service.getDate("createdate") == null) {
            mcr_service.setDate("createdate");
        }
        mcr_service.setDate("modifydate");
        Document jdom = createXML();

        /*
         * // save old classification for later reference boolean realUpdate =
         * false; Hashtable oldLinks = new Hashtable(); Document oldClass =
         * MCRClassification.receiveClassificationAsJDOM(getId().getId());
         * 
         * Hashtable oldIDs = new Hashtable();
         * getHashedIDs(oldClass.getRootElement(), oldIDs); // all categ-IDs
         * LOGGER.debug("hashing of old ids done"); // can easily be // searched
         * now Hashtable newIDs = new Hashtable();
         * getHashedIDs(jdom.getRootElement(), newIDs); Set removedIDs =
         * getRemovedIDs(oldIDs, newIDs); LOGGER.debug("removedIDs.size()=" +
         * removedIDs.size()); Document raked = rakeDocument(oldClass,
         * removedIDs); if (LOGGER.isDebugEnabled()) { XMLOutputter xout = new
         * XMLOutputter(Format.getPrettyFormat()); StringWriter out = new
         * StringWriter(); try { xout.output(raked, out); } catch (IOException
         * notImportant) { LOGGER.warn("Error while generating debug
         * information.", notImportant); } LOGGER.debug("raked Document: " +
         * out.toString()); }
         * checkActiveLinks(jdom.getRootElement().getAttributeValue(ID_ATTR),
         * raked.getRootElement(), oldIDs); // If the code tuns through the
         * previous method without error we're // fine Set markedIDs =
         * diff(oldClass, jdom, removedIDs); Iterator it = markedIDs.iterator();
         * while (it.hasNext()) { Object o = it.next(); // recursive call down
         * the subtree saveOldLinks(getId().getId(), o.toString(), oldLinks,
         * oldIDs); } it = oldLinks.keySet().iterator(); while (it.hasNext()) {
         * String categId = (String) it.next(); Iterator objIdIt = ((List)
         * oldLinks.get(categId)).iterator(); while (objIdIt.hasNext()) { //
         * delete all links that are changed
         * MCRLinkTableManager.instance().deleteClassificationLink(objIdIt.next().toString(),
         * getId().getId(), categId); } } realUpdate = true;
         */

        // Call event handler for ACL and XML table
        MCREvent evt = null;
        evt = new MCREvent(MCREvent.CLASS_TYPE, MCREvent.UPDATE_EVENT);
        evt.put("class", this);
        MCREventManager.instance().handleEvent(evt);

        /*
         * // for SQL delete and create for (int i = 0; i < cat.size(); i++) {
         * CM.deleteCategoryItem(cl.getID(), ((MCRCategoryItem)
         * cat.get(i)).getID()); } CM.deleteClassificationItem(cl.getID());
         * CM.createClassificationItem(cl); for (int i = 0; i < cat.size(); i++) {
         * CM.createCategoryItem(((MCRCategoryItem) cat.get(i))); }
         *  // new part 2 if (realUpdate) { // keySet of oldLinks is our
         * markedIds; Iterator itn = oldLinks.keySet().iterator(); while
         * (itn.hasNext()) { String categId = (String) itn.next(); Iterator
         * objIdIt = ((List) oldLinks.get(categId)).iterator(); while
         * (objIdIt.hasNext()) { // add all links that are changed
         * MCRLinkTableManager.instance().addClassificationLink(objIdIt.next().toString(),
         * getId().getId(), categId); // hopefully no Exception got thrown
         * between the delete // before and the add here } }
         *  }
         */

        CM.jDomCache.remove(getId());
    }

}
