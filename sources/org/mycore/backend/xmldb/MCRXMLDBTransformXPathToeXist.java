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
package org.mycore.backend.xmldb;

import java.util.HashSet;

import org.xmldb.api.base.*;
import org.xmldb.api.modules.*;
import org.mycore.common.*;
import org.mycore.common.xml.*;
import org.mycore.datamodel.metadata.*;
import org.mycore.services.query.*;

/**
 * This is the implementation of the MCRQueryInterface for the XML:DB API
 *
 * @author Marc Schluepmann
 * @author Harald Richter
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 **/
public class MCRXMLDBTransformXPathToeXist extends MCRQueryBase {

	private String database = "";

	public static final String DEFAULT_QUERY = "/*";

	/**
	 * The constructor.
	 **/
	public MCRXMLDBTransformXPathToeXist() {
		super();
		MCRXMLDBConnectionPool.instance();
		database = config.getString("MCR.persistence_xmldb_database", "");
		logger.debug(
			"MCRXMLDBQuery MCR.persistence_xmldb_database    : " + database);
	}

	/**
	 * This method start the Query over one object type and return the 
	 * result as MCRXMLContainer.
	 *
	 * @param type                  the MCRObject type
	 * @return                      a result list as MCRXMLContainer
	 **/
	protected final MCRXMLContainer startQuery(String type) {
		MCRXMLContainer result = new MCRXMLContainer();
		boolean hasts = false;
		boolean hasmeta = false;

		// Make all document searches
		HashSet idts = new HashSet();
		for (int i = 0; i < subqueries.size(); i++) {
			if (((String) subqueries.get(i)).indexOf(XPATH_ATTRIBUTE_DOCTEXT)
				!= -1) {
				hasts = true;
				flags.set(i, Boolean.TRUE);
				logger.debug("TextSearch query : " + (String) subqueries.get(i));
				// start the query against the textsearch
				for (int j = 0; j < tsint.length; j++) {
					String[] der = tsint[j].getDerivateIDs((String) subqueries.get(i));
					for (int k = 0; k < der.length; k++) {
						idts.add(getObjectID(der[k]));
					}
				}
			}
		}

		// prepare the query over the rest of the metadata
		HashSet idmeta = new HashSet();
		String query = handleQueryString(type);
		logger.debug("Transformed query : " + query);
		// do it over the metadata
		if (query.length() != 0) {
			hasmeta = true;
			try {
				Collection collection =
					MCRXMLDBConnectionPool.instance().getConnection(type);
				XPathQueryService xps =
					(XPathQueryService) collection.getService("XPathQueryService", "1.0");

				MCRXMLDBConnectionPool.instance().releaseConnection(collection);
				ResourceSet resultset = xps.query(query);
				logger.debug("Results: " + Integer.toString((int) resultset.getSize()));

				org.jdom.Document doc;
				ResourceIterator ri = resultset.getIterator();
				MCRXMLTableManager xmltable = MCRXMLTableManager.instance();
				while (ri.hasMoreResources()) {
					XMLResource xmldoc = (XMLResource) ri.nextResource();
					doc = MCRXMLDBPersistence.convertResToDoc(xmldoc);
					idmeta.add(
						new MCRObjectID(
							doc.getRootElement().getAttribute("ID").getValue()));
				}
			} catch (Exception e) {
				throw new MCRPersistenceException(e.getMessage(), e);
			}
		}

		// merge the results
		HashSet myresult = null;
		if (!hasts) {
			myresult = idmeta;
		}
		if (!hasmeta) {
			myresult = idts;
		}
		if ((hasts) && (hasmeta)) {
			myresult = MCRUtils.mergeHashSets(idts, idmeta, MCRUtils.COMMAND_AND);
		}

		// put the XML files in the result container
		result = createResultContainer(myresult);
		return result;
	}

	/**
	 * Handle query string for XML:DB database
	 **/
	private String handleQueryString(String type) {
		if (subqueries.size() == 0) {
			return DEFAULT_QUERY;
		}
		StringBuffer qsb = new StringBuffer(1024);
		for (int i = 0; i < subqueries.size(); i++) {
			if (((Boolean) flags.get(i)).booleanValue())
				continue;
			qsb.append(' ').append((String) subqueries.get(i)).append(' ').append(
				(String) andor.get(i));
			flags.set(i, Boolean.TRUE);
		}
		logger.debug("Incomming condition : " + qsb.toString());
		if (database.equals("exist") && (qsb.length() != 0))
			return handleQueryStringExist(qsb.toString().trim(), type);
		return qsb.toString();
	}

	/**
	 * Handle query string for exist
	 **/
	private String handleQueryStringExist(String query, String type) {
		query = MCRUtils.replaceString(query, "like", "&=");
		query = MCRUtils.replaceString(query, "text()", ".");
		query = MCRUtils.replaceString(query, "ts()", ".");
		query = MCRUtils.replaceString(query, "contains(", ".&=");
		query = MCRUtils.replaceString(query, ")", "");
		// combine the separated queries
		query = root + "[" + query + "]";
		return query;
	}
}
