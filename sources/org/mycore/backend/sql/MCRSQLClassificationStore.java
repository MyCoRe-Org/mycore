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

package mycore.sql;

import java.util.Vector;
import mycore.common.*;
import mycore.datamodel.*;

/** 
 * This class implements the MCRClassificationInterface as presistence
 * layer for a SQL database.
 *
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRSQLClassificationStore implements MCRClassificationInterface
{
  protected String tableClassif;
  protected String tableCateg;

/**
 * The constructor for the class MCRSQLClassificationStore. It reads 
 * the classification configuration and checks the table names.
 **/
public MCRSQLClassificationStore()
  { 
  MCRConfiguration config = MCRConfiguration.instance();
  tableClassif = config.getString( 
    "MCR.classifications_store_sql_table_classifications" );
  tableCateg   = config.getString( 
    "MCR.classifications_store_sql_table_categories" );
  if( ! tablesExist() ) createTables(); 
  }
  
/**
 * A private mothod to check the exist of a SQL table.
 *
 * @return true if both tables exist.
 **/
private final boolean tablesExist()
  {
  int number = MCRSQLConnection.justCountRows(
    "SYSCAT.TABLES WHERE TABNAME = '" + tableClassif +
    "' OR TABNAME = '" + tableCateg + "'" );
  return ( number == 2 );
  }

/**
 * The method drop the classification and category tables.
 **/
private final void dropTables()
  {
  MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
  try { 
    c.doUpdate( "DROP TABLE " + tableClassif );
    c.doUpdate( "DROP TABLE " + tableCateg   );
    }
  finally{ c.release(); }
  }

/**
 * The method create the classification and category tables.
 **/
private final void createTables()
  {
  MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
  try { 
    // the classification table
    c.doUpdate( new MCRSQLStatement( tableClassif )
     .addColumn( "ID VARCHAR(32) NOT NULL PRIMARY KEY" )
     .addColumn( "TAG VARCHAR(500) NOT NULL" )
     .toCreateTableStatement() );
    // the category table
    c.doUpdate( new MCRSQLStatement( tableCateg )
     .addColumn( "ID VARCHAR(32) NOT NULL" )
     .addColumn( "CLID VARCHAR(32) NOT NULL" )
     .addColumn( "TAG VARCHAR(500) NOT NULL" )
     .addColumn( "PID VARCHAR(32)" )
     .addColumn( "PRIMARY KEY ( CLID, ID )" )
     .addColumn( "CONSTRAINT CATEG2CLASSIF FOREIGN KEY ( CLID ) REFERENCES " +
       tableClassif + " ON DELETE CASCADE" )
     .addColumn( "CONSTRAINT CATEG2PARENT FOREIGN KEY ( CLID, PID ) REFERENCES "
       + tableCateg + " ON DELETE CASCADE" )
     .toCreateTableStatement() );
    }
  finally{ c.release(); }
  }
    
/**
 * The method create a new MCRClassificationItem in the datastore.
 *
 * @param classification an instance of a MCRClassificationItem
 **/
public final void createClassificationItem( MCRClassificationItem 
  classification )
  {
  MCRSQLConnection.justDoUpdate( new MCRSQLStatement( tableClassif )
    .setValue( "ID",    classification.getID()    )
    .setValue( "TAG",   classification.getTag() )
    .toInsertStatement() );
  }
  
/**
 * The method update a MCRClassificationItem in the datastore.
 *
 * @param classification an instance of a MCRClassificationItem
 **/
public void updateClassificationItem( MCRClassificationItem classification )
  {
  for (int i=0;i<classification.getSize();i++) {
    MCRSQLConnection.justDoUpdate( new MCRSQLStatement(  tableClassif )
      .setValue( "TAG", classification.getTag() )
      .setCondition( "ID",    classification.getID()    )
      .toUpdateStatement() );
    }
  }
  
/**
 * The method remove a MCRClassificationItem from the datastore.
 *
 * @param classification an instance of a MCRClassificationItem
 **/
public void deleteClassificationItem( String classifID )
  {
  MCRSQLConnection.justDoUpdate( new MCRSQLStatement( tableClassif )
    .setCondition( "ID", classifID )
    .toDeleteStatement() );
  }
  
/**
 * The method return a MCRClassificationItem from the datastore.
 *
 * @param classifID the ID of the MCRClassificationItem
 **/
public final MCRClassificationItem retrieveClassificationItem( String ID )
  {
  MCRSQLRowReader reader = MCRSQLConnection.justDoQuery( 
    new MCRSQLStatement( tableClassif )
    .setCondition( "ID", ID )
    .toSelectStatement() );
  if( ! reader.next() ) return null;
  MCRClassificationItem c = new MCRClassificationItem( ID );
  String tag = reader.getString( "TAG" );
  c.setTag( tag );
  return c;
  }
  
