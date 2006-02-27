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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
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
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import org.mycore.common.MCRDefaults;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.metadata.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRXMLTableManager;

/**
 * This class implements all methods for a classification and extended the
 * MCRClassificationObject class.
 * 
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRClassification {
	// logger
	static Logger LOGGER = Logger.getLogger(MCRClassification.class);

	private MCRClassificationItem cl;

	private ArrayList cat;

	private static final String OBJ_COUNT_ATTR = "counter";

	private static final String ID_ATTR = "ID";

	/**
	 * The constructor
	 */
	public MCRClassification() {
	}

	/**
	 * The method fill the instance of this class with a given JODM tree.
	 * 
	 * @param jdom
	 *            the classification as jdom tree
	 */
	private final void setFromJDOM(org.jdom.Document jdom) {
		cl = new MCRClassificationItem(new MCRObjectID(jdom.getRootElement().getAttributeValue(ID_ATTR)).getId());

		List tagList = jdom.getRootElement().getChildren("label");
		Element tag;

		for (int i = 0; i < tagList.size(); i++) {
			tag = (Element) tagList.get(i);
			cl.addData(tag.getAttributeValue("lang", Namespace.XML_NAMESPACE), tag.getAttributeValue("text"), tag.getAttributeValue("description"));
		}

		LOGGER.debug("processing Classification:" + cl.toString());
		cat = new ArrayList();
		tagList = jdom.getRootElement().getChild("categories").getChildren("category");

		for (int i = 0; i < tagList.size(); i++) {
			breakDownCategories((Element) tagList.get(i), cl);
		}
	}

	private void breakDownCategories(Element category, MCRClassificationObject parent) {
		// process labels
		MCRCategoryItem ci = new MCRCategoryItem(category.getAttributeValue(ID_ATTR), parent);
		List tagList = category.getChildren("label");
		Element element;

		for (int i = 0; i < tagList.size(); i++) {
			element = (Element) tagList.get(i);
			ci.addData(element.getAttributeValue("lang", Namespace.XML_NAMESPACE), element.getAttributeValue("text"), element.getAttributeValue("description"));
		}

		// process url, if given
		element = category.getChild("url");

		if (element != null) {
			ci.setURL(element.getAttributeValue("href", Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL)));
		}

		cat.add(ci); // add to list of categories
		tagList = category.getChildren("category");

		for (int i = 0; i < tagList.size(); i++) {
			breakDownCategories((Element) tagList.get(i), ci); // process

			// children
		}
	}

	/**
	 * The method create a MCRClassification from the given JDOM tree.
	 * 
	 * @param jdom
	 *            the classification as jdom tree
	 */
	public final String createFromJDOM(org.jdom.Document jdom) {
		setFromJDOM(jdom);

		XMLOutputter xout = new XMLOutputter(Format.getRawFormat());
		ByteArrayOutputStream bout = new ByteArrayOutputStream();

		try {
			xout.output(jdom, bout);
		} catch (IOException e) {
			throw new MCRException("Ooops", e);
		}

		String ID = jdom.getRootElement().getAttributeValue(ID_ATTR);
		MCRObjectID mcr_id = new MCRObjectID(ID);
		MCRXMLTableManager.instance().create(mcr_id, bout.toByteArray());
		cl.create();

		for (int i = 0; i < cat.size(); i++) {
			((MCRCategoryItem) cat.get(i)).create();
		}

		return cl.getClassificationID();
	}

	/**
	 * The method create a MCRClassification from the given XML array.
	 * 
	 * @param xml
	 *            the classification as byte array XML tree
	 * @exception MCRException
	 *                if the parser can't build a JDOM tree
	 */
	public final String createFromXML(byte[] xml) throws MCRException {
		try {
			org.jdom.input.SAXBuilder bulli = new org.jdom.input.SAXBuilder(false);
			org.jdom.Document jdom = bulli.build(new ByteArrayInputStream(xml));

			return createFromJDOM(jdom);
		} catch (Exception e) {
			throw new MCRException(e.getMessage());
		}
	}

	/**
	 * The method create a MCRClassification from the given URI.
	 * 
	 * @param uri
	 *            the classification URI
	 * @exception MCRException
	 *                if the parser can't build a JDOM tree
	 */
	public final String createFromURI(String uri) throws MCRException {
		try {
			org.jdom.Document jdom = MCRXMLHelper.parseURI(uri);

			return createFromJDOM(jdom);
		} catch (Exception e) {
			throw new MCRException(e.getMessage(), e);
		}
	}

	/**
	 * The method delete the MCRClassification for the given ID.
	 * 
	 * @param ID
	 *            the classification ID to delete
	 * @throws MCRActiveLinkException
	 */
	public static final void delete(String ID) throws MCRActiveLinkException {
		if (MCRXMLTableManager.instance().exist(new MCRObjectID(ID))) {
			// save old classification for later reference
			Document oldClass = MCRClassification.receiveClassificationAsJDOM(ID);

			Hashtable oldIDs = new Hashtable();
			getHashedIDs(oldClass.getRootElement(), oldIDs); // all categ-IDs
			// can easily be
			// searched now
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
			 * we have a raked tree of oldClass so that it keeps the hierarchie
			 * of oldClass but only contains removed categIDs, so that we can
			 * easily verify later for associated objects.
			 */
			checkActiveLinks(ID, raked.getRootElement(), oldIDs);
			// If the code tuns through the previous method without error we're
			// fine
		}
		// TODO: The following call gets redundant data (twice ID)
		(new MCRClassificationItem(ID)).delete(ID);
		MCRXMLTableManager.instance().delete(new MCRObjectID(ID));
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
	private static final void getHashedIDs(Element root, Hashtable sink) {
		Iterator children = root.getChildren().iterator();
		String id;
		Element cur;
		while (children.hasNext()) {
			// collect all IDs of children and save them together with the
			// element in the Hashtable
			cur = (Element) children.next();
			id = cur.getAttributeValue(ID_ATTR);
			if (id != null) {
				sink.put(id, cur);
				LOGGER.debug("added hash for " + id + ": " + cur.getAttributeValue(OBJ_COUNT_ATTR));
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
		HashSet returns = new HashSet(size);
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
		LOGGER.debug("gently removing " + oldParent.getName() + " with ID " + oldParent.getAttributeValue(ID_ATTR) + " parent is " + newParent.getName());
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
		Vector childList = new Vector();
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
		Vector childList = new Vector();
		while (children.hasNext()) {
			// as the list changes at runtime we must process it from the end
			curEl = (Element) children.next();
			if (rakeElement(curEl, keepIDs)) {
				children.remove();// rekursive to the leaves;
				childList.add(curEl);
			}
		}
		children = childList.iterator();
		while (children.hasNext()) {
			moveChildren((Element) children.next(), root);
		}
		String id = root.getAttributeValue(ID_ATTR);
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
		String curID = root.getAttributeValue(ID_ATTR);
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
				attr = ((Element) subCategs.get(j)).getAttributeValue(OBJ_COUNT_ATTR);
				if (attr == null) {
					cAvailable = false;
					break;
				}
				subTotals += Integer.parseInt(attr);
			}
			attr = oldCateg.getAttributeValue(OBJ_COUNT_ATTR);
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
				List activeLinks = MCRLinkTableManager.instance().getFirstLinksToCategory(classID, curID);

				Iterator it = activeLinks.iterator();
				MCRActiveLinkException e = new MCRActiveLinkException(new StringBuffer("Error while deleting category ").append(curID).append(
						" from Classification ").append(classID).append('.').toString());
				String curSource;
				while (it.hasNext()) {
					curSource = (String) it.next();
					// we add this element as this is not a element from the
					// descendant list
					LOGGER.debug("adding failed link " + curSource + "-->" + classID + "##" + curID);
					e.addLink(curSource, classID + "##" + curID);
				}
				// after all links are added to the exception
				throw e;
			}
		}
	}

	/**
	 * Returns a set of IDs that a root of a subtree so, that the root of the
	 * subtree has in oldClass a different parent than in newClass.
	 * 
	 * @param oldClass
	 *            old Classification
	 * @param newClass
	 *            new Classification
	 * @param removedIDs
	 *            Set to gather if IDs are available in newClass
	 */
	private final Set diff(Document oldClass, Document newClass, Set removedIDs) {
		Set markedIDs = new HashSet();
		diff(oldClass.getRootElement().getChild("categories"), newClass.getRootElement().getChild("categories"), markedIDs, removedIDs);
		return markedIDs;
	}

	/**
	 * helps diff for recursive calls;
	 * 
	 * @param oldCateg
	 *            old Category element with the same ID as
	 * @param newCateg
	 *            new Category element
	 * @param markedIDs
	 *            Set to save the IDs as noted in main diff()
	 * @param removedIDs
	 *            Set to gather if IDs are available in newClass
	 * @see #diff(Document, Document, Set)
	 */
	private final void diff(Element oldCateg, Element newCateg, Set markedIDs, Set removedIDs) {

		String attr = oldCateg.getAttributeValue(OBJ_COUNT_ATTR);
		int counter = 1;
		if (attr != null) {
			counter = Integer.parseInt(attr);
		}
		attr = oldCateg.getAttributeValue(ID_ATTR);
		if (counter == 0) {
			// no work in this subtree, as there are no links to this category
			return;
		}
		// objects are linked to this category
		/*
		 * now we synchronize the walk down the tree: For every ID we look in
		 * the childlist of new categs for that id. If we found a match we walk
		 * down that path
		 */
		List oldChildren = oldCateg.getChildren("category");
		List newChildren = newCateg.getChildren("category");
		Map newIDs = hashIDs(newChildren);
		Map oldIDs = hashIDs(oldChildren);
		Set diff = new HashSet();
		Iterator it = newIDs.keySet().iterator();
		while (it.hasNext()) {
			Object o = it.next();
			if (!oldIDs.containsKey(o)) {
				diff.add(o); // is not available in oldChildren
				it.remove();
			}
		}
		it = oldIDs.keySet().iterator();
		while (it.hasNext()) {
			Object o = it.next();
			if (removedIDs.contains(o)) {
				// we don't bother since this category has been removed and is
				// checked allready
				it.remove();
			} else if (!newIDs.containsKey(o)) {
				diff.add(o); // is not available in newChildren
				it.remove();
			}
		}
		// we can mark all in diff
		markedIDs.addAll(diff);
		// newIDs and oldIDs contain now only the same set of IDs
		it = oldIDs.keySet().iterator();
		while (it.hasNext()) {
			Object o = it.next();
			diff((Element) oldIDs.get(o), (Element) newIDs.get(o), markedIDs, removedIDs);
		}
	}

	/**
	 * Will hash all Elements in a list to a Hashtable.
	 * 
	 * This is strictly non recursive. For a recursive call see getHashedIDs()
	 * 
	 * @param children
	 *            a list with elements to hash
	 * @return a Hashtable with hashed elements of children
	 * @see #getHashedIDs(Element, Hashtable)
	 */
	private final Map hashIDs(List children) {
		Hashtable returns = new Hashtable(children.size());
		Iterator it = children.iterator();
		while (it.hasNext()) {
			Element el = (Element) it.next();
			String attr = el.getAttributeValue(ID_ATTR);
			if (attr != null) {
				returns.put(attr, el);
			}
		}
		return returns;
	}

	/**
	 * Saves all document links under category for later use in sink.
	 * 
	 * @param classId
	 *            the classification ID of the categories
	 * @param categid
	 *            the root of the category sub tree
	 * @param sink
	 *            this is where the information is saved to
	 * @param idMap
	 *            a Hashtable of the old Classification as described in
	 *            getHashedIds()
	 * @see #getHashedIDs(Element, Hashtable)
	 */
	private final void saveOldLinks(String classId, String categid, Map sink, Map idMap) {
		LOGGER.debug("saving old links for " + classId + "##" + categid);
		Element el = (Element) idMap.get(categid);
		String attr = el.getAttributeValue(OBJ_COUNT_ATTR);
		if (attr != null && attr.equals("0")) {
			return;
		}
		List list = MCRLinkTableManager.instance().getFirstLinksToCategory(classId, categid);
		if (list.size() > 0) {
			sink.put(categid, list);
		}
		MCRCategoryItem[] children = MCRCategoryItem.getCategoryItem(classId, categid).getChildren();
		for (int i = 0; i < children.length; i++) {
			saveOldLinks(classId, children[i].getClassificationID(), sink, idMap);
		}
	}

	/**
	 * The method update a MCRClassification from the given JDOM tree.
	 * 
	 * @param jdom
	 *            the classification as jdom tree
	 */
	public final String updateFromJDOM(org.jdom.Document jdom) throws MCRActiveLinkException {
		// old part 1 starts
		org.jdom.Element root = jdom.getRootElement();
		String ID = root.getAttributeValue(ID_ATTR);
		MCRObjectID mcr_id = new MCRObjectID(ID);
		// old part 1 ends

		boolean realUpdate = false;
		Hashtable oldLinks = new Hashtable();

		if (MCRXMLTableManager.instance().exist(mcr_id)) {
			// save old classification for later reference
			Document oldClass = MCRClassification.receiveClassificationAsJDOM(jdom.getRootElement().getAttributeValue(ID_ATTR));

			Hashtable oldIDs = new Hashtable();
			getHashedIDs(oldClass.getRootElement(), oldIDs); // all categ-IDs
			LOGGER.debug("hashing of old ids done");
			// can easily be
			// searched now
			Hashtable newIDs = new Hashtable();
			getHashedIDs(jdom.getRootElement(), newIDs);
			Set removedIDs = getRemovedIDs(oldIDs, newIDs);
			LOGGER.debug("removedIDs.size()=" + removedIDs.size());
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
			 * we have a raked tree of oldClass so that it keeps the hierarchie
			 * of oldClass but only contains removed categIDs, so that we can
			 * easily verify later for associated objects.
			 */
			checkActiveLinks(jdom.getRootElement().getAttributeValue(ID_ATTR), raked.getRootElement(), oldIDs);
			// If the code tuns through the previous method without error we're
			// fine
			/*
			 * now updates may follow if the structure of the categories has
			 * changed with the update a transaction should start here to be
			 * save: now the write access begins. We compare from the root of
			 * the new and old categories where a root of a subtree differs, so
			 * that subtreeRoot of oldClass has a different parent than its
			 * related node of newClass. We'll have to keep track of all those
			 * root ids that contain links from other objects.
			 */
			Set markedIDs = diff(oldClass, jdom, removedIDs);
			Iterator it = markedIDs.iterator();
			while (it.hasNext()) {
				Object o = it.next();
				// recursive call down the subtree
				saveOldLinks(mcr_id.toString(), o.toString(), oldLinks, oldIDs);
			}
			it = oldLinks.keySet().iterator();
			while (it.hasNext()) {
				String categId = (String) it.next();
				Iterator objIdIt = ((List) oldLinks.get(categId)).iterator();
				while (objIdIt.hasNext()) {
					// delete all links that are changed
					MCRLinkTableManager.instance().deleteClassificationLink(objIdIt.next().toString(), mcr_id.toString(), categId);
				}
			}
			realUpdate = true;
		}

		// old part 2
		cl = new MCRClassificationItem(mcr_id.getId());
		cl.delete(cl.getClassificationID());
		setFromJDOM(jdom);

		XMLOutputter xout = new XMLOutputter(Format.getRawFormat());
		ByteArrayOutputStream bout = new ByteArrayOutputStream();

		try {
			xout.output(jdom, bout);
		} catch (IOException e) {
			throw new MCRException("Ooops", e);
		}

		MCRXMLTableManager mgr = MCRXMLTableManager.instance();

		if (mgr.exist(mcr_id)) {
			mgr.update(mcr_id, bout.toByteArray());
		} else {
			mgr.create(mcr_id, bout.toByteArray());
		}

		cl.create();

		for (int i = 0; i < cat.size(); i++) {
			((MCRCategoryItem) cat.get(i)).create();
		}

		// new part 2
		if (realUpdate) {
			// keySet of oldLinks is our markedIds;
			Iterator it = oldLinks.keySet().iterator();
			while (it.hasNext()) {
				String categId = (String) it.next();
				Iterator objIdIt = ((List) oldLinks.get(categId)).iterator();
				while (objIdIt.hasNext()) {
					// add all links that are changed
					MCRLinkTableManager.instance().addClassificationLink(objIdIt.next().toString(), mcr_id.toString(), categId);
					// hopefully no Exception got thrown between the delete
					// before and the add here
				}
			}

		}

		return cl.getClassificationID();
	}

	/**
	 * The method update a MCRClassification from the given XML array.
	 * 
	 * @param xml
	 *            the classification as byte array XML tree
	 * @exception MCRException
	 *                if the parser can't build a JDOM tree
	 */
	public final String updateFromXML(byte[] xml) throws MCRActiveLinkException, MCRException {
		try {
			org.jdom.input.SAXBuilder bulli = new org.jdom.input.SAXBuilder(false);
			org.jdom.Document jdom = bulli.build(new ByteArrayInputStream(xml));

			return updateFromJDOM(jdom);
		} catch (Exception e) {
			if (e instanceof MCRActiveLinkException) {
				throw (MCRActiveLinkException) e;
			}
			throw new MCRException(e.getMessage());
		}
	}

	/**
	 * The method update a MCRClassification from the given URI.
	 * 
	 * @param uri
	 *            the classification URI
	 * @exception MCRException
	 *                if the parser can't build a JDOM tree
	 * @throws MCRActiveLinkException
	 */
	public final String updateFromURI(String uri) throws MCRException, MCRActiveLinkException {
		try {
			org.jdom.Document jdom = MCRXMLHelper.parseURI(uri);

			return updateFromJDOM(jdom);
		} catch (Exception e) {
			if (e instanceof MCRActiveLinkException) {
				throw (MCRActiveLinkException) e;
			}
			throw new MCRException(e.getMessage(), e);
		}
	}

	private static Document getClassification(String ID) throws MCRException {
		MCRObjectID classID = new MCRObjectID(ID);
		LOGGER.debug("Loading Classification " + ID + " of MCRType: " + classID.getTypeId());

		MCRXMLTableManager tm = MCRXMLTableManager.instance();

		return tm.readDocument(classID);
	}

	/**
	 * The method return the classification as JDOM tree.
	 * 
	 * @param classID
	 *            the classification ID to delete
	 * @return the classification as JDOM
	 */
	public static final Document receiveClassificationAsJDOM(String classID) {
		Document classification = null;

		try {
			classification = getClassification(classID);
		} catch (MCRException e) {
			LOGGER.error("Oops", e);
		}

		Map map = MCRLinkTableManager.instance().countReferenceCategory(classID);
		// in map we've got the sharp number for every categoryID (without
		// children)
		// now we add to every categoryID the numbers of the children
		for (Iterator it = classification.getDescendants(new ElementFilter("category")); it.hasNext();) {
			Element category = (Element) it.next();
			String mapKey = classID + "##" + category.getAttributeValue(ID_ATTR);
			int count = (map.get(mapKey) != null) ? ((Integer) map.get(mapKey)).intValue() : 0;
			category.setAttribute(OBJ_COUNT_ATTR, Integer.toString(count));
		}
		return classification;
	}

	/**
	 * The method return the classification as XML byte array.
	 * 
	 * @param classID
	 *            the classification ID
	 * @return the classification as XML
	 */
	public final byte[] receiveClassificationAsXML(String classID) {
		org.jdom.Document doc = receiveClassificationAsJDOM(classID);
		byte[] xml = MCRUtils.getByteArray(doc);

		return xml;
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
	public final org.jdom.Document receiveCategoryAsJDOM(String classID, String categID) {
		MCRLinkTableManager mcr_linktable = MCRLinkTableManager.instance();
		org.jdom.Element elm = new org.jdom.Element("mycoreclass");
		org.jdom.Document doc = new org.jdom.Document(elm);
		elm.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xsi", MCRDefaults.XSI_URL));
		elm.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
		elm.setAttribute(ID_ATTR, classID);

		org.jdom.Element cats = new org.jdom.Element("categories");

		// get the classification
		try {
			cl = MCRClassificationItem.getClassificationItem(classID);
		} catch (Exception e) {
			cl = null;
		}

		if (cl != null) {
			for (int i = 0; i < cl.getSize(); i++) {
				elm.addContent(cl.getJDOMElement(i));
			}

			// get the category
			MCRCategoryItem ci = cl.getCategoryItem(categID);

			if (ci != null) {
				org.jdom.Element cat = new org.jdom.Element("category");
				cat.setAttribute(ID_ATTR, ci.getID());

				int cou = mcr_linktable.countReferenceCategory(classID, categID);
				cat.setAttribute("counter", Integer.toString(cou));

				for (int i = 0; i < ci.getSize(); i++) {
					cat.addContent(ci.getJDOMElement(i));
				}

				if (ci.getURL().length() != 0) {
					org.jdom.Element u = new org.jdom.Element("url");
					u.setAttribute("href", ci.getURL(), org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
					cat.addContent(u);
				}

				cats.addContent(cat);
			}
		}

		elm.addContent(cats);

		return doc;
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
	public final byte[] receiveCategoryAsXML(String classID, String categID) {
		org.jdom.Document doc = receiveCategoryAsJDOM(classID, categID);
		byte[] xml = MCRUtils.getByteArray(doc);

		return xml;
	}

	/**
	 * The method get a XQuery and responds a JDOM document.
	 * 
	 * @param query
	 *            the query string
	 * @return a JDOM document
	 */
	public final org.jdom.Document search(String query) {
		// classification ID
		boolean cat = false;
		String classname = "";
		String categname = "";
		query = query.replace('\'', '\"');

		int classstart = query.indexOf("@ID");

		if (classstart == -1) {
			return null;
		}

		classstart = query.indexOf("\"", classstart);

		if (classstart == -1) {
			return null;
		}

		int classstop = query.indexOf("\"", classstart + 1);

		if (classstop == -1) {
			return null;
		}

		classname = query.substring(classstart + 1, classstop);

		// category ID
		int categstart = query.indexOf("@ID", classstop + 1);
		int categstop = -1;

		if (categstart != -1) {
			categstart = query.indexOf("\"", categstart);

			if (categstart == -1) {
				return null;
			}

			categstop = query.indexOf("\"", categstart + 1);

			if (categstop == -1) {
				return null;
			}

			categname = query.substring(categstart + 1, categstop);

			if (categname.equals("*")) {
				cat = false;
			} else {
				cat = true;
			}
		}

		if (cat) {
			return receiveCategoryAsJDOM(classname, categname);
		}
		return receiveClassificationAsJDOM(classname);
	}

	/**
	 * The method return a category ID for the given category label of a defined
	 * classification.
	 * 
	 * @param classid
	 *            the classification id as MCRObjectID
	 * @param labeltext
	 *            the text of a categoy label
	 * @return the correspondig category ID
	 */
	public static final String getCategoryID(MCRObjectID classid, String labeltext) {
		if (labeltext == null) {
			return "";
		}

		if (labeltext.length() == 0) {
			return "";
		}

		MCRClassificationItem cl = new MCRClassificationItem(classid);
		MCRCategoryItem cat = cl.getCategoryItemForLabelText(labeltext);

		if (cat == null) {
			return "";
		}

		return cat.getID();
	}

	/**
	 * The method returns all availiable classification ID's they are loaded.
	 * 
	 * @return a list of classification ID's as String array
	 */
	public static final String[] getAllClassificationID() {
		return (MCRClassificationItem.manager()).getAllClassificationID();
	}
	
 }
