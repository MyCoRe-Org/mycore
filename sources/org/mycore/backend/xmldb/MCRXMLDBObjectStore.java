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

import org.mycore.common.*;
import org.mycore.datamodel.ifs.*;
import java.util.*;
import java.io.*;
import org.xmldb.api.base.*;
import org.xmldb.api.modules.*;
import org.xmldb.api.*;

/**
 * This class implements the MCRContentStore interface to store the content of
 * MCRFile objects in a XMLDB ready database. 
 *
 * THIS CONTENTSTORE IS UNTESTED BECAUSE THERE EXISTS CURRENTLY NO
 * OPEN SOURCE DATABASE SYSTEM WHICH IMPLEMENTED XML:DB BINARYRESOURCE
 *
 * @author marc schluepmann
 * @version $Revision$ $Date$
 */
public class MCRXMLDBObjectStore extends MCRContentStore {
    protected int segmentSize;
    
    /** The unique store ID for this MCRContentStore implementation */
    protected String storeID;

    /** The database we use to store our content */
    protected Database database;

    /** The root collection for our whole content */
    protected org.xmldb.api.base.Collection rootCollection;

    /** 
     * Creates a new MCRCStoreXMLDB instance. This instance has to 
     * be initialized by calling init() before it can be used.
     */
    public MCRXMLDBObjectStore() {}
    
    public void init( String storeID ) 
	throws MCRPersistenceException {
	this.storeID = storeID;
	String prefix = "MCR.IFS.ContentStore." + storeID + ".";

	MCRConfiguration config = MCRConfiguration.instance();  
	String drivername = config.getString( prefix + "DriverName" );
	String dbvendor = config.getString( prefix + "Vendor" );
	String host = config.getString( prefix + "Hostname", "" );
	int port = config.getInt( prefix + "Port", 0 );
	String rootCollectionName = config.getString( prefix + "RootCollection" );
	try {
	    Class driverclass = Class.forName( drivername );
	    database = (Database)driverclass.newInstance();
	    DatabaseManager.registerDatabase( database );
	    
  	    String connString =  "xmldb:"
		+ dbvendor 
		+ "://" 
		+ host;
	    if( port != 0 )
		connString = connString + ":" + port;
	    connString = connString + "/" + rootCollectionName;
	    System.out.println( connString );
	    rootCollection = DatabaseManager.getCollection( connString );
	}
	catch( Exception e ) {
	    String msg = "Unable to initialize XMLDB database";
	    throw new MCRPersistenceException( msg, e );
	}
    }

    public String getID() { 
	return storeID; 
    }

    public String storeContent( MCRFile file, MCRContentInputStream source )
	throws MCRPersistenceException {
	try {
	    org.xmldb.api.base.Collection storageCollection = rootCollection.getChildCollection( file.getOwnerID() );
	    if( storageCollection == null ) {
		CollectionManagementService cmservice =
		    (CollectionManagementService)rootCollection.getService( "CollectionManagementService", 
									    "1.0" );

		cmservice.createCollection( file.getOwnerID() );
		storageCollection = rootCollection.getChildCollection( file.getOwnerID() );
	    }
	    if( storageCollection != null ) {
		BinaryResource res = 
		    (BinaryResource)storageCollection.createResource( file.getStorageID(),
								      BinaryResource.RESOURCE_TYPE );
		// Read content!
		//		res.setContent( content );
		storageCollection.storeResource( res );
		return res.getId();
	    }
	    else {
		return null;
	    }
	}
	catch( Exception e ) {
	    throw new MCRPersistenceException( e.getMessage(), e );
	}
    }

    public void deleteContent( MCRFile file )
	throws MCRPersistenceException {
	try {
	    org.xmldb.api.base.Collection storageCollection = rootCollection.getChildCollection( file.getOwnerID() );
	    Resource res = storageCollection.getResource( file.getStorageID() );
	    storageCollection.removeResource( res );
	}
	catch( Exception e ) {
	    throw new MCRPersistenceException( e.getMessage(), e );
	}	
    }

    public void doDeleteContent( String storageID ) throws MCRPersistenceException {
	try {
	    org.xmldb.api.base.Collection storageCollection = rootCollection.getChildCollection( storageID );
	    Resource res = storageCollection.getResource( storageID );
	    storageCollection.removeResource( res );
	}
	catch( Exception e ) {
	    throw new MCRPersistenceException( e.getMessage(), e );
	}	
    }

    public void retrieveContent( MCRFile file, OutputStream target )
	throws MCRPersistenceException {
	try {
	    org.xmldb.api.base.Collection storageCollection = rootCollection.getChildCollection( file.getOwnerID() );
	    Resource res = storageCollection.getResource( file.getStorageID() );
	    if( res.getContent() instanceof byte[] ) {
		target.write( (byte[])res.getContent() );
	    }
	}
	catch( Exception e ) {
	    throw new MCRPersistenceException( e.getMessage(), e );
	}		
    }
/*    
    protected void doDeleteContent(String storageID) throws Exception {
    }
*/    
    protected void doRetrieveContent(MCRFileReader file, OutputStream target) throws Exception {
      System.out.println("MCRXMLDBObjectStore method doRetrieveContent not implemented!!!!!"); 
    }
    
    protected String doStoreContent(MCRFileReader file, MCRContentInputStream source) throws Exception {
      System.out.println("MCRXMLDBObjectStore method doStoreContent not implemented!!!!!"); 
      return null;  
    }
    
}
