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
 * This class provides methods to get the names of the tables and columns
 * that correspond to Content Manager 7 index classes and keyfields. 
 * This is needed by functions that bypass the CM/EIP API to directly 
 * read index class contents via a DB2 JDBC connection.
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRCM7Bypass
{
  /** Stores the table names that have been looked up so far */
  protected static Properties tables = new Properties();

  /** Stores the column names that have been looked up so far */
  protected static Properties columns = new Properties();

  /** The user ID where the Content Manager DB2 tables are stored */
  protected static String owner = 
    MCRConfiguration.instance().getString( "MCR.persistence_cm7_db2_user_id" ); 

  /**
   * Returns the name of the DB2 table that is equivalent to
   * the Content Manager index class with the given name.
   *
   * @param indexClassName the name of the Content Manager index class
   * @return the name of the DB2 table
   **/
  public static String getTableName( String indexClassName )
  {
    MCRArgumentChecker.ensureNotEmpty( indexClassName, "indexClassName" );

    if( ! tables.containsKey( indexClassName ) )
    {
      int code = getKeywordCode( indexClassName, "3" );

      String query = "SELECT CLASSTABLENAME FROM " + owner +
                     ".SBTCLASSDEFS WHERE CLASSID = " + code;

      String name = null;
      Connection conn = MCRCM7ConnectionPoolDB2.getConnection();

      try
      {
        Statement stmt = conn.createStatement();
        ResultSet rs   = stmt.executeQuery( query );

        if( rs.next() ) name = rs.getString( "CLASSTABLENAME" );

        rs.close();
        stmt.close();
      }
      catch( Exception ex )
      { 
        throw new MCRPersistenceException 
        ( "Error while retrieving DB2 table name for index class " + indexClassName, ex ); 
      }
      finally
      { MCRCM7ConnectionPoolDB2.releaseConnection( conn ); }

      if( name == null )
      {
        throw new MCRConfigurationException
        ( "Could not find DB2 table name entry for index class " + indexClassName );
      }
      else tables.put( indexClassName, owner + "." + name.trim() );
    }
    return tables.getProperty( indexClassName );
  }

  /**
   * Returns the name of the DB2 table column that is equivalent to
   * the Content Manager keyfield with the given name.
   *
   * @param keyFieldName the name of the Content Manager keyfield
   * @return the name of the DB2 table column
   **/
  public static String getColumnName( String keyFieldName )
  {
    if( ! columns.containsKey( keyFieldName ) )
    {
      int code = getKeywordCode( keyFieldName, "1" );
      String sCode  = String.valueOf( code );
      String column = "ATTRIBUTE00000".substring( 0, 14 - sCode.length() ) + sCode;
      columns.put( keyFieldName, column );
    }
    return columns.getProperty( keyFieldName );
  }

  /**
   * Returns the keyword code used by Content Manager for a given
   * keyfield or index class.
   *
   * @param keywordClass the type of keyword ( "1" = keyfield, "3" = index class ) 
   * @param fieldName the name of the Content Manager index class or keyfield
   * @return the keyword code used by Content Manager
   **/
  protected static int getKeywordCode( String fieldName, String keywordClass )
  {
    String query = "SELECT KEYWORDCODE FROM " + owner + ".SBTNLSKEYWORDS WHERE " +
                   "KEYWORDSTRING LIKE '" + fieldName + "' AND " +
                   "KEYWORDCLASSFI = " + keywordClass;
 
    int keywordCode = -1;
    Connection conn = MCRCM7ConnectionPoolDB2.getConnection();

    try
    {
      Statement stmt = conn.createStatement();
      ResultSet rs   = stmt.executeQuery( query );

      if( rs.next() ) keywordCode = rs.getInt( "KEYWORDCODE" );

      rs.close();
      stmt.close();
    }
    catch( Exception ex )
    {
      throw new MCRPersistenceException
      ( "Error while retrieving keyword code from SBTNLSKEYWORDS table for field " + fieldName, ex );
    }
    finally
    { MCRCM7ConnectionPoolDB2.releaseConnection( conn ); }

    if( keywordCode == -1 )
    {
      throw new MCRConfigurationException
      ( "Could not find keyword code in SBTNLSKEYWORDS for field " + fieldName );
    }
    else return keywordCode;
  }
}
