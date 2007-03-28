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
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
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

    // the XML table manager
    static MCRXMLTableManager TM = MCRXMLTableManager.instance();

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
     * @param classID
     *            the classification ID
     */
    public final void setFromStore(MCRObjectID classid) {
        byte[] xml = MCRXMLTableManager.instance().retrieveAsXML(classid);
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
        CM.createClassificationItem(this);
        CM.createCategoryItems(getCategories());

        return;
    }

    /**
     * The method delete the MCRClassification for the given ID.
     * 
     * @param ID
     *            the classification ID to delete
     * @throws MCRActiveLinkException
     */
    public final void deleteFromDatastore(MCRObjectID ID) throws MCRActiveLinkException {
        // exist the object?
        if (!MCRXMLTableManager.instance().exist(ID)) {
            LOGGER.warn("The classification with ID " + ID + " does not exist.");
            return;
        }

        // save old classification for later reference and roll back
        Document oldClass = TM.retrieveAsJDOM(ID);
        Hashtable oldIDs = new Hashtable();
        // all categ-IDs can easily be searched now
        getHashedIDs(oldClass.getRootElement(), oldIDs);
        Set removedIDs = oldIDs.keySet();
        LOGGER.debug("removedIDs.size()=" + removedIDs.size());
        // TODO: The rake process is not realy needed, because every
        // category is removed. Will have to check this later
        Document raked = rakeDocument(oldClass, removedIDs);
        if (LOGGER.isDebugEnabled()) {
            XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
            StringWriter out = new StringWriter();
            try {
                xout.output(raked, out);
            } catch (IOException notImportant) {
                LOGGER.warn("Error while generating debug information.", notImportant);
            }
            LOGGER.debug("raked Document: " + out.toString());
        }
        /*
         * we have a raked tree of oldClass so that it keeps the hierarchie of
         * oldClass but only contains removed categIDs, so that we can easily
         * verify later for associated objects.
         */
        checkActiveLinks(ID.getId(), raked.getRootElement(), oldIDs);

        // SQL delete
        Iterator it = removedIDs.iterator();
        while (it.hasNext()) {
            String cid = (String) it.next();
            LOGGER.debug("Delete category with ID " + cid);
            CM.deleteCategoryItem(ID, cid);
        }
        CM.deleteClassificationItem(ID);
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
        return MCRXMLTableManager.instance().retrieveAsXML(mcrid);
    }

    /**
     * The method return the classification as JDOM tree.
     * 
     * @param classID
     *            the classification ID to delete
     * @return the classification as JDOM
     */
    public static final Document receiveClassificationAsJDOM(String classID) {
        return receiveClassificationAsJDOM(classID, false);
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
        Document classification = MCRXMLHelper.parseXML(xml, flag);

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
        org.jdom.Element cat = MCRClassificationTransformer.getMetaDataElement(item, true);
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
         * CM.createCategoryItem(((MCRCategoryItem) cat.get(i))); } // new part
         * 2 if (realUpdate) { // keySet of oldLinks is our markedIds; Iterator
         * itn = oldLinks.keySet().iterator(); while (itn.hasNext()) { String
         * categId = (String) itn.next(); Iterator objIdIt = ((List)
         * oldLinks.get(categId)).iterator(); while (objIdIt.hasNext()) { // add
         * all links that are changed
         * MCRLinkTableManager.instance().addClassificationLink(objIdIt.next().toString(),
         * getId().getId(), categId); // hopefully no Exception got thrown
         * between the delete // before and the add here } } }
         */

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

    /**
     * returns only these IDs in a new set that are part of oldIDs but not in
     * newIDs.
     * 
     * @param oldIDs
     *            Hashtable as described in getHashedIDs()
     * @param newIDs
     *            Hashtable as described in getHashedIDs()
     * @see #getHashedIDs(Element, Hashtable)
     * @return a Set with all IDs from oldIds that are not in newIds
     */
    private static final Set getRemovedIDs(Hashtable oldIDs, Hashtable newIDs) {
        int size = oldIDs.size() - newIDs.size();
        size = (size < 0) ? 10 : size;
        HashSet<String> returns = new HashSet<String>(size);
        Iterator it = oldIDs.keySet().iterator();
        String id;
        while (it.hasNext()) {
            id = (String) it.next();
            if (!newIDs.containsKey(id)) {
                returns.add(id); // ID is removed in new Classification, mark
                // it
            }
        }
        return returns;
    }

    /**
     * Relinks a all children of oldParent to newParent. This will also unlink
     * oldParent from its parent, which removes it actually from the document
     * tree.
     * 
     * @param oldParent
     *            Element to remove
     * @param newParent
     *            Element to link all children of oldParent
     */
    private static final void moveChildren(Element oldParent, Element newParent) {
        LOGGER.debug("gently removing " + oldParent.getName() + " with ID " + oldParent.getAttributeValue("ID") + " parent is " + newParent.getName());
        oldParent.detach();
        Iterator children = oldParent.getChildren("category").iterator();
        Element child;
        while (children.hasNext()) {
            child = (Element) children.next();
            children.remove();
            newParent.addContent(child.detach());
        }
    }

    /**
     * Removes all categories from the document that IDs are not in keepIDs.
     * This method preserves any affinity of the categories that a kept. For
     * example if category ID X is an ancestor of category Y and both are in
     * keepIDs, than after the rake X is still ancestor of Y. But both maybe
     * close on an ancestor axis after the rake operation.
     * 
     * @param oldDoc
     *            document in MyCoRe XML view of a classification
     * @param keepIDs
     *            IDs that should be kept
     */
    private static final Document rakeDocument(Document oldDoc, Set keepIDs) {
        Document raked = (Document) oldDoc.clone();
        Element categories = raked.getRootElement().getChild("categories");
        Iterator children = categories.getChildren().iterator();
        Element curEl;
        Vector<Element> childList = new Vector<Element>();
        while (children.hasNext()) {
            // as the list changes at runtime we must process it from the end
            curEl = (Element) children.next();
            if (rakeElement(curEl, keepIDs)) {
                children.remove();
                /*
                 * To avoid java.util.ConcurrentModificationException we
                 * memorize elements we want to remove from the jdom tree.
                 */
                childList.add(curEl);
            }
        }
        children = childList.iterator();
        while (children.hasNext()) {
            moveChildren((Element) children.next(), categories);
        }
        moveChildren(categories, raked.getRootElement());
        raked.getRootElement().removeChildren("label");
        return raked; // after the root element all children now contains
        // "ID"-Attributes
    }

    /**
     * helps rakeDocument to function. It's just a recursive helper function
     * 
     * @param root
     *            the current Element
     * @param keepIDs
     *            IDs that should be kept
     * @return true if the element should be removed
     */
    private static final boolean rakeElement(Element root, Set keepIDs) {
        Iterator children = root.getChildren().iterator();
        Element curEl;
        Vector<Element> childList = new Vector<Element>();
        while (children.hasNext()) {
            // as the list changes at runtime we must process it from the end
            curEl = (Element) children.next();
            if (rakeElement(curEl, keepIDs)) {
                // rekursive to the leaves
                children.remove();
                childList.add(curEl);
            }
        }
        children = childList.iterator();
        while (children.hasNext()) {
            moveChildren((Element) children.next(), root);
        }
        String id = root.getAttributeValue("ID");
        if (id == null || !keepIDs.contains(id)) {
            // LOGGER.debug("gently removing "+root.getName()+" with ID "+id);
            return true;
        }
        return false;
    }

    /**
     * checks if there are links to any category under root.
     * 
     * @param classID
     *            the classification ID of the categories
     * @param root
     *            the current Element/root element
     * @param oldClass
     *            HashTable as in getHashedIDs()
     * @see #getHashedIDs(Element, Hashtable)
     * @throws MCRActiveLinkException
     *             if links to any category under root where detected
     */
    private final static void checkActiveLinks(String classID, Element root, Map oldClass) throws MCRActiveLinkException {
        Iterator children = root.getChildren().iterator();
        while (children.hasNext()) {
            checkActiveLinks(classID, (Element) children.next(), oldClass);
        }
        if (root.getParentElement() == null) {
            return; // do not check <mycoreclass>
        }
        String curID = root.getAttributeValue("ID");
        LOGGER.debug("Checking " + classID + "##" + curID);
        if (curID != null) {
            Element oldCateg = (Element) oldClass.get(curID); // fetched
            // category-Element
            // to current ID
            List subCategs = oldCateg.getChildren("category");
            int subTotals = 0;
            boolean cAvailable = true;
            String attr;
            for (int j = 0; j < subCategs.size(); j++) {
                attr = ((Element) subCategs.get(j)).getAttributeValue("counter");
                if (attr == null) {
                    cAvailable = false;
                    break;
                }
                subTotals += Integer.parseInt(attr);
            }
            attr = oldCateg.getAttributeValue("counter");
            /*
             * We calculated the total number of links to all children at this
             * point. If this number is equal to the counter of the current
             * category element, than there no need for further checking. This
             * category only contains links that its children contains and no
             * other. We'll check this issue in the following if-block.
             */
            if (!cAvailable || attr == null || Integer.parseInt(attr) != subTotals) {
                LOGGER.debug("cAvailable: " + cAvailable);
                LOGGER.debug("totals: " + attr);
                LOGGER.debug("Subtotals: " + subTotals);
                /*
                 * OK, there is the big possibility that the current and yet to
                 * be removed category contains links from other objects. We
                 * check this issue now further.
                 */

                // This call only returns IDs, that are in the current category
                // but not in its children
                List activeLinks;
                try {
                    activeLinks = MCRLinkTableManager.instance().getFirstLinksToCategory(classID, curID);
                } catch (Exception e) { 
                    LOGGER.warn("Cant retrieve category "+curID+" of classification "+classID);
                    return;
                }
                Iterator it = activeLinks.iterator();
                MCRActiveLinkException e = new MCRActiveLinkException(new StringBuffer("Error while deleting category ").append(curID).append(" from Classification ").append(classID).append('.').toString());
                String curSource;
                while (it.hasNext()) {
                    curSource = (String) it.next();
                    // we add this element as this is not a element from the
                    // descendant list
                    LOGGER.debug("adding failed link " + curSource + "-->" + classID + "##" + curID);
                    //e.addLink(curSource, classID + "##" + curID);
                }
                // after all links are added to the exception
                //throw e;
            }
        }
    }

}
