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

import org.xmldb.api.*;
import org.xmldb.api.base.*;
import org.xmldb.api.modules.*;
import org.mycore.common.*;
import org.mycore.common.xml.*;
import org.mycore.services.query.*;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


/**
 * This is the implementation of the MCRQueryInterface for the XML:DB API
 *
 * @author marc schluepmann
 * @version $Revision$ $Date$
 **/
public class MCRXMLDBQuery implements MCRQueryInterface {
    // common data
//      protected static String NL =
//  	new String( (System.getProperties()).getProperty( "line.separator" )
//  		    );

    static Logger logger = Logger.getLogger( MCRXMLDBQuery.class.getName() );

    // defaults
    private final int MAX_RESULTS = 1000;
    // private data
    private static String conf_prefix = "MCR.persistence_xmldb_";
    private int maxres = 0;
    private String vendor;
    private String hostname;
    private int port;
    private String dbpath;
    private String root;
    private MCRConfiguration config = null;
    private Collection rootCollection = null;
    private Collection typeCollection = null;
    private Database database = null;

    /**
     * The constructor.
     **/
    public MCRXMLDBQuery() {
	config = MCRConfiguration.instance();
	PropertyConfigurator.configure( config.getLoggingProperties() );
	maxres = config.getInt( "MCR.query_max_results", MAX_RESULTS );
    	vendor = config.getString( conf_prefix + "vendor" );
	hostname = config.getString( conf_prefix + "hostname", "" );
	port = config.getInt( conf_prefix + "port", 0 );
	dbpath = config.getString( conf_prefix + "dbpath" );
	root = config.getString( conf_prefix + "root" );
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
	// This has to be parametrized!
	String projectID = "MyCoReDemoDC";

	MCRXMLContainer result = new MCRXMLContainer();
	if( (type == null) || ((type = type.trim()).length() == 0) )
	    return result;
	if( (maxresults < 1) || (maxresults > maxres) )
	    return result;
	if( query.trim().equals( "" ) )
	    query = "/*";
	try {
	    Class driverclass = Class.forName( config.getString( conf_prefix + "driver" ) );
	    database = (Database)driverclass.newInstance();
	    DatabaseManager.registerDatabase( database );

	    String connString = MCRXMLDBTools.buildConnectString( vendor, hostname, port, dbpath + "/" + root );

	    rootCollection = DatabaseManager.getCollection( connString );
	    typeCollection = rootCollection.getChildCollection( type.toLowerCase() );
	    XPathQueryService xps =
		(XPathQueryService)typeCollection.getService(
							     "XPathQueryService", "1.0" );
	    
	    ResourceSet resultset = xps.query( query );
	    //	    System.out.println( "Results: " + resultset.getSize() );

	    String objid = "";
	    ResourceIterator ri = resultset.getIterator();
	    while( ri.hasMoreResources() ) {
		XMLResource xmldoc = (XMLResource)ri.nextResource();
		//		Document doc = (Document)xmldoc.getContentAsDOM();
		//  		doc.getDocumentElement().removeAttribute( "src:col" );
		//  		doc.getDocumentElement().removeAttribute( "src:key" );
		//  		doc.getDocumentElement().removeAttribute( "xmlns:src" );
		//		xmldoc.setContentAsDOM( doc );
		MCRXMLDBItem item = new MCRXMLDBItem( xmldoc );
		objid = item.getId().getId();
		byte[] xml = MCRUtils.getByteArray( item.getContent() );
		result.add( "local", objid, 0, xml );
	    }
	}
	catch( Exception e ) {
	    throw new MCRPersistenceException( e.getMessage(), e );
	}
	finally {
	    try {
		MCRXMLDBTools.safelyClose( typeCollection );
		MCRXMLDBTools.safelyClose( rootCollection );
		if (database != null)
		    DatabaseManager.deregisterDatabase( database );
  	    }
	    catch( Exception e ) {
		throw new MCRPersistenceException( e.getMessage(), e );
	    }
	}
	return result;
    }
}

