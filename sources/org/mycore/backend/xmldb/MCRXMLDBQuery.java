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
 * @author marc schluepmann
 * @author harald richter
 * @version $Revision$ $Date$
 **/
public class MCRXMLDBQuery implements MCRQueryInterface {

    static Logger logger = Logger.getLogger( MCRXMLDBQuery.class.getName() );

    // defaults
    private final int MAX_RESULTS = 1000;
    // private data
    private int maxres = 0;
    private MCRConfiguration config = null;
    private String database    = "";

    /**
     * The constructor.
     **/
    public MCRXMLDBQuery() {
	config = MCRConfiguration.instance();
	PropertyConfigurator.configure( config.getLoggingProperties() );
	maxres = config.getInt( "MCR.query_max_results", MAX_RESULTS );
        MCRXMLDBConnectionPool.instance();
        database   = config.getString( "MCR.persistence_xmldb_database" , "");
        logger.info("MCRXMLDBQuery MCR.persistence_xmldb_database    : " + database); 
    }

    /**
     * This method parses the XQuery string and return the result as
     * MCRQueryResultArray. If the type is null or empty or maxresults
     * is lower 1 an empty list was returned.
     *
     * @param query                 the XQuery string
     * @param maxresults            the maximum of results
     * @param type                  the MCRObject type
     * @return                      a result list as MCRXMLContainer
     **/
    public final MCRXMLContainer getResultList( String query,
						String type,
						int maxresults ) {

	MCRXMLContainer result = new MCRXMLContainer();
	if( (type == null) || ((type = type.trim()).length() == 0) )
	    return result;
	if( (maxresults < 1) || (maxresults > maxres) )
	    return result;
	if( query.trim().equals( "" ) )
	    query = "/*";
        query = handleQueryString( query, type);
	try {
	    Collection collection = MCRXMLDBConnectionPool.instance().getConnection( type );
	    XPathQueryService xps =
		(XPathQueryService)collection.getService( "XPathQueryService", "1.0" );
	    
            MCRXMLDBConnectionPool.instance().releaseConnection( collection );
	    ResourceSet resultset = xps.query( query );
	    //	    System.out.println( "Results: " + resultset.getSize() );

	    String objid = "";
            org.jdom.Document doc;
	    ResourceIterator ri = resultset.getIterator();
	    while( ri.hasMoreResources() ) {
		XMLResource xmldoc = (XMLResource)ri.nextResource();
                doc        = MCRXMLDBPersistence.convertResToDoc( xmldoc );
                objid      =  doc.getRootElement().getAttribute( "ID" ).getValue();
		byte[] xml = MCRUtils.getByteArray( doc );
		result.add( "local", objid, 0, xml );
	    }
	}
	catch( Exception e ) {
	    throw new MCRPersistenceException( e.getMessage(), e );
	}
	finally {
	    try {
  	    }
	    catch( Exception e ) {
		throw new MCRPersistenceException( e.getMessage(), e );
	    }
	}
	return result;
    }
    /**
     * Handle query string for XML:DB database
     **/
    private String handleQueryString( String query, String type ) {
        logger.debug("MCRXMLDBQuery handlequerstring   (old)  : " + query + " type : " + type); 
        
        if ( database.equals( "xindice" ) )
          query = handleQueryStringXindice( query, type );
        else if ( database.equals( "exist" ) )
          query = handleQueryStringExist( query, type );
        
        logger.debug("MCRXMLDBQuery handlequerstring   (new)  : " + query); 
	return query;
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
//        query = MCRUtils.replaceString(query, "contains(", "contains(.,");
        query = MCRUtils.replaceString(query, ")", "");
        query = MCRUtils.replaceString(query, "contains(", ".&=");
        query = MCRUtils.replaceString(query, "metadata/*/*/@href=", "metadata//*/@xlink:href=");
        if ( -1 != query.indexOf("] and") )
        {
          query = MCRUtils.replaceString(query, "[", "/");
          query = MCRUtils.replaceString(query, "] and", " and");
          query = "//*[" + query; 
        }
        query = MCRUtils.replaceString(query, "/mycoreobject/", "/");
	return query;
    }
}