/**
 * The method return if the MCRClassificationItem is in the datastore.
 *
 * @param classifID the ID of the MCRClassificationItem
 * @return true if the MCRClassificationItem was found, else false
 **/
public final boolean classificationItemExists( String classifID )
  {
  return MCRSQLConnection.justCheckExists( new MCRSQLStatement( tableClassif )
    .setCondition( "ID", classifID )
    .toRowSelector() );
  }

/**
 * The method create a new MCRCategoryItem in the datastore.
 *
 * @param category an instance of a MCRCategoryItem
 **/
public final void createCategoryItem( MCRCategoryItem category )
  {
  MCRSQLConnection.justDoUpdate( new MCRSQLStatement( tableCateg )
    .setValue( "ID",    category.getID()               )
    .setValue( "TAG",   category.getTag()              )
    .setValue( "CLID",  category.getClassificationID() )
    .setValue( "PID",   category.getParentID()         )
    .toInsertStatement() );
  }
    
/**
 * The method update a MCRCategoryItem in the datastore.
 *
 * @param category an instance of a MCRCategoryItem
 **/
public final void updateCategoryItem( MCRCategoryItem category )
  {
  MCRSQLConnection.justDoUpdate( new MCRSQLStatement( tableCateg )
    .setValue( "TAG", category.getTag()            )
    .setValue( "PID",   category.getParentID()         )
    .setCondition( "ID",    category.getID()               )
    .setCondition( "CLID",  category.getClassificationID() )
    .toUpdateStatement() );
  }

/**
 * The method remove a MCRCategoryItem from the datastore.
 *
 * @param classifID the ID of the MCRClassificationItem
 * @param categID the ID of the MCRCategoryItem
 **/
public final void deleteCategoryItem( String classifID, String categID )
  {
  MCRSQLConnection.justDoUpdate( new MCRSQLStatement( tableCateg )
    .setCondition( "CLID", classifID )
    .setCondition( "ID",   categID   )
    .toDeleteStatement() );
  }
  
/**
 * The method return a MCRCategoryItem from the datastore.
 *
 * @param classifID the ID of the MCRClassificationItem
 * @param categID the ID of the MCRCategoryItem
 **/
public final MCRCategoryItem retrieveCategoryItem( String classifID, 
  String categID )
  {
  MCRSQLRowReader reader = MCRSQLConnection.justDoQuery( 
    new MCRSQLStatement( tableCateg )
    .setCondition( "ID",   categID   )
    .setCondition( "CLID", classifID )
    .toSelectStatement() );
  if( ! reader.next() ) return null;
  String parentID = reader.getString( "PID"   );
  MCRCategoryItem c = new MCRCategoryItem( categID, classifID, parentID );
  String tag = reader.getString( "TAG" );
  c.setTag( tag );
  return c;
  }
  
/**
 * The method return if the MCRCategoryItem is in the datastore.
 *
 * @param classifID the ID of the MCRClassificationItem
 * @param categID the ID of the MCRCategoryItem
 * @return true if the MCRCategoryItem was found, else false
 **/
public final boolean categoryItemExists( String classifID, String categID )
  {
  return MCRSQLConnection.justCheckExists( new MCRSQLStatement( tableCateg )
    .setCondition( "ID",   categID   )
    .setCondition( "CLID", classifID )
    .toRowSelector() );
  }

/**
 * The method return an Vector of MCRCategoryItems from the datastore.
 *
 * @param classifID the ID of the MCRClassificationItem
 * @param categID the ID of the MCRCategoryItem
 * @return a list of MCRCategoryItem children
 **/
public final Vector retrieveChildren( String classifID, String parentID )
  {
  MCRSQLRowReader reader = MCRSQLConnection.justDoQuery( 
    new MCRSQLStatement( tableCateg )
    .setCondition( "PID",  parentID  )
    .setCondition( "CLID", classifID )
    .toSelectStatement() );
  Vector children = new Vector();
  while( reader.next() ) {
    String ID    = reader.getString( "ID"    );
    MCRCategoryItem child = new MCRCategoryItem( ID, classifID, parentID );
    String tag = reader.getString( "TAG" );
    child.setTag( tag );
    children.addElement( child );
    }
  return children;
  }
  
/**
 * The method return the number of MCRCategoryItems from the datastore.
 *
 * @param classifID the ID of the MCRClassificationItem
 * @param categID the ID of the MCRCategoryItem
 * @return the number of MCRCategoryItem children
 **/
public final int retrieveNumberOfChildren( String classifID, String parentID )
  {
  return MCRSQLConnection.justCountRows( new MCRSQLStatement( tableCateg )
    .setCondition( "PID",  parentID  )
    .setCondition( "CLID", classifID )
    .toRowSelector() );
  }
  
}

