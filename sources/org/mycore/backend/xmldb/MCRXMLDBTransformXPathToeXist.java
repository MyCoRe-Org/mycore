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

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.xmldb.api.base.*;
import org.xmldb.api.modules.*;

import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import org.mycore.common.*;
import org.mycore.common.xml.*;
import org.mycore.datamodel.metadata.*;
import org.mycore.services.query.*;

/**
 * This is the implementation of the MCRMetaSearchInterface for the XML:DB API
 *
 * @author Marc Schluepmann
 * @author Harald Richter
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 **/
public class MCRXMLDBTransformXPathToeXist implements MCRMetaSearchInterface {

/** The default query **/
public static final String DEFAULT_QUERY = "/*";

// the logger
protected static Logger logger =
  Logger.getLogger(MCRXMLDBTransformXPathToeXist.class.getName());
private MCRConfiguration config = null;
private String database;

/**
 * The constructor.
 **/
public MCRXMLDBTransformXPathToeXist() {
  config = MCRConfiguration.instance();
  PropertyConfigurator.configure(config.getLoggingProperties());
  MCRXMLDBConnectionPool.instance();
  database = config.getString("MCR.persistence_xmldb_database", "");
  logger.debug("MCR.persistence_xmldb_database    : " + database);
  }

/**
 * This method start the Query over the XML:DB persitence layer for one 
 * object type and and return the query result as HashSet of MCRObjectIDs.
 *
 * @param root                  the query root
 * @param query                 the metadata queries
 * @param type                  the MCRObject type
 * @param maxresults            the maximum of results
 * @return                      a result list as MCRXMLContainer
 **/
public final HashSet getResultIDs(String root, String query, String type,
  int maxresults)
  {
  // prepare the query over the rest of the metadata
  HashSet idmeta = new HashSet();
  logger.debug("Incomming condition : " + query);
  String newquery = "";
  if ((root==null) && (query.length()==0)) { newquery = DEFAULT_QUERY; }
  if (database.equals("exist") && (query.length() != 0)) {
    newquery = handleQueryStringExist(root,query,type); }
  if ( database.equals("tamino") && (query.length() != 0)) {
    newquery = handleQueryStringTamino(root,query,type ); }
  logger.debug("Transformed query : " + newquery);

  // do it over the metadata
  if (newquery.length() != 0) {
    try {
      Collection collection = MCRXMLDBConnectionPool.instance().getConnection(type);
      XPathQueryService xps = (XPathQueryService) collection.getService("XPathQueryService", "1.0");

      MCRXMLDBConnectionPool.instance().releaseConnection(collection);
      ResourceSet resultset = xps.query(newquery);
      logger.debug("Results: " + Integer.toString((int) resultset.getSize()));
      org.jdom.Document doc;
      ResourceIterator ri = resultset.getIterator();
      MCRXMLTableManager xmltable = MCRXMLTableManager.instance();
      while (ri.hasMoreResources()) {
        XMLResource xmldoc = (XMLResource) ri.nextResource();
        doc = MCRXMLDBPersistence.convertResToDoc(xmldoc);
        if (doc==null) {
          throw new NullPointerException("Document is null"); }
        if (doc.getRootElement()==null) {
          throw new NullPointerException("Document root element is null"); }
	if (doc.getRootElement().getAttribute("ID")==null) {
          XMLOutputter output=new XMLOutputter(Format.getPrettyFormat());
          output.output(doc,System.err);
          throw new NullPointerException("Root elements Attribute \"ID\" is not available");
          }
        idmeta.add(new MCRObjectID(doc.getRootElement().getAttribute("ID").getValue()));
        }
      } 
    catch (Exception e) {
      throw new MCRPersistenceException(e.getMessage(), e); }
    }

  return idmeta;
  }

/**
 * Handle query string for exist
 **/
private String handleQueryStringExist(String root, String query, String type) {
	query = MCRUtils.replaceString(query, "#####", "");
	query = MCRUtils.replaceString(query, "like", "&=");
	query = MCRUtils.replaceString(query, "text()", ".");
	query = MCRUtils.replaceString(query, "ts()", ".");
	query = MCRUtils.replaceString(query, "contains(", "&=");
	query = MCRUtils.replaceString(query, ")", "");
	// combine the separated queries
	query = root + "[" + query + "]";
	return query;
}
  
/**
 * Handle query string for Tamino
 **/
private String handleQueryStringTamino(String root, String query, String type ) {
	query = MCRUtils.replaceString(query, "#####", "");
	query = MCRUtils.replaceString(query, "like", "~=");    // 030919
	query = MCRUtils.replaceString(query, ")", "");
	query = MCRUtils.replaceString(query, "\"", "'");
	query = MCRUtils.replaceString(query, "contains(", "~=");
	query = MCRUtils.replaceString(query, "metadata/*/*/@href=", "metadata//@xlink:href=");

    if ( -1 != query.indexOf("] and") )
    {
      query = MCRUtils.replaceString(query, "] and /mycoreobject[", " and /mycoreobject/");  // 031002
    }

		query = root + "[" + query + "]";
    return query;
  }
}
