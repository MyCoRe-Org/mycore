package org.mycore.backend.xmldb;

import org.xmldb.api.*;
import org.xmldb.api.base.*;
import org.xmldb.api.modules.*;

public final class MCRXMLDBTools {
    /**
     * This method returns an XML:DB conform connection string of the
     * form:
     * <code>xmldb:vendor://host:port/collectionpath</code>. It is
     * built from the configuration file mycore.properties. Therefore
     * we need to set:<br/>
     * <code>
     * MCR.persistence_xmldb_vendor<br/>
     * MCR.persistence_xmldb_hostname<br/>
     * MCR.persistence_xmldb_port<br/>
     * MCR.persistence_xmldb_root<br/>
     * </code>
     * The properties <code>MCR.persistence_xmldb_hostname</code> and
     * <code>MCR.persistence_xmldb_port</code> are optional. If they
     * are not given, the XML:DB database <strong>must</strong> run on
     * the same server as mycore itself.
     *
     * @return the connection string
     **/
    public static String buildConnectString( String vendor,
					     String hostname,
					     int port,
					     String root ) {
	String connString = "xmldb:"
	    + vendor
	    + "://"
	    + hostname;
	if( port != 0 )
	    connString = connString + ":" + port;
	connString = connString
	    + root;
	return connString;
    }

    /**
     * This method returns the root collection of a given connection.
     *
     * @param driver name of the XML:DB driver
     * @param connection the connection string
     *
     * @throws ClassNotFoundException if driver class could not be found
     * @throws IllegalAccessException if you have no access to database
     * @throws InstantiationException if driver class could not be instantiated
     * @throws XMLDBException if an XML:DB problem occurs
     **/
    public static Collection getRootCollection( String driver,
						String connection )
	throws ClassNotFoundException,
	       IllegalAccessException,
	       InstantiationException,
	       XMLDBException {
	Class driverclass = Class.forName( driver );
	Database database = (Database)driverclass.newInstance();
	DatabaseManager.registerDatabase( database );
	return DatabaseManager.getCollection( connection );
    }

    /**
     * Closes all child collections of the given collection
     * recursively and the collection itself.
     *
     * @param collection the collection whose child collections shall
     * be closed
     *
     * @throws XMLDBException a XMLDBException
     **/
    public static void safelyClose( Collection collection )
    throws XMLDBException {
  	    if( collection != null && collection.isOpen() )
		collection.close();
    }

    /**
     * Looks under given collection if a collection with the given
     * name exists. If not the collection will be generated if create
     * parameter is true.
     * @param parent the collection to look under
     * @param name collection name to look for
     * @param create creates collection if it is not there
     * @return the collection found or the newly created one, null if
     * collection was not found and should not be created
     **/
    public static Collection collectionExistsUnder( Collection parent, String name, boolean create ) throws XMLDBException {
	Collection new_coll;
	if( (new_coll = parent.getChildCollection( name ) ) == null && create ) {
	    CollectionManagementService cms = 
		(CollectionManagementService) parent.getService(
								"CollectionManagementService",
								"1.0" );
	    new_coll = cms.createCollection( name );
	}
	return new_coll;
    }
}
