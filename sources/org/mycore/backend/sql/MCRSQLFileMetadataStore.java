/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  *** 
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
 
package org.mycore.backend.sql;

import org.mycore.common.*;
import org.mycore.datamodel.ifs.*;
import org.mycore.datamodel.metadata.*;

import java.sql.*;
import java.util.Vector;
import java.util.GregorianCalendar;

public class MCRSQLFileMetadataStore implements MCRFileMetadataStore
{
  protected String table;
  
  public MCRSQLFileMetadataStore()
    throws MCRPersistenceException
  { 
    MCRConfiguration config = MCRConfiguration.instance();
    table = config.getString( "MCR.IFS.FileMetadataStore.SQL.TableName" );
    if( ! MCRSQLConnection.doesTableExist(table) ) createTable(); 
  }
  
  private void dropTable()
    throws MCRPersistenceException
  { MCRSQLConnection.justDoUpdate( "DROP TABLE " + table ); }

  private void createTable()
  {
    MCRSQLConnection.justDoUpdate( new MCRSQLStatement( table )
      .addColumn( "ID CHAR(16) NOT NULL PRIMARY KEY" )
      .addColumn( "PID CHAR(16)" )
      .addColumn( "TYPE CHAR(1) NOT NULL" )
      .addColumn( "OWNER VARCHAR("+Integer.toString(MCRObjectID.MAX_LENGTH)+
        ") NOT NULL" )
      .addColumn( "NAME VARCHAR(250) NOT NULL" )
      .addColumn( "LABEL VARCHAR(250)" )
      .addColumn( "SIZE BIGINT NOT NULL" )
      .addColumn( "DATE TIMESTAMP NOT NULL" )
      .addColumn( "STOREID VARCHAR(32)" )
      .addColumn( "STORAGEID VARCHAR(250)" )
      .addColumn( "FCTID VARCHAR(32)" )
      .addColumn( "MD5 CHAR(32)" )
      .addColumn( "NUMCHDD INTEGER" ) // direct directories
      .addColumn( "NUMCHDF INTEGER" ) // direct files
      .addColumn( "NUMCHTD INTEGER" ) // total directories
      .addColumn( "NUMCHTF INTEGER" ) // total files
      .toCreateTableStatement() 
    );
  }
    
  public void storeNode( MCRFilesystemNode node )
    throws MCRPersistenceException
  {
    deleteNode( node.getID() );
    
    String ID    = node.getID();
    String PID   = node.getParentID();
    String OWNER = node.getOwnerID();
    String NAME  = node.getName();
    String LABEL = node.getLabel();
    long   SIZE  = node.getSize();

    GregorianCalendar DATE = node.getLastModified();  
 
    String TYPE       = null;
    String STOREID    = null;
    String STORAGEID  = null;
    String FCTID      = null;
    String MD5        = null;
    
    int NUMCHDD = 0, NUMCHDF = 0, NUMCHTD = 0, NUMCHTF = 0;

    if( node instanceof MCRFile )
    {
      MCRFile file = (MCRFile)node;

      TYPE      = "F";
      STOREID   = file.getStoreID();
      STORAGEID = file.getStorageID();
      FCTID     = file.getContentTypeID();
      MD5       = file.getMD5();
    }
    else
    {
      MCRDirectory dir = (MCRDirectory)node;
      
      TYPE    = "D";
      NUMCHDD = dir.getNumChildren( MCRDirectory.DIRECTORIES, MCRDirectory.HERE  );
      NUMCHDF = dir.getNumChildren( MCRDirectory.FILES,       MCRDirectory.HERE  );
      NUMCHTD = dir.getNumChildren( MCRDirectory.DIRECTORIES, MCRDirectory.TOTAL );
      NUMCHTF = dir.getNumChildren( MCRDirectory.FILES,       MCRDirectory.TOTAL );
    }
  
    MCRSQLConnection connection =
      MCRSQLConnectionPool.instance().getConnection();

    try
    {
      String insert = "INSERT INTO " + table + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      PreparedStatement statement = connection.getJDBCConnection().prepareStatement( insert );

      statement.setString   ( 1,  ID    );
      statement.setString   ( 2,  PID   );
      statement.setString   ( 3,  TYPE  );
      statement.setString   ( 4,  OWNER );
      statement.setString   ( 5,  NAME  );
      statement.setString   ( 6,  LABEL );
      statement.setLong     ( 7,  SIZE  );
      statement.setTimestamp( 8,  new Timestamp( DATE.getTime().getTime() ) );
      statement.setString   ( 9,  STOREID   );
      statement.setString   ( 10, STORAGEID );
      statement.setString   ( 11, FCTID     );
      statement.setString   ( 12, MD5       );
      statement.setInt      ( 13, NUMCHDD   ); 
      statement.setInt      ( 14, NUMCHDF   ); 
      statement.setInt      ( 15, NUMCHTD   ); 
      statement.setInt      ( 16, NUMCHTF   ); 

      statement.execute();
      statement.close();
    }
    catch( Exception sql )
    {
      String msg = "Error while inserting IFS node metadata into SQL table";
       throw new MCRPersistenceException( msg, sql );
    }
    finally{ connection.release(); }
  }
  
