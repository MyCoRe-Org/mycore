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

package mycore.cm7;

import java.util.*;
import java.sql.*;
import mycore.common.*;

/**
 * This class implements a pool of database connections to DB2. Other 
 * classes get a connection from the pool when they need one and release 
 * the connection after their work has finished.
 *
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRCM7ConnectionPoolDB2
{
  protected static Vector freeConnections;
  protected static Vector usedConnections;
  protected static int    maxConnections;

  static
  {
    try
    {
      // The following line is a workaround for CM 7.1 under AIX
      // to prevent error FrnSysInitSharedMem to be thrown:
      // Before connecting to DB2, ensure connect to CM is already done

      MCRCM7ConnectionPool.
         releaseConnection( MCRCM7ConnectionPool.getConnection() );

      // Rest is business as usual...

      maxConnections  = MCRConfiguration.instance()
        .getInt("MCR.persistence_cm7_db2_max_connections",1);
      freeConnections = new Vector( maxConnections );
      usedConnections = new Vector( maxConnections );

      System.out.print( "Connecting to DB2: " );
      Class.forName( "COM.ibm.db2.jdbc.app.DB2Driver" );

      for( int i = 0; i < maxConnections; i++ )
      {
        Connection connection = DriverManager.getConnection( "jdbc:db2:" + 
          MCRConfiguration.instance().getString(
          "MCR.persistence_cm7_library_server") );
        freeConnections.addElement( connection );
        System.out.print( "#" );
      }
      System.out.println( " created " + maxConnections + " connections" );
    }
    catch( Exception ex )
    { System.out.println( ex.getClass().getName() + ":" + ex.getMessage() ); }
  }
  
  public static synchronized Connection getConnection()
  {
    if( usedConnections.size() == maxConnections )
    {
      try{ MCRCM7ConnectionPoolDB2.class.wait(); }
      catch( InterruptedException ex ){}
      return getConnection();
    }
    else
    {
      Connection connection = (Connection)( freeConnections.firstElement() );
      freeConnections.removeElement( connection );
      usedConnections.addElement( connection );
      return connection;
    }
  }

  public static synchronized void releaseConnection( Connection connection )
  {
    if(    ( ! freeConnections.contains( connection ) )
        && (   usedConnections.contains( connection ) ) )
    {
      usedConnections.removeElement( connection );
      freeConnections.addElement   ( connection );
      MCRCM7ConnectionPoolDB2.class.notifyAll();
    }
  }
}
