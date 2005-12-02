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
import org.jdom.JDOMException;
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

	private static final String cAttr = "counter";

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
		cl = new MCRClassificationItem(new MCRObjectID(jdom.getRootElement().getAttributeValue("ID")).getId());

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
		MCRCategoryItem ci = new MCRCategoryItem(category.getAttributeValue("ID"), parent);
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

		String ID = jdom.getRootElement().getAttributeValue("ID");
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

	private static final void getHashedIDs(Element root, Hashtable sink) {
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
			}
			getHashedIDs(cur, sink); // recursive call for all children
		}
	}

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
		String id = root.getAttributeValue("ID");
		if (id == null || !keepIDs.contains(id)) {
			// LOGGER.debug("gently removing "+root.getName()+" with ID "+id);
			return true;
		}
		return false;
	}

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
				attr = ((Element) subCategs.get(j)).getAttributeValue(cAttr);
				if (attr == null) {
					cAvailable = false;
					break;
				}
				subTotals += Integer.parseInt(attr);
			}
			attr = oldCateg.getAttributeValue(cAttr);
			/* We calculated the total number of links to all children
			 * at this point. If this number is equal to the counter of the
			 * current category element, than there no need for further checking.
			 * This category only contains links that its children contains and no other.
			 * We'll check this issue in the following if-block.
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

				//This call only returns IDs, that are in the current category but not in its children
				List activeLinks = MCRLinkTableManager.instance().getFirstLinksToCategory(classID, attr);

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
	 * The method update a MCRClassification from the given JDOM tree.
	 * 
	 * @param jdom
	 *            the classification as jdom tree
	 */
	public final String updateFromJDOM(org.jdom.Document jdom) throws MCRActiveLinkException {
		// old part 1 starts
		org.jdom.Element root = jdom.getRootElement();
		String ID = (String) root.getAttribute("ID").getValue();
		MCRObjectID mcr_id = new MCRObjectID(ID);
		// old part 1 ends

		if (MCRXMLTableManager.instance().exist(mcr_id)) {
			// save old classification for later reference
			Document oldClass = MCRClassification.receiveClassificationAsJDOM(jdom.getRootElement().getAttributeValue("ID"));

			Hashtable oldIDs = new Hashtable();
			getHashedIDs(oldClass.getRootElement(), oldIDs); // all categ-IDs
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
			checkActiveLinks(jdom.getRootElement().getAttributeValue("ID"), raked.getRootElement(), oldIDs);
			// If the code tuns through the previous method without error we're
			// fine
		}
		// now updates may follow if the structure of the categories has changed
		// with the update
		// a transaction should start here to be save: now the write access
		// begins

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
	public final String updateFromXML(byte[] xml) throws MCRException {
		try {
			org.jdom.input.SAXBuilder bulli = new org.jdom.input.SAXBuilder(false);
			org.jdom.Document jdom = bulli.build(new ByteArrayInputStream(xml));

			return updateFromJDOM(jdom);
		} catch (Exception e) {
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
	 */
	public final String updateFromURI(String uri) throws MCRException {
		try {
			org.jdom.Document jdom = MCRXMLHelper.parseURI(uri);

			return updateFromJDOM(jdom);
		} catch (Exception e) {
			throw new MCRException(e.getMessage(), e);
		}
	}

	private static Document getClassification(String ID) throws MCRException, JDOMException, IOException {
		MCRObjectID classID = new MCRObjectID(ID);
		LOGGER.debug("Loading Classification " + ID + " of MCRType: " + classID.getTypeId());

		MCRXMLTableManager tm = MCRXMLTableManager.instance();

		return tm.readDocument(classID);
	}

	/**
	 * The method return the classification as JDOM tree.
	 * 
	 * @param ID
	 *            the classification ID to delete
	 * @return the classification as JDOM
	 */
	public static final Document receiveClassificationAsJDOM(String classID) {
		Document classification = null;

		try {
			classification = getClassification(classID);
		} catch (MCRException e) {
			LOGGER.error("Oops", e);
		} catch (JDOMException e) {
			LOGGER.error("Oops", e);
		} catch (IOException e) {
			LOGGER.error("Oops", e);
		}

		Map map = MCRLinkTableManager.instance().countCategoryReferencesSharp(classID);
		// in map we've got the sharp number for every categoryID (without
		// children)
		// now we add to every categoryID the numbers of the children
		for (Iterator it = classification.getDescendants(new ElementFilter("category")); it.hasNext();) {
			Element category = (Element) it.next();
			String mapKey = classID + "##" + category.getAttributeValue("ID");
			int count = (map.get(mapKey) != null) ? ((Integer) map.get(mapKey)).intValue() : 0;
			category.setAttribute(cAttr, Integer.toString(count));
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
		elm.setAttribute("ID", classID);

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
				cat.setAttribute("ID", ci.getID());

				int cou = mcr_linktable.countCategoryReferencesFuzzy(classID, categID);
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
		} else {
			return receiveClassificationAsJDOM(classname);
		}
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
