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

package mycore.sql;

import java.sql.*;
import java.util.*;
import mycore.common.*;

/**
 * This class implements a pool of database connections to a
 * relational database like DB2, using JDBC. Other classes get
 * a connection from the pool when they need one and release 
 * the connection after their work has finished.
 *
 * @see MCRSQLConnection
 *
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 *
 * @version $Revision$ $Date$
 */
public class MCRSQLConnectionPool
{
  /** The connection pool singleton */
  protected static MCRSQLConnectionPool singleton;

  /**
   * Returns the connection pool singleton.
   *
   * @throws MCRPersistenceException if the JDBC driver could not be loaded or initial connections could not be created
   **/
  public static MCRSQLConnectionPool instance()
  { 
    if( singleton == null ) singleton = new MCRSQLConnectionPool();
    return singleton; 
  }

  /** The internal list of free connections */
  protected Vector freeConnections = new Vector();
  
  /** The internal list of connections that are currently in use*/
  protected Vector usedConnections = new Vector() ;

  /** The maximum number of connections that will be built */
  protected int maxNumberOfConnections;

  /**
   * Builds the connection pool singleton.
   *
   * @throws MCRPersistenceException if the JDBC driver could not be loaded
   **/
  protected MCRSQLConnectionPool()
    throws MCRPersistenceException
  {
    System.out.println( "Building JDBC connection pool." );

    // The following line is a workaround for CM 7.1 under AIX
    // to prevent error FrnSysInitSharedMem to be thrown by CM:
    // Before connecting to DB2, ensure connect to CM is already done

    mycore.cm7.MCRCM7ConnectionPool.releaseConnection
      ( mycore.cm7.MCRCM7ConnectionPool.getConnection() );

  	MCRConfiguration config = MCRConfiguration.instance();
  	
    int initNumberOfConnections = config.getInt( "MCR.persistence_sql_init_connections", 1 );
        maxNumberOfConnections  = config.getInt( "MCR.persistence_sql_max_connections" );

    // Ok, we tolerate that the user is weak in math ;-)
    if( initNumberOfConnections > maxNumberOfConnections )
      maxNumberOfConnections = initNumberOfConnections;
        
    String driver = config.getString( "MCR.persistence_sql_driver" );
    
    // Load the JDBC driver
    try{ Class.forName( driver );  }
    catch( Exception exc )
    {
      throw new MCRPersistenceException
      ( "Could not find and load JDBC driver class " + driver, exc );
    }
    
    // Build the initial number of JDBC connections
    for( int i = 0; i < initNumberOfConnections; i++ )
      freeConnections.addElement( new MCRSQLConnection() );
  }

  /**
   * Gets a free connection from the pool. When this
   * connection is not used any more by the invoker, he is
   * responsible for returning it into the pool by invoking
   * the <code>release()</code> method of the connection.
   *
   * @see MCRSQLConnection#release()
   *
   * @return a free connection for your personal use
   * @throws MCRPersistenceException if there was a problem while building the connection
   **/
  public synchronized MCRSQLConnection getConnection()
    throws MCRPersistenceException
  {
  	// Wait for a free connection
    while( usedConnections.size() == maxNumberOfConnections )
      try{ wait(); } catch( InterruptedException ignored ){}

    MCRSQLConnection connection;
    
    // Do we have to build a connection or is there already one?
    if( freeConnections.isEmpty() ) 
      connection = new MCRSQLConnection();
    else
    {
      connection = (MCRSQLConnection)( freeConnections.firstElement() );
      freeConnections.removeElement( connection );
    }
    
    usedConnections.addElement( connection );
    return connection;
  }
  
  /**
   * Releases a connection, indicating that it is not used any more 
   * and should be returned to to pool of free connections. This
   * method is invoked when you call the method <code>release()</code> 
   * of the <code>MCRSQLConnection</code> object.
   *
   * @see MCRSQLConnection#release()
   *
   * @param connection the connection that has been used
   **/
  synchronized void releaseConnection( MCRSQLConnection connection )
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
        ( (Connection)( usedConnections.elementAt( i ) ) ).close();
      for( int i = 0; i < freeConnections.size(); i++ )
        ( (Connection)( freeConnections.elementAt( i ) ) ).close();
    }
    catch( Exception ignored ){}
  }
}
