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

import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;

import org.xmldb.api.*;
import org.xmldb.api.base.*;
import org.xmldb.api.modules.*;


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
  protected Vector connections     = new Vector();
  protected Vector connectionsName = new Vector();
 
  /** The logger */
  private static Logger logger=Logger.getLogger("org.mycore.backend.XMLDB");

  private static String conf_prefix = "MCR.persistence_xmldb_";
  private static String driver      = "";
  private static String connString  = "";
//  private static String database    = "";
  private  Database database;
  
  /**
   * Returns the connection pool singleton.
   *
   * @throws MCRPersistenceException if connect to XMLDB was not successful
   **/
  public static synchronized MCRXMLDBConnectionPool instance()
    { 
    if( singleton == null ) singleton = new MCRXMLDBConnectionPool();
    return singleton; 
    }

  /**
   * Builds the connection pool singleton.
   *
   * @throws MCRPersistenceException if connect to XMLDB was not successful
   **/
  protected MCRXMLDBConnectionPool() throws MCRPersistenceException
    {
     MCRConfiguration config = MCRConfiguration.instance();
     PropertyConfigurator.configure(config.getLoggingProperties());
     logger.info( "Building connection to XML:DB..." );
     driver     = config.getString( conf_prefix + "driver" );
     logger.info("MCRXMLDBConnectionPool MCR.persistence_xmldb_driver      : " + driver); 
     connString = config.getString( conf_prefix + "database_url" , "");
     logger.info("MCRXMLDBConnectionPool MCR.persistence_xmldb_database_url: " + connString); 
     try
     {
      Class driverclass = Class.forName( driver );
      database = (Database)driverclass.newInstance();
      DatabaseManager.registerDatabase( database );
      Collection col = DatabaseManager.getCollection( connString );
      if( col == null ) 
      {
       int i = connString.lastIndexOf("/");
       if ( -1 != i )
       {
        String uri  = connString.substring(0,i); 
        String coll =  connString.substring(i+1);
        createCollection( uri, coll ); 
       }
      }
      else
       col.close();
     }
    catch( Exception e ) 
    {
     throw new MCRPersistenceException( e.getMessage(), e );
    }
   }
  
  /**
   * Creates a collection in an XML:DB collection
   *
   * @throws MCRPersistenceException if create fails
   **/
  private void createCollection( String uri, String collection ) throws MCRPersistenceException
  {
     try
     {
      Collection col;
   
      logger.info( "try to create collection in XML:DB: " + collection );
      Collection root = DatabaseManager.getCollection( uri );
      if ( null == root ) 
      {
       String msg = "MCRXMLDBConnectionPool: Could not connect to XML:DB: " + uri;
       throw new MCRPersistenceException( msg );
      }
   
      CollectionManagementService mgtService = 
                (CollectionManagementService)root.getService("CollectionManagementService", "1.0");
      col = mgtService.createCollection( collection );
      if ( null == col )
      {
       String msg = "MCRXMLDBConnectionPool: Could not create collection in XML:DB: " + collection;
       throw new MCRPersistenceException( msg );
      }
      else
      {    
       logger.info( "...done and successful" );
       col.close();   
      } 
     }
    catch( Exception e ) 
    {
     throw new MCRPersistenceException( e.getMessage(), e );
    }
  }
  
  /**
   * Creates a connection to an XML:DB collection
   *
   * @throws MCRPersistenceException if connect to XML:DB
   **/
  protected Collection buildConnection( String collection )  
    {
     Collection connection = null;
     String con = connString + "/" + collection;
     logger.info( "MCRXMLDBConnectionPool: Building connection to: " + con );
     try
     {
      connection = DatabaseManager.getCollection( con );
      if ( null == connection )
      {
       createCollection( connString, collection );    
       connection = DatabaseManager.getCollection( con );
      }
      return connection;
     }
     catch( Exception ex ) 
     {
      String msg = "MCRXMLDBConnectionPool: Could not connect to XML:DB: " + con;
      throw new MCRPersistenceException( msg, ex );
     }
    }
  
  /**
   * Gets a free connection from the pool. When this
   * connection is not used any more by the invoker, he is
   * responsible for returning it into the pool by invoking
   * the <code>releaseConnection()</code> method.
   *
   * @return a free connection to the Content Manager library server datastore
   * @throws MCRPersistenceException if there was a problem connecting to XML:DB
   **/
  public synchronized Collection getConnection( String collection )
    throws MCRPersistenceException
    {
     collection = collection.toLowerCase();   
     // Do we have to build a connection or is there already one?
     int i = connectionsName.indexOf( collection );
     if( i >= 0 )
       return (Collection)connections.elementAt( i );  
     
     Collection connection;
     try {
       connection = buildConnection( collection ); 
       connections.addElement( connection );
       connectionsName.addElement( collection );
     }
     catch( Exception e ) {
       throw new MCRPersistenceException( e.getMessage(), e );
     }
     
     if ( null == connection )
     {
      String msg = "MCRXMLDBConnectionPool: Collection not available: " + collection;
      throw new MCRPersistenceException( msg );
     }
     return connection;
    }

  /**
   * Releases a connection, indicating that it is not used any more 
   * and should be returned to to pool of free connections.
   *
   * @param connection the Content Manager connection that has been used
   **/
  public synchronized void releaseConnection( Collection connection )
    {
    if( connection == null ) return;
    try {
//      connection.close();
     }
     catch( Exception e ) {
       throw new MCRPersistenceException( e.getMessage(), e );
     }
/*    
    if( connections.contains( connection ) )
    {    
      connections.removeElement( connection );
    }  
 */
    }

  /**
   * Finalizer, closes all connections in this connection pool
   **/
  public void finalize()
    {
    try {
      for( int i = 0; i < connections.size(); i++ )
        ( (Collection)( connections.elementAt( i ) ) ).close();
      }
    catch( Exception ignored ){}
    }

  /**
   * The method return the logger for org.mycore.backend.xmldb.
   *
   * @return the logger.
   **/
  static final Logger getLogger()
    { return logger; }

  }

