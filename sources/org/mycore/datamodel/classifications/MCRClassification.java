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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;

import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.common.MCRXMLTableManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;

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
    static final Logger LOGGER = Logger.getLogger(MCRClassification.class);

    // the XML table manager
    static final MCRXMLTableManager TM = MCRXMLTableManager.instance();

    // the link table manager
    static final MCRLinkTableManager LM = MCRLinkTableManager.instance();

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
     * @param classID
     *            the classification ID
     */
    public final void setFromStore(MCRObjectID classid) {
        setFromJDOM(retrieveClassificationAsJDOM(classid.toString()));
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
        setId((new MCRObjectID(xmlblob.getAttributeValue("ID"))).getId());
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
        if (MCRXMLTableManager.instance().exist(new MCRObjectID(getId()))) {
            LOGGER.warn("The classification " + getId() + " allready exists, nothing done.");
            return;
        }

        // set date values in service part
        if (mcr_service == null) {
            mcr_service = new MCRObjectService();
        }
        if (mcr_service.getDate("createdate") == null) {
            mcr_service.setDate("createdate");
        }
        mcr_service.setDate("modifydate");

        // Call event handler
        MCREvent evt = new MCREvent(MCREvent.CLASS_TYPE, MCREvent.CREATE_EVENT);
        evt.put("class", this);
        MCREventManager.instance().handleEvent(evt);

        return;
    }

    /**
     * The method delete the MCRClassification for the given ID.
     * 
     * @param ID
     *            the classification ID to delete
     * @throws MCRActiveLinkException
     */
    public static final void deleteFromDatastore(MCRObjectID ID) throws MCRActiveLinkException {
        // exist the object?
        if (!MCRXMLTableManager.instance().exist(ID)) {
            LOGGER.warn("The classification with ID " + ID + " does not exist.");
            return;
        }

        // save old classification for later reference and roll back
        Document oldClass = TM.retrieveAsJDOM(ID);
        Hashtable<String, Element> oldIDs = new Hashtable<String, Element>();
        // all categ-IDs can easily be searched now
        getHashedIDs(oldClass.getRootElement(), oldIDs);
        Set removedIDs = oldIDs.keySet();
        int count = 0;
        boolean candelete = true;
        Iterator it = removedIDs.iterator();
        while (it.hasNext()) {
            String cid = (String) it.next();
            int i = LM.countReferenceCategory(ID.getId(), cid);
            count += i;
            LOGGER.debug("Check link for category with ID " + cid);
            if (i != 0) {
                LOGGER.error("Category " + cid + " in classification " + ID.getId() + " has " + (new Integer(i).toString() + " links. Can't remove!"));
                candelete = false;
            }
        }
        if (!candelete) {
            MCRActiveLinkException e = new MCRActiveLinkException(new StringBuffer("Error while deleting classification ").append(ID.getId()).append(". ").append((new Integer(count).toString())).append(" unresolved reverences!").toString());
            throw e;
        }

        // handle events
        MCREvent evt = new MCREvent(MCREvent.CLASS_TYPE, MCREvent.DELETE_EVENT);
        MCRClassification oldcl = new MCRClassification();
        oldcl.setFromJDOM(oldClass);
        evt.put("class", oldcl);
        MCREventManager.instance().handleEvent(evt, MCREventManager.BACKWARD);
    }

    /**
     * The method return the classification as XML byte array.
     * 
     * @param classID
     *            the classification ID
     * @return the classification as XML
     */
    public static final byte[] retrievClassificationAsXML(String classID) {
        MCRObjectID mcrid = new MCRObjectID(classID);
        return MCRXMLTableManager.instance().retrieveAsXML(mcrid);
    }

    /**
     * The method return the classification as JDOM tree.
     * 
     * @param classID
     *            the classification ID to delete
     * @return the classification as JDOM
     */
    public static final Document retrieveClassificationAsJDOM(String classID) {
        return TM.readDocument(new MCRObjectID(classID));
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
    public static final MCRCategoryItem retrieveCategoryItem(String classifID, String categID) {
        return CM.retrieveCategoryItem(classifID, categID);
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
        if (!MCRXMLTableManager.instance().exist(new MCRObjectID(getId()))) {
            LOGGER.warn("The classification " + getId() + " does not exists, create it.");
            createInDatastore();
            return;
        }

        // set date
        if (mcr_service == null) {
            mcr_service = new MCRObjectService();
        }
        if (mcr_service.getDate("createdate") == null) {
            mcr_service.setDate("createdate");
        }

        Document thisClass = createXML();
        Hashtable<String, Element> thisIDs = new Hashtable<String, Element>();
        // save old classification for later reference and roll back
        Document oldClassDoc = TM.retrieveAsJDOM(new MCRObjectID(getId()));
        Hashtable<String, Element> oldIDs = new Hashtable<String, Element>();
        // all categ-IDs can easily be searched now
        getHashedIDs(oldClassDoc.getRootElement(), oldIDs);
        getHashedIDs(thisClass.getRootElement(), thisIDs);
        Set oldIDsSet = oldIDs.keySet();
        Set thisIDsSet = thisIDs.keySet();
        // set create date from old
        Element service = oldClassDoc.getRootElement().getChild("service");
        if (service != null) {
            mcr_service.setFromDOM(service);
        }        
        mcr_service.setDate("modifydate");
        // compare entries
        ArrayList<String> oldCateg = new ArrayList<String>();
        Iterator it = oldIDsSet.iterator();
        while (it.hasNext()) {
            String cid = (String) it.next();
            int i = LM.countReferenceCategory(getId(), cid);
            if (i == 0) {
                it.remove();
                continue;
            }
            if (thisIDsSet.contains(cid)) {
                it.remove();
                continue;
            }
            oldCateg.add(cid);
            LOGGER.warn("The category " + cid + " of classification " + getId() + " has " + (new Integer(i)).toString() + " references and was not found in new classification.");
        }
        //oldCateg contains all Categories that are removed and are target of object links
        if (oldCateg.size() != 0) {
            MCRClassification oldClass = new MCRClassification();
            oldClass.setFromJDOM(oldClassDoc);
            for (int i = 0; i < oldCateg.size(); i++) {
                copyCategory(oldClass, (String) oldCateg.get(i));
            }
        }

        // Call event handler for ACL and XML table
        MCREvent evt = null;
        evt = new MCREvent(MCREvent.CLASS_TYPE, MCREvent.UPDATE_EVENT);
        evt.put("class", this);
        MCREventManager.instance().handleEvent(evt);

        CM.jDomCache.remove(getId());
    }

    /**
     * Finds all elements with attribute ID and gives access to them via this
     * attribute. It a workaround to access Element objects like the
     * getElementById() of DOM.
     * 
     * @param root
     *            the root element (starting point)
     * @param sink
     *            a Hashtable with key(ID as String) and value(Element with that
     *            ID)
     */
    private static final void getHashedIDs(Element root, Hashtable<String, Element> sink) {
        Iterator children = root.getChildren().iterator();
        String id;
        Element cur;
        while (children.hasNext()) {
            // collect all IDs of children and save them together with the
            // element in the Hashtable
            cur = (Element) children.next();
            id = cur.getAttributeValue("ID");
            if (id != null) {
                sink.put(id, cur);
                LOGGER.debug("added hash for " + id + ": " + cur.getAttributeValue("counter"));
            }
            getHashedIDs(cur, sink); // recursive call for all children
        }
    }

    public static Document retrieveClassificationAsJDOM(String classID, boolean withCounter) {
        Document classification=retrieveClassificationAsJDOM(classID);
        if (withCounter) {
            Map map = MCRLinkTableManager.instance().countReferenceCategory(classID);
            // in map we've got the sharp number for every categoryID (without
            // children)
            // now we add to every categoryID the numbers of the children
            for (Iterator it = classification.getDescendants(new ElementFilter("category")); it.hasNext();) {
                Element category = (Element) it.next();
                String mapKey = classID + "##" + category.getAttributeValue("ID");
                int count = (map.get(mapKey) != null) ? ((Number) map.get(mapKey)).intValue() : 0;
                category.setAttribute("counter", Integer.toString(count));
            }
        }
        return classification;
    }

}