  public MCRFilesystemNode retrieveNode( String ID )
    throws MCRPersistenceException
  {
    MCRSQLRowReader reader = MCRSQLConnection.justDoQuery( 
      new MCRSQLStatement( table )
      .setCondition( "ID", ID )
      .toSelectStatement() );
    
    if( reader.next() ) 
      return buildNodeFromReader( reader );
    else 
      return null;
  }

  public MCRFilesystemNode buildNodeFromReader( MCRSQLRowReader reader )
  {
    String objectType      = reader.getString( "TYPE"      );
    String ID              = reader.getString( "ID"        );
    String parentID        = reader.getString( "PID"       );
    String ownerID         = reader.getString( "OWNER"     );
    String name            = reader.getString( "NAME"      );
    String label           = reader.getString( "LABEL"     );
    long size              = reader.getLong  ( "SIZE"      );
    GregorianCalendar date = reader.getDate  ( "DATE"      );
    String storeID         = reader.getString( "STOREID"   );
    String storageID       = reader.getString( "STORAGEID" );
    String fctID           = reader.getString( "FCTID"     );
    String md5             = reader.getString( "MD5"       );
    int numchdd            = reader.getInt   ( "NUMCHDD"   );
    int numchdf            = reader.getInt   ( "NUMCHDF"   );
    int numchtd            = reader.getInt   ( "NUMCHTD"   );
    int numchtf            = reader.getInt   ( "NUMCHTF"   );
    
    return MCRFileMetadataManager.instance().buildNode( objectType, 
      ID, parentID, ownerID, name, label, size, date, 
      storeID, storageID, fctID, md5, numchdd, numchdf, numchtd, numchtf );
  }
  
  public String retrieveRootNodeID( String ownerID )
    throws MCRPersistenceException
  {
    MCRSQLRowReader reader = MCRSQLConnection.justDoQuery( 
      new MCRSQLStatement( table )
      .setCondition( "PID",   null    )
      .setCondition( "OWNER", ownerID )
      .toSelectStatement() );

    return ( reader.next() ? reader.getString( "ID" ) : null );
  }
  
  public MCRFilesystemNode retrieveChild( String parentID, String name )
  {
    MCRSQLRowReader reader = MCRSQLConnection.justDoQuery( 
      new MCRSQLStatement( table )
      .setCondition( "PID",  parentID )
      .setCondition( "NAME", name     )
      .toSelectStatement() );
    
    if( reader.next() ) 
      return buildNodeFromReader( reader );
    else
      return null;
  }
  
  public Vector retrieveChildrenIDs( String parentID )
    throws MCRPersistenceException
  {
    MCRSQLRowReader reader = MCRSQLConnection.justDoQuery( 
      new MCRSQLStatement( table )
      .setCondition( "PID", parentID )
      .toSelectStatement() );
    
    Vector childrenIDs = new Vector();
    while( reader.next() ) 
      childrenIDs.addElement( reader.getString( "ID" ) );
    
    return childrenIDs;
  }
  
  public void deleteNode( String ID )
    throws MCRPersistenceException
  {
    MCRSQLConnection.justDoUpdate( new MCRSQLStatement( table )
      .setCondition( "ID", ID )
      .toDeleteStatement() );
  }
}


