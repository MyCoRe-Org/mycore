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

package org.mycore.backend.cm7;

import java.util.*;
import com.ibm.mm.sdk.server.*;
import com.ibm.mm.sdk.common.*;
import org.mycore.common.*;

/**
 * This class implements a pool of database connections to 
 * IBM Content Manager 7.1 Library Server. Other classes get
 * a connection from the pool when they need one and release 
 * the connection after their work has finished.
 *
 * @author Frank Luetzenkirchen
 * @author Jens Kupferschmidt
 *
 * @version $Revision$ $Date$
 **/
public class MCRCM7ConnectionPool implements DKConstant
{
  /** The connection pool singleton */
  protected static MCRCM7ConnectionPool singleton;

  /**
   * Returns the connection pool singleton.
   *
   * @throws MCRPersistenceException if connect to CM7 was not successful
   **/
  public static synchronized MCRCM7ConnectionPool instance()
  { 
    if( singleton == null ) singleton = new MCRCM7ConnectionPool();
    return singleton; 
  }

  /** The internal list of free connections */
  protected Vector freeConnections = new Vector();
  
  /** The internal list of connections that are currently in use*/
  protected Vector usedConnections = new Vector() ;

  /** The maximum number of connections that will be built */
  protected int maxNumConnections;

  /** The symbolic name of the CM 7.1 library server */
  protected String serverName;
  
  /** The user ID to be used for connecting to the library server */
  protected String uid;
  
  /** The password to be used for connecting to the library server */
  protected String password;
  
  /**
   * Builds the connection pool singleton.
   *
   * @throws MCRPersistenceException if connect to CM7 was not successful
   **/
  protected MCRCM7ConnectionPool()
    throws MCRPersistenceException
  {
    System.out.println( "Building Content Manager connection pool..." );
    
    MCRConfiguration config = MCRConfiguration.instance();
    
    serverName = config.getString( "MCR.persistence_cm7_library_server" );
    uid        = config.getString( "MCR.persistence_cm7_user_id"        );
    password   = config.getString( "MCR.persistence_cm7_password"       );

    maxNumConnections = config.getInt( "MCR.persistence_cm7_max_connections", 1 );
    int initNumConnections = config.getInt( "MCR.persistence_cm7_init_connections", maxNumConnections );

    // Build the initial number of JDBC connections
    for( int i = 0; i < initNumConnections; i++ )
      freeConnections.addElement( buildConnection() );
  }
  
  /**
   * Creates a DKDatastoreDL connection to the Content Manager library server.
   *
   * @throws MCRPersistenceException if connect to Content Manager fails
   **/
  protected DKDatastoreDL buildConnection()  
  {
    System.out.println( "Building connection to Content Manager..." );

    try 
    {
      DKDatastoreDL connection = new DKDatastoreDL();
      connection.connect( serverName, uid, password, "" );
      connection.setOption( DK_OPT_DL_WAKEUPSRV, new Integer( DK_FALSE ) );
      return connection;
    }
    catch( Exception exc ) 
    {
      String msg = "Could not connect to Content Manager library server";
      throw new MCRPersistenceException( msg, exc );
    }
  }
  
  /**
   * Gets a free connection from the pool. When this
   * connection is not used any more by the invoker, he is
   * responsible for returning it into the pool by invoking
   * the <code>releaseConnection()</code> method.
   *
   * @return a free connection to the Content Manager library server datastore
   * @throws MCRPersistenceException if there was a problem connecting to CM
   **/
  public synchronized DKDatastoreDL getConnection()
    throws MCRPersistenceException
  {
    // Wait for a free connection
    while( usedConnections.size() == maxNumConnections )
    try{ wait(); } catch( InterruptedException ignored ){}

    DKDatastoreDL connection;
    
    // Do we have to build a connection or is there already one?
    if( freeConnections.isEmpty() ) 
      connection = buildConnection();
    else
    {
      connection = (DKDatastoreDL)( freeConnections.firstElement() );
      freeConnections.removeElement( connection );
    }
    
    usedConnections.addElement( connection );
    return connection;
  }

  /**
   * Releases a connection, indicating that it is not used any more 
   * and should be returned to to pool of free connections.
   *
   * @param connection the Content Manager connection that has been used
   **/
  public synchronized void releaseConnection( DKDatastoreDL connection )
  {
    if( connection == null ) return;
    
    if( usedConnections.contains( connection ) )
      usedConnections.removeElement( connection );
    if( ! freeConnections.contains( connection ) ) 
      freeConnections.addElement( connection );
    
    notifyAll();
  }

  /**
   * Finalizer, closes all connections in this connection pool
   **/
  public void finalize()
  {
    try
    {
      for( int i = 0; i < usedConnections.size(); i++ )
        ( (DKDatastoreDL)( usedConnections.elementAt( i ) ) ).disconnect();
      for( int i = 0; i < freeConnections.size(); i++ )
        ( (DKDatastoreDL)( freeConnections.elementAt( i ) ) ).disconnect();
    }
    catch( Exception ignored ){}
  }
}

