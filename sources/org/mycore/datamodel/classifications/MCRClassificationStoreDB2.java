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

package mycore.classifications;

import java.util.Vector;
import mycore.common.*;
import mycore.sql.*;

public class MCRClassificationStoreDB2 implements MCRClassificationStore
{
  protected String tableClassif;
  protected String tableCateg;

  public MCRClassificationStoreDB2()
  { 
    MCRConfiguration config = MCRConfiguration.instance();
    tableClassif = config.getString( "MCR.classifications_store_db2_table_classifications" );
    tableCateg   = config.getString( "MCR.classifications_store_db2_table_categories" );

    if( ! tablesExist() ) createTables(); 
  }
  
  protected boolean tablesExist()
  {
    int number = MCRSQLConnection.justCountRows(
      "SYSCAT.TABLES WHERE TABNAME = '" + tableClassif +
      "' OR TABNAME = '" + tableCateg + "'" );
    return ( number == 2 );
  }

  protected void dropTables()
  {
    MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
    try
    { 
      c.doUpdate( "DROP TABLE " + tableClassif );
      c.doUpdate( "DROP TABLE " + tableCateg   );
    }
    finally{ c.release(); }
  }

  protected void createTables()
  {
    MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
    try
    { 
      c.doUpdate( new MCRSQLStatement( tableClassif )
        .addColumn( "ID VARCHAR(32) NOT NULL PRIMARY KEY" )
        .addColumn( "LABEL VARCHAR(250) NOT NULL" )
        .toCreateTableStatement() );
      c.doUpdate( new MCRSQLStatement( tableCateg )
        .addColumn( "ID VARCHAR(32) NOT NULL" )
        .addColumn( "CLID VARCHAR(32) NOT NULL" )
        .addColumn( "LABEL VARCHAR(250) NOT NULL" )
        .addColumn( "PID VARCHAR(32)" )
        .addColumn( "PRIMARY KEY ( CLID, ID )" )
        .addColumn( "CONSTRAINT CATEG2CLASSIF FOREIGN KEY ( CLID ) REFERENCES " + tableClassif + " ON DELETE CASCADE" )
        .addColumn( "CONSTRAINT CATEG2PARENT FOREIGN KEY ( CLID, PID ) REFERENCES " + tableCateg + " ON DELETE CASCADE" )
        .toCreateTableStatement() );
    }
    finally{ c.release(); }
  }
    
  public void createClassification( MCRClassification classification )
  {
    MCRSQLConnection.justDoUpdate( new MCRSQLStatement( tableClassif )
      .setValue( "ID",    classification.getID()    )
      .setValue( "LABEL", classification.getLabel() )
      .toInsertStatement() );
  }
  
  public void updateClassification( MCRClassification classification )
  {
    MCRSQLConnection.justDoUpdate( new MCRSQLStatement(  tableClassif )
      .setValue    ( "LABEL", classification.getLabel() )
      .setCondition( "ID",    classification.getID()    )
      .toUpdateStatement() );
  }
  
  public void createCategory( MCRCategory category )
  {
    MCRSQLConnection.justDoUpdate( new MCRSQLStatement( tableCateg )
      .setValue( "ID",    category.getID()               )
      .setValue( "LABEL", category.getLabel()            )
      .setValue( "CLID",  category.getClassificationID() )
      .setValue( "PID",   category.getParentID()         )
      .toInsertStatement() );
  }
    
  public void updateCategory( MCRCategory category )
  {
    MCRSQLConnection.justDoUpdate( new MCRSQLStatement( tableCateg )
      .setValue    ( "LABEL", category.getLabel()            )
      .setValue    ( "PID",   category.getParentID()         )
      .setCondition( "ID",    category.getID()               )
      .setCondition( "CLID",  category.getClassificationID() )
      .toUpdateStatement() );
  }

  public MCRClassification retrieveClassification( String ID )
  {
    MCRSQLRowReader reader = MCRSQLConnection.justDoQuery( 
      new MCRSQLStatement( tableClassif )
      .setCondition( "ID", ID )
      .toSelectStatement() );
    
    if( ! reader.next() ) return null;

    String label = reader.getString( "LABEL" );

    return new MCRClassification( ID, label, false );
  }
  
  public MCRCategory retrieveCategory( String classifID, String categID )
  {
    MCRSQLRowReader reader = MCRSQLConnection.justDoQuery( 
      new MCRSQLStatement( tableCateg )
      .setCondition( "ID",   categID   )
      .setCondition( "CLID", classifID )
      .toSelectStatement() );
    
    if( ! reader.next() ) return null;

    String label    = reader.getString( "LABEL" );
    String parentID = reader.getString( "PID"   );

    return new MCRCategory( categID, label, classifID, parentID );
  }
  
  public Vector retrieveChildren( String classifID, String parentID )
  {
    MCRSQLRowReader reader = MCRSQLConnection.justDoQuery( 
      new MCRSQLStatement( tableCateg )
      .setCondition( "PID",  parentID  )
      .setCondition( "CLID", classifID )
      .toSelectStatement() );
    
    Vector children = new Vector();
    
    while( reader.next() )
    {
      String ID    = reader.getString( "ID"    );
      String label = reader.getString( "LABEL" );
      
      MCRCategory child = new MCRCategory( ID, label, classifID, parentID );
      children.addElement( child );
    }

    return children;
  }
  
  public int retrieveNumberOfChildren( String classifID, String parentID )
  {
    return MCRSQLConnection.justCountRows( new MCRSQLStatement( tableCateg )
      .setCondition( "PID",  parentID  )
      .setCondition( "CLID", classifID )
      .toRowSelector() );
  }
  
  public boolean classificationExists( String classifID )
  {
    return MCRSQLConnection.justCheckExists( new MCRSQLStatement( tableClassif )
      .setCondition( "ID", classifID )
      .toRowSelector() );
  }
  
  public boolean categoryExists( String classifID, String categID )
  {
    return MCRSQLConnection.justCheckExists( new MCRSQLStatement( tableCateg )
      .setCondition( "ID",   categID   )
      .setCondition( "CLID", classifID )
      .toRowSelector() );
  }
  
  public void deleteClassification( String classifID )
  {
    MCRSQLConnection.justDoUpdate( new MCRSQLStatement( tableClassif )
      .setCondition( "ID", classifID )
      .toDeleteStatement() );
  }
  
  public void deleteCategory( String classifID, String categID )
  {
    MCRSQLConnection.justDoUpdate( new MCRSQLStatement( tableCateg )
      .setCondition( "CLID", classifID )
      .setCondition( "ID",   categID   )
      .toDeleteStatement() );
  }
}
