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

import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRPersistenceException;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;


/**
 * This class implements a pool of connections to XML:DB
 * Other classes get a connection from the pool when they 
 * need one and release the connection after their work 
 * has finished.
 *
 * @author Harald Richter
 *
 * @version $Revision$ $Date$
 **/
public class MCRXMLDBConnectionPool
  {
  /** The connection pool singleton */
  protected static MCRXMLDBConnectionPool singleton;

  /** The internal list of connections */
  protected Hashtable connections = new Hashtable();
 
  /** The logger */
  private static Logger logger=Logger.getLogger("org.mycore.backend.XMLDB");

  private static String conf_prefix = "MCR.persistence_xmldb_";
  private static String driver      = "";
  private static String connString  = "";
  private static String user;
  private static String passwd;
  private  Database database;
  
  /**
   * Returns the connection pool singleton.
   * 
   * @throws MCRPersistenceException
   *             if connect to XMLDB was not successful
   */
	public static synchronized MCRXMLDBConnectionPool instance() {
		if (singleton == null)
			singleton = new MCRXMLDBConnectionPool();
		return singleton;
	}

  /**
   * Builds the connection pool singleton.
   * 
   * @throws MCRPersistenceException
   *             if connect to XMLDB was not successful
   */
	protected MCRXMLDBConnectionPool() throws MCRPersistenceException {
		MCRConfiguration config = MCRConfiguration.instance();
		PropertyConfigurator.configure(config.getLoggingProperties());
		logger.info("Building connection to XML:DB...");
		driver = config.getString(conf_prefix + "driver");
		logger
				.debug("MCRXMLDBConnectionPool MCR.persistence_xmldb_driver      : "
						+ driver);
		connString = config.getString(conf_prefix + "database_url", "");
		logger
				.debug("MCRXMLDBConnectionPool MCR.persistence_xmldb_database_url: "
						+ connString);
		user = config.getString(conf_prefix + "user", null);
		logger
				.debug("MCRXMLDBConnectionPool MCR.persistence_xmldb_user      : "
						+ user);
		passwd = config.getString(conf_prefix + "passwd", null);
		try {
			Class driverclass = Class.forName(driver);
			database = (Database) driverclass.newInstance();
			DatabaseManager.registerDatabase(database);

			// try to create database
			if (config.getString(conf_prefix + "database_create", "true")
					.equals("true")) {
				Collection col = DatabaseManager.getCollection(connString);
				int i = connString.lastIndexOf("/");
				String uri,collname;
				if (i != -1) {
					uri = connString.substring(0, i);
					collname = connString.substring(i + 1);
					if (col==null)
						createCollection(uri, collname);
					else
						connections.put(collname,col);
				}
			}
		} catch (Exception e) {
			throw new MCRPersistenceException(e.getMessage(), e);
		}
	}
  
  /**
   * Creates a collection in an XML:DB collection
   * 
   * @throws MCRPersistenceException
   *             if create fails
   */
	private void createCollection(String uri, String collection)
			throws MCRPersistenceException {
		try {
			Collection col;

			logger.info("try to create collection in XML:DB: " + collection);
			Collection root = getCollection(uri);
			if (root==null) {
				String msg = "MCRXMLDBConnectionPool: Could not connect to XML:DB: "
						+ uri;
				throw new MCRPersistenceException(msg);
			}

			CollectionManagementService mgtService = (CollectionManagementService) root
					.getService("CollectionManagementService", "1.0");
			col = mgtService.createCollection(collection);
			if (col==null) {
				String msg = "MCRXMLDBConnectionPool: Could not create collection in XML:DB: "
						+ collection;
				throw new MCRPersistenceException(msg);
			}
			logger.info("...done and successful");
			synchronized(connections){
				connections.put(collection,col);
			}
		} catch (Exception e) {
			throw new MCRPersistenceException(e.getMessage(), e);
		}
	}
  
  /**
   * Creates a connection to an XML:DB collection
   * 
   * @throws XMLDBException
   */
	protected Collection buildConnection(String collection)
			throws XMLDBException {
		String con = connString + "/" + collection;
		logger.debug("MCRXMLDBConnectionPool: Building connection to: " + con);
		Collection connection = getCollection(con);
		if (connection == null) {
			createCollection(connString, collection);
			connection = getCollection(con);
		}
		return connection;
	}
  
  /**
   * returns a collection for a user account if available
 * @param collection
 * @return 
 * @throws XMLDBException
 */
private Collection getCollection(String collection) throws XMLDBException {
	Collection connection;
	if (user==null)
		connection = DatabaseManager.getCollection(collection);
	else
		connection = DatabaseManager.getCollection(collection, user, passwd);
	return connection;
}

/**
   * Gets a free connection from the pool. When this connection is not used any
   * more by the invoker, he is responsible for returning it into the pool by
   * invoking the <code>releaseConnection()</code> method.
   * 
   * @return a free connection to the Content Manager library server datastore
   * @throws MCRPersistenceException
   *             if there was a problem connecting to XML:DB
   */
  public Collection getConnection(String collection)
    throws MCRPersistenceException{
  	// Do we have to build a connection or is there already one?
    if(connections.containsKey(collection))
    	return (Collection)connections.get(collection);  
    
    Collection connection;
    try {
    	connection = buildConnection(collection);
    	synchronized(connections){
    		connections.put(collection, connection);
    	}
    	if (null == connection){
    		String msg = "MCRXMLDBConnectionPool: Collection not available: " + collection;
    		throw new NullPointerException(msg);
    	}
    }
    catch( Exception ex ) 
		{
    	String msg = "MCRXMLDBConnectionPool: Could not connect to XML:DB: " + collection;
    	throw new MCRPersistenceException( msg, ex );
		}
    return connection;
  }

  /**
   * Finalizer, closes all connections in this connection pool
   */
  public void finalize(){
  	try {
  		Enumeration enum = connections.elements();
  		while(enum.hasMoreElements())
  			((Collection)enum.nextElement()).close();
  	}
  	catch(Exception ignored){}
  }

  /**
   * The method return the logger for org.mycore.backend.xmldb.
   * 
   * @return the logger.
   */
  static final Logger getLogger(){
  	return logger;
  }

}

