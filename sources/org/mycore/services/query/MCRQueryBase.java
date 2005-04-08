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
package org.mycore.services.query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.Namespace;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.xml.MCRXMLContainer;
import org.mycore.datamodel.ifs.MCRContentStoreFactory;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRXMLTableManager;

/**
 * This is the implementation of the MCRQueryInterface is a basic class which
 * the special query classes under the persistence layer should extend. Some
 * methods can be overwrite from this classes.
 *
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 **/
abstract public class MCRQueryBase implements MCRQueryInterface {

	// the logger
	protected static Logger logger =
		Logger.getLogger(MCRQueryBase.class.getName());
	// defaults
	/** The default maximum of resultes */
	public final static int MAX_RESULTS = 1000;
	/** The node name for a search in all document texts */
	public final static String XPATH_ATTRIBUTE_DOCTEXT = "doctext()";

	protected static String NL =
		new String((System.getProperties()).getProperty("line.separator"));

	// protected data
	protected int maxres = 0;
	protected int maxresults = 0;
	protected MCRConfiguration config = null;
	protected ArrayList flags = null;
	protected ArrayList subqueries = null;
	protected ArrayList andor = null;
	protected String root = null;
	protected MCRXMLTableManager xmltable = null;
	protected MCRTextSearchInterface[] tsint = null;
	protected boolean searchfulltext = true;

	/**
	 * The Constructor
	 **/
	public MCRQueryBase() {
		config = MCRConfiguration.instance();
		maxres = config.getInt("MCR.query_max_results", MAX_RESULTS);
		logger.info("The maximum of the results is " + Integer.toString(maxres));
		xmltable = MCRXMLTableManager.instance();
		tsint = MCRContentStoreFactory.getAllIndexables();
		searchfulltext = true;
	}

	/**
	 * This method parses the XPath Query string and return the result as
	 * MCRQueryResultArray. If the type is null or empty or maxresults is lower
	 * 1 an empty list was returned.
	 * 
	 * @param query
	 *            the XQuery string
	 * @param maxresults
	 *            the maximum of results
	 * @param type
	 *            a list of the MCRObject types seperated by ,
	 * @return a result list as MCRXMLContainer
	 */
	public MCRXMLContainer getResultList(String query, String type,
			int maxresults) {
		// check the input
		MCRXMLContainer result = new MCRXMLContainer();
		if ((type == null) || ((type = type.trim()).length() == 0))
			return result;
		if ((maxresults < 1) || (maxresults > maxres))
			return result;
		this.maxresults = maxresults;
		searchfulltext = true;
		// separate the query
		logger.debug("QIn: The incomming query is " + query);
		flags = new ArrayList();
		subqueries = new ArrayList();
		andor = new ArrayList();
		root = null;
		int i = 0, j = query.length(), l, m, n, kon;
		while (i < j) {
			l = query.indexOf("/mycore", i);
			if (l == -1) {
				break;
			}
			m = query.indexOf("[", l);
			if (m == -1) {
				logger.error("Error 1 while analyze the query string : "
						+ query);
				break;
			}
			if (root == null) {
				root = query.substring(l, m);
			}
			n = query.indexOf("]", m);
			if (n == -1) {
				logger.error("Error 2 while analyze the query string : "
						+ query);
				break;
			}
			kon = 1;
			for (int o = m + 1; o < n; o++) {
				if (query.charAt(o) == '[') {
					kon++;
				}
			}
			for (int o = kon; o > 1; o--) {
				n = query.indexOf("]", n + 1);
				if (n == -1) {
					logger.error("Error 3 while analyze the query string : "
							+ query);
					break;
				}
			}
			flags.add(Boolean.FALSE);
			subqueries.add(query.substring(m + 1, n));
			i = n + 1;
			if (i + 5 < j) {
				l = query.toLowerCase().indexOf(" and ", i);
				if (l != -1) {
					andor.add("and");
					i = l + 5;
					continue;
				}
			}
			if (i + 4 < j) {
				l = query.toLowerCase().indexOf(" or ", i);
				if (l != -1) {
					andor.add("or");
					i = l + 4;
					continue;
				}
			}
			andor.add("");
		}
		// debug subqueries
		logger.debug("QSubSize: "+Integer.toString(subqueries.size()));
		for (i = 0; i < flags.size(); i++) {
			logger.debug("Q: " + (String) subqueries.get(i) + " by "
					+ (String) andor.get(i));
		}
		logger.debug("R: The root string is " + root);
		// run over all types
		String typelist = config.getString("MCR.type_" + type, "none");
		if (typelist.equals("none")) {
			return result;
		}
		if (typelist.equals("true")) {
			typelist = type;
		}
		i = 0;
		j = typelist.length();
		l = i;
		String onetype = "";
		while ((l < j) && (l != -1)) {
			l = typelist.indexOf(",", i);
			if (l == -1) {
				onetype = typelist.substring(i, j);
			} else {
				onetype = typelist.substring(i, l);
				i = l + 1;
			}
			if ((onetype == null) || ((onetype = onetype.trim()).length() == 0)) {
				break;
			}
			try {
				String ct = config.getString("MCR.type_" + onetype);
				if (!ct.equals("true")) {
					continue;
				}
			} catch (MCRConfigurationException ce) {
				continue;
			}
			logger.debug("T: The separated query type is " + onetype);
			MCRObjectID [] testid = new MCRObjectID[subqueries.size()];
			int isid = 0;
			for (int p = 0; p < subqueries.size();p++) {
				String q = ((String) subqueries.get(p)).trim();
				q = q.replace('\'','\"');
				if (q.startsWith("@ID")) {
					int a = q.indexOf("\"");
					int b = q.indexOf("\"", a + 1);
					if (a <= 7) {
						if (b == q.length() - 1) {
							try {
								testid [p] = new MCRObjectID(q.substring(a + 1, b));
								isid++;
							} catch (Exception e) {
							}
						}
					}
				}
			}
			if ((isid == subqueries.size()) && (isid != 0)) {
				logger.debug("Retrieve the data direcly from XML database.");
				for (int p = 0; p < subqueries.size();p++) {
					if (onetype.equals(testid[p].getTypeId())) {
						try {
							byte[] xml = xmltable.retrieve(testid[p]);
							result.add("local", testid[p].getId(), 0, xml);
						} catch (Exception e) {
						}
					}
				}
			} else {
				result.importElements(startQuery(onetype));
				for (int o = 0; o < flags.size(); o++) {
					flags.set(o, Boolean.FALSE);
				}
			}
			int isfull = 0;
			for (int p = 0; p < subqueries.size();p++) {
				if (((String)subqueries.get(p)).startsWith("doctext()")) {
					isfull++;
					}
			}
			if (subqueries.size()==isfull) {
				searchfulltext = false; }
			else {
				searchfulltext = true; }
		}
		result.cutDownTo(maxresults);
		return result;
	}

