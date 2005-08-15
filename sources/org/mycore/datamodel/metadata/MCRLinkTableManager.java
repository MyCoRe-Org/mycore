/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.datamodel.metadata;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;

/**
 * This class manage all accesses to the link table database. This database
 * holds all informations about links between MCRObjects/MCRClassifications.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRLinkTableManager {

	/** The list of the table types * */
	public static final String[] LINK_TABLE_TYPES = { "class", "href" };

	/** The link table manager singleton */
	protected static MCRLinkTableManager singleton;

	// logger
	static Logger logger = Logger
			.getLogger(MCRLinkTableManager.class.getName());

	// the list of link table
	private String persistclassname = null;

	private ArrayList tablelist;

	/**
	 * Returns the link table manager singleton.
	 */
	public static synchronized MCRLinkTableManager instance() {
		if (singleton == null)
			singleton = new MCRLinkTableManager();
		return singleton;
	}

	/**
	 * The constructor of this class.
	 */
	protected MCRLinkTableManager() {
		MCRConfiguration config = MCRConfiguration.instance();
		// Load the persistence class
		persistclassname = config.getString("MCR.linktable_store_class");
		Object obj = new Object();
		tablelist = new ArrayList();
		for (int i = 0; i < LINK_TABLE_TYPES.length; i++) {
			try {
				obj = Class.forName(persistclassname).newInstance();
			} catch (ClassNotFoundException e) {
				throw new MCRException(persistclassname
						+ " ClassNotFoundException for " + LINK_TABLE_TYPES[i]);
			} catch (IllegalAccessException e) {
				throw new MCRException(persistclassname
						+ " IllegalAccessException for " + LINK_TABLE_TYPES[i]);
			} catch (InstantiationException e) {
				throw new MCRException(persistclassname
						+ " InstantiationException for " + LINK_TABLE_TYPES[i]);
			}
			try {
				((MCRLinkTableInterface) obj).init(LINK_TABLE_TYPES[i]);
			} catch (Exception e) {
				e.printStackTrace();
				throw new MCRException(" UnknownException for "
						+ LINK_TABLE_TYPES[i], e);
			}
			tablelist.add(obj);
		}
	}

	/**
	 * The method check the type.
	 * 
	 * @param type
	 *            the table type
	 * @return true if the type is in the list, else return false
	 */
	private final int checkType(String type) {
		if ((type == null) || ((type = type.trim()).length() == 0)) {
			return -1;
		}
		for (int i = 0; i < MCRLinkTableManager.LINK_TABLE_TYPES.length; i++) {
			if (type.equals(MCRLinkTableManager.LINK_TABLE_TYPES[i])) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * The method add a reference link pair for the given type to the store.
	 * 
	 * @param type
	 *            the table type
	 * @param from
	 *            the source of the reference as MCRObjectID
	 * @param to
	 *            the target of the reference as MCRObjectID
	 */
	public void addReferenceLink(String type, MCRObjectID from, MCRObjectID to) {
		addReferenceLink(type, from.getId(), to.getId());
	}

	/**
	 * The method add a reference link pair for the given type to the store.
	 * 
	 * @param type
	 *            the table type
	 * @param from
	 *            the source of the reference as String
	 * @param to
	 *            the target of the reference as String
	 */
	public void addReferenceLink(String type, String from, String to) {
		int i = checkType(type);
		if (i == -1) {
			logger
					.warn("The type value of a reference link is false, the link was "
							+ "not added to the link table");
			return;
		}
		if ((from == null) || ((from = from.trim()).length() == 0)) {
			logger
					.warn("The from value of a reference link is false, the link was "
							+ "not added to the link table");
			return;
		}
		if ((to == null) || ((to = to.trim()).length() == 0)) {
			logger
					.warn("The to value of a reference link is false, the link was "
							+ "not added to the link table");
			return;
		}
		logger
				.debug("Link in table " + type + " add for " + from + "<-->"
						+ to);
		try {
			((MCRLinkTableInterface) tablelist.get(i)).create(from, to);
		} catch (Exception e) {
			logger.warn("An error was occured while add a dataset from the"
					+ " reference link table, add not succesful.");
		}
	}

	/**
	 * The method delete a reference link pair for the given type to the store.
	 * 
	 * @param type
	 *            the table type
	 * @param from
	 *            the source of the reference as MCRObjectID
	 */
	public void deleteReferenceLink(String type, MCRObjectID from) {
		deleteReferenceLink(type, from.getId());
	}

	/**
	 * The method delete a reference link pair for the given type to the store.
	 * 
	 * @param type
	 *            the table type
	 * @param from
	 *            the source of the reference as String
	 */
	public void deleteReferenceLink(String type, String from) {
		int i = checkType(type);
		if (i == -1) {
			logger
					.warn("The type value of a reference link is false, the link was "
							+ "not deleted from the link table");
			return;
		}
		if ((from == null) || ((from = from.trim()).length() == 0)) {
			logger
					.warn("The from value of a reference link is false, the link was "
							+ "not deleted from the link table");
			return;
		}
		try {
			((MCRLinkTableInterface) tablelist.get(i)).delete(from);
		} catch (Exception e) {
			logger.warn("An error was occured while delete a dataset from the"
					+ " reference link table, deleting is not succesful.");
		}
	}

	/**
	 * The method add a classification link pair for the given type to the
	 * store.
	 * 
	 * @param from
	 *            the source of the reference as MCRObjectID
	 * @param classid
	 *            the target classification id as MCRObjectID
	 * @param categid
	 *            the target category id ad String
	 */
	public void addClassificationLink(MCRObjectID from, MCRObjectID classid,
			String categid) {
		addReferenceLink("class", from.getId(), classid.getId() + "##"
				+ categid);
	}

	/**
	 * The method add a classification link pair for the given type to the
	 * store.
	 * 
	 * @param from
	 *            the source of the reference as String
	 * @param classid
	 *            the target classification id as String
	 * @param categid
	 *            the target category id ad String
	 */
	public void addClassificationLink(String from, String classid,
			String categid) {
		addReferenceLink("class", from, classid + "##" + categid);
	}

	/**
	 * The method delete a classification link pair for the given type to the
	 * store.
	 * 
	 * @param from
	 *            the source of the reference as MCRObjectID
	 */
	public void deleteClassificationLink(MCRObjectID from) {
		deleteReferenceLink("class", from.getId());
	}

	/**
	 * The method delete a classification link pair for the given type to the
	 * store.
	 * 
	 * @param from
	 *            the source of the reference as String
	 */
	public void deleteClassificationLink(String from) {
		deleteReferenceLink("class", from);
	}

	/**
	 * The method coutn the reference links for a given object ID.
	 * 
	 * @param type
	 *            the table type
	 * @param to
	 *            the object ID as String, they was referenced
	 * @return the number of references
	 */
	public int countReferenceLinkTo(String type, String to) {
		int i = checkType(type);
		if (i == -1) {
			logger
					.warn("The type value of a reference link is false, the link was "
							+ "not added to the link table");
			return 0;
		}
		if ((to == null) || ((to = to.trim()).length() == 0)) {
			logger
					.warn("The to value of a reference link is false, the link was "
							+ "not added to the link table");
			return 0;
		}
		try {
			return ((MCRLinkTableInterface) tablelist.get(i)).countTo(to);
		} catch (Exception e) {
			logger.warn("An error was occured while search for references of "
					+ to + ".");
		}
		return 0;
	}

	/**
	 * The method coutn the reference links for a given object ID.
	 * 
	 * @param type
	 *            the table type
	 * @param to
	 *            the object ID as String, they was referenced
	 * @param to
	 *            document_type Array
	 * @param to
	 *            the restricion of a category id
	 * @return the number of references
	 */
	public int countReferenceLinkTo(String type, String to, String[] doctypes,
			String restriction) {

		int i = checkType(type);
		if (i == -1) {
			logger
					.warn("The type value of a reference link is false, the link was "
							+ "not added to the link table");
			return 0;
		}
		if ((to == null) || ((to = to.trim()).length() == 0)) {
			logger
					.warn("The to value of a reference link is false, the link was "
							+ "not added to the link table");
			return 0;
		}
		try {
			if ((doctypes != null && doctypes.length > 0)) {
				String mydoctype = "";
				int cnt = 0, idt = 0;
				for (idt = 0; idt < doctypes.length; idt++) {
					mydoctype = doctypes[idt];
					cnt += ((MCRLinkTableInterface) tablelist.get(i)).countTo(
							to, mydoctype, restriction);
				}
				return cnt;
			} else {
				return ((MCRLinkTableInterface) tablelist.get(i)).countTo(to,
						"", restriction);
			}
		} catch (Exception e) {
			logger.warn("An error was occured while search for references of "
					+ to + ".");
		}
		return 0;
	}

	/**
	 * The method coutn the reference links for a given object ID.
	 * 
	 * @param type
	 *            the table type
	 * @param to
	 *            the object ID as MCRObjectID, they was referenced
	 * @return the number of references
	 */
	public int countReferenceLinkTo(String type, MCRObjectID to) {
		return countReferenceLinkTo(type, to.getId());
	}

	/**
	 * The method count the number of references to a category of a
	 * classification without sub ID's.
	 * 
	 * @param classid
	 *            the classification ID as MCRObjectID
	 * @param caregid
	 *            the category ID as String
	 * @return the number of references
	 */
	public int countCategoryReferencesSharp(MCRObjectID classid, String categid) {
		return countReferenceLinkTo("class", classid.getId() + "##" + categid);
	}

	/**
	 * The method count the number of references to a category of a
	 * classification without sub ID's.
	 * 
	 * @param classid
	 *            the classification ID as String
	 * @param caregid
	 *            the category ID as String
	 * @return the number of references
	 */
	public int countCategoryReferencesSharp(String classid, String categid) {
		return countReferenceLinkTo("class", classid + "##" + categid);
	}

	/**
	 * The method count the number of references to a category of a
	 * classification including sub ID's.
	 * 
	 * @param classid
	 *            the classification ID as MCRObjectID
	 * @param caregid
	 *            the category ID as String
	 * @return the number of references
	 */
	public int countCategoryReferencesFuzzy(MCRObjectID classid, String categid) {
		return countReferenceLinkTo("class", classid.getId() + "##" + categid
				+ "%");
	}

	/**
	 * The method count the number of references to a category of a
	 * classification including sub ID's.
	 * 
	 * @param classid
	 *            the classification ID as String
	 * @param caregid
	 *            the category ID as String
	 * @return the number of references
	 */
	public int countCategoryReferencesFuzzy(String classid, String categid) {
		return countReferenceLinkTo("class", classid + "##" + categid + "%");
	}

	/**
	 * The method count the number of references to a category of a
	 * classification including sub ID's.
	 * 
	 * @param classid
	 *            the classification ID as String
	 * @param categid
	 *            the category ID as String
	 * @param doctypes
	 *            Array of document type slected by the mcrfrom content
	 * @return the number of references
	 */
	public int countCategoryReferencesFuzzy(String classid, String categid,
			String[] doctypes, String restriction) {
		return countReferenceLinkTo("class", classid + "##" + categid + "%",
				doctypes, restriction);
	}

}
