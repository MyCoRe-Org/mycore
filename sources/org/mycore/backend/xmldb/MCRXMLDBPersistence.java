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

import java.util.GregorianCalendar;
import java.util.StringTokenizer;
import org.mycore.common.*;
import org.mycore.datamodel.metadata.*;
import org.jdom.Document;
import org.xmldb.api.*;
import org.xmldb.api.base.*;
import org.xmldb.api.modules.*;

/**
 * This class is the persistence layer for XML:DB databases.
 **/
public final class MCRXMLDBPersistence 
    implements MCRObjectPersistenceInterface {
    private MCRConfiguration config = MCRConfiguration.instance();  
    private static String conf_prefix = "MCR.persistence_xmldb_";
    private String driver;
    private String vendor;
    private String hostname;
    private int port;
    private String root;
    private String dbpath;
    private Collection rootCollection;

    /**
     * Creates a new MCRXMLDBPersistence.
     **/
    public MCRXMLDBPersistence() throws MCRPersistenceException {
        System.out.println("XMLStart mit: " + conf_prefix);
	driver = config.getString( conf_prefix + "driver" );
    	vendor = config.getString( conf_prefix + "vendor" );
	hostname = config.getString( conf_prefix + "hostname", "" );
	port = config.getInt( conf_prefix + "port", 0 );
	root = config.getString( conf_prefix + "root" );
	dbpath = config.getString( conf_prefix + "dbpath" );
	try {
	    String connString = MCRXMLDBTools.buildConnectString( vendor, 
								  hostname, 
								  port, 
								  dbpath + "/" + root );
            System.out.println("MCRXMLDBPersistence connString:" + connString); 
            System.out.println("MCRXMLDBPersistence driver    :" + driver); 
	    rootCollection = MCRXMLDBTools.getRootCollection( driver, 
							      connString );
	    if( rootCollection == null ) {
		Collection col = DatabaseManager.getCollection( MCRXMLDBTools.buildConnectString( vendor,
												  hostname,
												  port,
												  dbpath )
								);
		StringTokenizer st = new StringTokenizer( root, "/" );
		while( st.hasMoreTokens() ) {
		    col = MCRXMLDBTools.collectionExistsUnder( col, st.nextToken(), true );
		}
		rootCollection = MCRXMLDBTools.getRootCollection( driver, connString );
	    }
	}
	catch( Exception e ) {
            System.out.println("XMLException: " + e.getMessage());
	    throw new MCRPersistenceException( e.getMessage(), e );
	}
    }
    /**
     * This method creates and stores the data from MCRTypedContent and
     * XML data in the XMLDB datastore.
     *
     * @param mcr_tc the special typed content
     * @param doc the content as JDOM Document
     *
     * @throws MCRConfigurationExcpetion
     * @throws MCRPersistenceException
     **/
    public final void create( MCRTypedContent mcr_tc, Document doc, String mcr_ts ) 
	throws MCRPersistenceException {
	try {
            System.out.println("MCRTypedContent: " + mcr_tc);
            System.out.println("Document       : " + doc);
            System.out.println("String         : " + mcr_ts);
	    MCRObjectID mcr_id = null;
	    String mcr_label = null;
	    for( int i = 0; i < mcr_tc.getSize(); i++ ) {
		if( mcr_tc.getNameElement( i ).equals( "ID" ) ) {
		    mcr_id = new MCRObjectID( (String)mcr_tc.getValueElement( i ) ); 
		    mcr_label = (String)mcr_tc.getValueElement( i+1 ); }
	    }
            System.out.println("typeCollection: " + mcr_id.getTypeId().toLowerCase());
	    Collection typeCollection = rootCollection.getChildCollection( mcr_id.getTypeId().toLowerCase() );
	    MCRXMLDBItem item = new MCRXMLDBItem( typeCollection, mcr_id, doc );
	    item.create();
	}
	catch( Exception e ) {
	    throw new MCRPersistenceException( e.getMessage(), e );
	}
    }

    /**
     * Creates an empty database from the given configuration
     * file. Currently not used in this persistence layer.
     *
     * @param mcr_type
     * @param mcr_conf
     *
     * @throws MCRConfigurationException 
     * @throws MCRPersistenceException
     **/
    public void createDataBase( String mcr_type, org.jdom.Document mcr_conf )
	throws MCRConfigurationException, 
	       MCRPersistenceException {
	try {
	    CollectionManagementService cms = 
		(CollectionManagementService)
		rootCollection.getService( "CollectionManagementService",
					   "1.0" );
	    cms.createCollection( mcr_type.toLowerCase() );
	}
	catch( Exception e ) {
	    throw new MCRPersistenceException( e.getMessage(), e );
	}
    }

    /**
     * Updates the content in the database. Currently the same as
     * create. Should be made with XUpdate in the future.
     *
     * @param mcr_tc
     * @param doc
     **/    
    public void update( MCRTypedContent mcr_tc, Document doc, String mcr_ts ) {
        System.out.println("update");
	create( mcr_tc, doc, mcr_ts );
    }

    /**
     * Deletes the object with the given object id in the datastore.
     *
     * @param mcr_id id of the object to delete
     *
     * @throws MCRPersistenceException something goes wrong during delete
     **/     
    public void delete( MCRObjectID mcr_id ) 
	throws MCRPersistenceException {
	Collection typeCollection = null;
	try {
	    typeCollection = 
		rootCollection.getChildCollection( mcr_id.getTypeId().toLowerCase() );
	    MCRXMLDBItem item = new MCRXMLDBItem( typeCollection, 
						  mcr_id, 
						  null );
	    item.delete();
	}
	catch( Exception e ) {
	    throw new MCRPersistenceException( e.getMessage(), e );
	}
    }

    /**
     * Retrieves the object with the given ID from the datastore.
     *
     * @param mcr_id object id whose data shall be received
     *
     * @return the content as a JDOM document
     *
     * @throws MCRConfigurationException
     * @throws MCRPersistenceException
     **/
    public final byte[] receive( MCRObjectID mcr_id )
	throws MCRConfigurationException, 
	MCRPersistenceException {
        System.out.println("MCRXMLDBPersistence receive");    
	return MCRUtils.getByteArray( getItem( mcr_id ).getContent() );
    }

    /**
     * Retrieves a MCRXMLDBItem with given MCRObjectID from datastore.
     *
     * @param mcr_id the object id of the returned MCRXMLDBtem
     * @return the MCRXMLDBItem
     * @throws MCRPersistenceException
     **/
    protected MCRXMLDBItem getItem( MCRObjectID mcr_id ) 
	throws MCRPersistenceException {
	Collection typeCollection = null;
	MCRXMLDBItem item = null;
        System.out.println("MCRXMLDBPersistence getItem with: " + mcr_id.getTypeId().toLowerCase());    
	try {
	    typeCollection = 
		rootCollection.getChildCollection( mcr_id.getTypeId().toLowerCase() );

	    item = new MCRXMLDBItem( typeCollection, mcr_id, null );
	    item.retrieve();
	}
	catch( Exception e ) {
	    throw new MCRPersistenceException( e.getMessage(), e );
	}
	return item;
    }
    
    /**
     * This method returns the date of creation of the object with the
     * given ID. Currently not implemented.
     *
     * @param mcr_id the id of the object whose date of creation
     * should be returned
     * @return null
     * @throws MCRConfigurtionException
     * @throws MCRPersistenceException
     **/
    public final GregorianCalendar receiveCreateDate(MCRObjectID mcr_id)
	throws MCRConfigurationException, MCRPersistenceException {
	return null;
    }

    public final String receiveLabel(MCRObjectID mcr_id)
	throws MCRConfigurationException, MCRPersistenceException {
	return "";
    }

    public synchronized String getNextFreeId( String project_ID, String type_ID )
	throws MCRPersistenceException { 
        System.out.println("MCRXMLDBPersistence getNextFreeId");    
	return "";
	
    }

    /**
     * Checks whether an object with the given object id exists in the
     * datastore.
     *
     * @param mcr_id id of the object to delete
     *
     * @throws MCRPersistenceException something goes wrong during delete
     **/     
    public boolean exist( MCRObjectID mcr_id ) 
	throws MCRConfigurationException, MCRPersistenceException {
//  	Database database = null;
//  	Collection rootCollection = null;
//  	Collection typeCollection = null;
//  	try {
//  	    String connString = MCRXMLDBTools.buildConnectString( vendor, 
//  								  hostname, 
//  								  port, 
//  								  root );
//  	    rootCollection = MCRXMLDBTools.getRootCollection( driver, 
//  							    connString );
//  	    typeCollection = 
//  		rootCollection.getChildCollection( mcr_id.getProjectId().toLowerCase() ).getChildCollection( mcr_id.getTypeId().toLowerCase() );
	    
//  	    MCRXMLDBItem item = new MCRXMLDBItem( typeCollection, 
//  						  mcr_id, 
//  						  null );
//  	    item.delete();
//  	}
//  	catch( Exception e ) {
//  	    throw new MCRPersistenceException( e.getMessage(), e );
//  	}
//  	finally {
//  	    try {
//  		MCRXMLDBTools.safelyClose( rootCollection );
//  		if (database != null)
//  		    DatabaseManager.deregisterDatabase( database );
//  	    }
//  	    catch( Exception e ) {
//  		throw new MCRPersistenceException( e.getMessage(), e );
//  	    }
        System.out.println("Check exist");    
	return false;
    }
}
