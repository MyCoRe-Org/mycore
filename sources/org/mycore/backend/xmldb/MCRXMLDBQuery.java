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

import java.io.*;
import org.xmldb.api.*;
import org.xmldb.api.base.*;
import org.xmldb.api.modules.*;
import org.mycore.common.*;
import org.mycore.common.xml.*;
import org.mycore.services.query.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;

/**
 * This is the implementation of the MCRQueryInterface for the XML:DB API
 *
 * @author Marc Schluepmann
 * @author Harald Richter
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRXMLDBQuery extends MCRQueryBase {

private String database    = "";

public static final String DEFAULT_QUERY = "/*";

/**
 * The constructor.
 **/
public MCRXMLDBQuery() {
  super();
  MCRXMLDBConnectionPool.instance();
  database   = config.getString( "MCR.persistence_xmldb_database" , "");
  logger.info("MCRXMLDBQuery MCR.persistence_xmldb_database    : " + database); 
  }

/**
 * This method start the Query over one object type and return the 
 * result as MCRXMLContainer.
 *
 * @param type                  the MCRObject type
 * @return                      a result list as MCRXMLContainer
 **/
protected final MCRXMLContainer startQuery( String type ) {
  MCRXMLContainer result = new MCRXMLContainer();
  // Mark all document searches
  for (int i=0;i<subqueries.size();i++) {
    if (((String)subqueries.get(i)).indexOf(XPATH_ATTRIBUTE_DOCTEXT) != -1)
      flags.set(i,Boolean.TRUE);
    }
  // prepare the query over the metadata
  String query = handleQueryString(type);
  logger.debug(query);
  // do it over the metadata
  try {
    Collection collection = MCRXMLDBConnectionPool.instance().getConnection( type );
    XPathQueryService xps =
      (XPathQueryService)collection.getService( "XPathQueryService", "1.0" );
	    
    MCRXMLDBConnectionPool.instance().releaseConnection( collection );
    ResourceSet resultset = xps.query( query );
    logger.debug("Results: "+Integer.toString((int)resultset.getSize()));

    String objid = "";
    org.jdom.Document doc;
    ResourceIterator ri = resultset.getIterator();
    while( ri.hasMoreResources() ) {
      XMLResource xmldoc = (XMLResource)ri.nextResource();
      doc = MCRXMLDBPersistence.convertResToDoc( xmldoc );
      objid      =  doc.getRootElement().getAttribute( "ID" ).getValue();
      byte[] xml = MCRUtils.getByteArray( doc );
      result.add( "local", objid, 0, xml );
      }
    }
  catch( Exception e ) {
    throw new MCRPersistenceException( e.getMessage(), e ); }
  finally {
    try {
      }
    catch( Exception e ) {
      throw new MCRPersistenceException( e.getMessage(), e ); }
    }
  // Here you can add other searches and merge the result container with
  // them from the first query.
  return result;
  }

/**
 * Handle query string for XML:DB database
 **/
private String handleQueryString(String type) {
  if (subqueries.size() == 0) { return DEFAULT_QUERY; }
  StringBuffer qsb = new StringBuffer(1024);
  for (int i=0;i<subqueries.size();i++) {
    if (((Boolean)flags.get(i)).booleanValue()) continue;
    qsb.append(' ').append((String)subqueries.get(i)).append(' ')
      .append((String)andor.get(i));
    flags.set(i,Boolean.TRUE);
    }
  logger.debug(qsb.toString());
  if ( database.equals( "xindice" ) )
    return handleQueryStringXindice(qsb.toString().trim(),type);
  else if ( database.equals( "exist" ) )
    return handleQueryStringExist(qsb.toString().trim(),type);
  return qsb.toString();
  }
    
/**
 * Handle query string for Xindice
 **/
private String handleQueryStringXindice( String query, String type ) {
  return query;
  }
    
/**
 * Handle query string for exist
 **/
private String handleQueryStringExist( String query, String type ) {
  query = MCRUtils.replaceString(query, "like", "&=");
  query = MCRUtils.replaceString(query, "contains(", ".&=");
  query = MCRUtils.replaceString(query, ")", "");
  //query = MCRUtils.replaceString(query, "metadata/*/*/@href=", "metadata//*/@xlink:href=");
  // combine the separated queries
  int i = query.indexOf("[");
  if (i == -1) return DEFAULT_QUERY;
  String prefix = query.substring(0,i+1);
  query = MCRUtils.replaceString(query, "] and", " and");
  query = MCRUtils.replaceString(query, "] or", " or");
  query = MCRUtils.replaceString(query, "/mycoreobject["," ");
  query = MCRUtils.replaceString(query, "/mycorederivate["," ");
  query = prefix + query; 
  return query;
  }
}