	/**
	 * returns the ObjectID of the Object containing derivate with given ID
	 * 
	 * @param DerivateID
	 *            ID of Derivate
	 * @return ObjectID if found, else null
	 */
	public MCRObjectID getObjectID(String DerivateID) {
		HashSet in = new HashSet();
		in.add(new MCRObjectID(DerivateID));
		MCRXMLContainer derivate = createResultContainer(in);
		//<mycorederivate
		if (derivate.size() > 0) {
			Element derivateE = derivate.getXML(0);
			String objectID =
				derivateE
					.getChild("derivate")
					.getChild("linkmetas")
					.getChild("linkmeta")
					.getAttributeValue(
						"href",
						Namespace.getNamespace("http://www.w3.org/1999/xlink"));
			return (new MCRObjectID(objectID));
		}
		else logger.warn("Error retrieving Meta infos of "+DerivateID);
		return null;
	}

	/**
	 * This method take a n ArrayList of MCRObjectID's and put the coresponding XML in a
	 * MCRResultContainer. If the method can not find a coresponding XML, nothing will be add
	 * for this MCRObjectID.
	 *
	 * @param objectIDs	the set of MCRObjectID's
	 * @return a MCRResultContainer
	 **/
	protected MCRXMLContainer createResultContainer(HashSet objectIDs) {
		MCRXMLContainer result = new MCRXMLContainer();
		if (objectIDs == null) {
			return result;
		}
		for (Iterator it = objectIDs.iterator(); it.hasNext();) {
			try {
				MCRObjectID s = (MCRObjectID) it.next();
				byte[] xml = xmltable.retrieve(s);
				result.add("local", s.getId(), 0, xml);
			} catch (Exception e) {
			}
		}
		return result;
	}

	/**
	 * This method start the Query over one object type and return the
	 * result as MCRXMLContainer. This implementation must be overwrite with
	 * them form the persitence layer.
	 *
	 * @param type                  the MCRObject type
	 * @return                      a result list as MCRXMLContainer
	 **/
	abstract protected MCRXMLContainer startQuery(String type);

}
