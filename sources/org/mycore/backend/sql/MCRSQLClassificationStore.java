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

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.mycore.common.*;
import org.mycore.datamodel.metadata.*;
import org.mycore.datamodel.classifications.*;

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

// logger
static Logger logger=Logger.getLogger(MCRSQLClassificationStore.class.getName());

// internal data
private String tableClass;
private String tableClassLabel;
private String tableCateg;
private String tableCategLabel;
private int lengthClassID = MCRMetaClassification.MAX_CLASSID_LENGTH;
private int lengthCategID = MCRMetaClassification.MAX_CATEGID_LENGTH;
private int lengthLang    = MCRClassificationObject.MAX_CLASSIFICATION_LANG;
private int lengthText    = MCRClassificationObject.MAX_CLASSIFICATION_TEXT;
private int lengthURL     = MCRClassificationObject.MAX_CATEGORY_URL;
private int lengthDescription = MCRClassificationObject
    .MAX_CLASSIFICATION_DESCRIPTION;

/**
 * The constructor for the class MCRSQLClassificationStore. It reads 
 * the classification configuration and checks the table names.
 **/
public MCRSQLClassificationStore()
  { 
  MCRConfiguration config = MCRConfiguration.instance();
  // set the logger property
  PropertyConfigurator.configure(config.getLoggingProperties());
  // set configuration
  tableClass = config.getString( 
    "MCR.classifications_store_sql_table_class","MCRCLASS" );
  tableClassLabel = config.getString( 
    "MCR.classifications_store_sql_table_classlabel","MCRCLASSLABEL" );
  tableCateg = config.getString( 
    "MCR.classifications_store_sql_table_categ","MCRCATEG" );
  tableCategLabel = config.getString( 
    "MCR.classifications_store_sql_table_categlabel","MCRCATEGLABEL" );
  if(! MCRSQLConnection.doesTableExist(tableClass)) {
    logger.info("Create table "+tableClass);
    createClass(); 
    logger.info("Done."); }
  if(! MCRSQLConnection.doesTableExist(tableClassLabel)) { 
    logger.info("Create table "+tableClassLabel);
    createClassLabel(); 
    logger.info("Done."); }
  if(! MCRSQLConnection.doesTableExist(tableCateg)) { 
    logger.info("Create table "+tableCateg);
    createCateg(); 
    logger.info("Done."); }
  if(! MCRSQLConnection.doesTableExist(tableCategLabel)) { 
    logger.info("Create table "+tableCategLabel);
    createCategLabel(); 
    logger.info("Done."); }
  }
  
/**
 * The method drop the classification and category tables.
 **/
public final void dropTables()
  {
  MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
  try { 
    c.doUpdate( "DROP TABLE " + tableClass );
    c.doUpdate( "DROP TABLE " + tableClassLabel );
    c.doUpdate( "DROP TABLE " + tableCateg );
    c.doUpdate( "DROP TABLE " + tableClassLabel );
    }
  finally{ c.release(); }
  }

/**
 * The method create a table for classification.
 **/
private final void createClass()
  {
  MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
  try { 
    // the classification table
    c.doUpdate( new MCRSQLStatement( tableClass )
     .addColumn( "ID VARCHAR("+Integer.toString(lengthClassID)+
       ") NOT NULL PRIMARY KEY" )
     .toCreateTableStatement() );
    }
  finally{ c.release(); }
  }
    
/**
 * The method create a table for category.
 **/
private final void createCateg()
  {
  MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
  try { 
    // the category table
    c.doUpdate( new MCRSQLStatement( tableCateg )
     .addColumn( "ID VARCHAR("+Integer.toString(lengthCategID)+") NOT NULL" )
     .addColumn( "CLID VARCHAR("+Integer.toString(lengthClassID)+") NOT NULL" )
     .addColumn( "PID VARCHAR("+Integer.toString(lengthCategID)+")" )
     .addColumn( "URL VARCHAR("+Integer.toString(lengthURL)+")" )
     .addColumn( "PRIMARY KEY ( CLID, ID )" )
     .toCreateTableStatement() );
    }
  finally{ c.release(); }
  }
    
/**
 * The method create a label table for classification.
 **/
private final void createClassLabel()
  {
  MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
  try { 
    // the classification table
    c.doUpdate( new MCRSQLStatement( tableClassLabel )
     .addColumn( "ID VARCHAR("+Integer.toString(lengthClassID)+
       ") NOT NULL" )
     .addColumn( "LANG VARCHAR("+Integer.toString(lengthLang)+
       ") NOT NULL" )
     .addColumn( "TEXT VARCHAR("+Integer.toString(lengthText)+
       ")" )
     .addColumn( "MCRDESC VARCHAR("+Integer.toString(lengthDescription)+
       ")" )
     .addColumn( "PRIMARY KEY ( ID, LANG )" )
     .toCreateTableStatement() );
    }
  finally{ c.release(); }
  }
    
/**
 * The method create a label table for category.
 **/
private final void createCategLabel()
  {
  MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
  try { 
    // the category table
    c.doUpdate( new MCRSQLStatement( tableCategLabel )
     .addColumn( "ID VARCHAR("+Integer.toString(lengthCategID)+
       ") NOT NULL" )
     .addColumn( "CLID VARCHAR("+Integer.toString(lengthClassID)+
       ") NOT NULL" )
     .addColumn( "LANG VARCHAR("+Integer.toString(lengthLang)+
       ") NOT NULL" )
     .addColumn( "TEXT VARCHAR("+Integer.toString(lengthText)+
       ")" )
     .addColumn( "MCRDESC VARCHAR("+Integer.toString(lengthDescription)+
       ")" )
     .addColumn( "PRIMARY KEY ( CLID, ID, LANG )" )
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
  MCRSQLConnection.justDoUpdate( new MCRSQLStatement( tableClass )
    .setValue( "ID",    classification.getID()    )
    .toInsertStatement() );
  for (int i=0;i<classification.getSize();i++) {
    MCRSQLConnection.justDoUpdate( new MCRSQLStatement( tableClassLabel )
      .setValue( "ID",    classification.getID()    )
      .setValue( "LANG",  classification.getLang(i) )
      .setValue( "TEXT",  classification.getText(i) )
      .setValue( "MCRDESC",  classification.getDescription(i) )
      .toInsertStatement() );
    }
  }
  
/**
 * The method remove a MCRClassificationItem from the datastore.
 *
 * @param ID the ID of the MCRClassificationItem
 **/
public void deleteClassificationItem( String ID )
  {
  MCRSQLConnection.justDoUpdate( new MCRSQLStatement( tableClass )
    .setCondition( "ID", ID )
    .toDeleteStatement() );
  MCRSQLConnection.justDoUpdate( new MCRSQLStatement( tableClassLabel )
    .setCondition( "ID", ID )
    .toDeleteStatement() );
  MCRSQLConnection.justDoUpdate( new MCRSQLStatement( tableCateg )
    .setCondition( "CLID", ID )
    .toDeleteStatement() );
  MCRSQLConnection.justDoUpdate( new MCRSQLStatement( tableCategLabel )
    .setCondition( "CLID", ID )
    .toDeleteStatement() );
  }
  
/**
 * The method return a MCRClassificationItem from the datastore.
 *
 * @param ID the ID of the MCRClassificationItem
 **/
public final MCRClassificationItem retrieveClassificationItem( String ID )
  {
  MCRSQLRowReader reader = MCRSQLConnection.justDoQuery( 
    new MCRSQLStatement( tableClass )
    .setCondition( "ID", ID )
    .toSelectStatement() );
  if( ! reader.next() ) return null;
  MCRClassificationItem c = new MCRClassificationItem( ID );
  reader = MCRSQLConnection.justDoQuery( 
    new MCRSQLStatement( tableClassLabel )
    .setCondition( "ID", ID )
    .toSelectStatement() );
  while (reader.next()) {
    String lang = reader.getString( "LANG" );
    String text = reader.getString( "TEXT" );
    String desc = reader.getString( "MCRDESC" );
    c.addData(lang,text,desc);
    }
  return c;
  }
  
/**
 * The method return if the MCRClassificationItem is in the datastore.
 *
 * @param ID the ID of the MCRClassificationItem
 * @return true if the MCRClassificationItem was found, else false
 **/
public final boolean classificationItemExists( String ID )
  {
  return MCRSQLConnection.justCheckExists( new MCRSQLStatement( tableClass )
    .setCondition( "ID", ID )
    .toRowSelector() );
  }

/**
 * The method create a new MCRCategoryItem in the datastore.
 *
 * @param category an instance of a MCRCategoryItem
 **/
public final void createCategoryItem( MCRCategoryItem category )
  {
  try {
    MCRSQLConnection.justDoUpdate( new MCRSQLStatement( tableCateg )
    .setValue( "ID",    category.getID()               )
    .setValue( "CLID",  category.getClassificationID() )
    .setValue( "PID",   category.getParentID()         )
    .setValue( "URL",   category.getURL()         )
    .toInsertStatement() );
    }
  catch (Exception e) {
    MCRSQLConnection.justDoUpdate( new MCRSQLStatement( tableCateg )
    .setValue( "ID",    category.getID()               )
    .setValue( "CLID",  category.getClassificationID() )
    .setValue( "PID",   category.getParentID()         )
    .toInsertStatement() );
    }
  for (int i=0;i<category.getSize();i++) {
    MCRSQLConnection.justDoUpdate( new MCRSQLStatement( tableCategLabel )
      .setValue( "ID",    category.getID() )
      .setValue( "CLID",  category.getClassificationID() )
      .setValue( "LANG",  category.getLang(i) )
      .setValue( "TEXT",  category.getText(i) )
      .setValue( "MCRDESC",  category.getDescription(i) )
      .toInsertStatement() );
    }
  }
    
/**
 * The method remove a MCRCategoryItem from the datastore.
 *
 * @param classifID the ID of the MCRClassificationItem
 * @param categID the ID of the MCRCategoryItem
 **/
public final void deleteCategoryItem( String CLID, String ID )
  {
  MCRSQLConnection.justDoUpdate( new MCRSQLStatement( tableCateg )
    .setCondition( "CLID", CLID )
    .setCondition( "ID",   ID   )
    .toDeleteStatement() );
  MCRSQLConnection.justDoUpdate( new MCRSQLStatement( tableCategLabel )
    .setCondition( "CLID", CLID )
    .setCondition( "ID",   ID   )
    .toDeleteStatement() );
  }
  
/**
 * The method return a MCRCategoryItem from the datastore.
 *
 * @param CLID the ID of the MCRClassificationItem
 * @param ID   the ID of the MCRCategoryItem
 **/
public final MCRCategoryItem retrieveCategoryItem( String CLID, String ID )
  {
  MCRSQLRowReader reader = MCRSQLConnection.justDoQuery( 
    new MCRSQLStatement( tableCateg )
    .setCondition( "ID",   ID   )
    .setCondition( "CLID", CLID )
    .toSelectStatement() );
  if( ! reader.next() ) return null;
  String PID = reader.getString( "PID"   );
  String URL = "";
  try { URL = reader.getString( "URL" ); } catch(Exception e) { URL = ""; }
  if (URL == null) { URL = ""; }
  MCRCategoryItem c = new MCRCategoryItem( ID, CLID, PID );
  c.setURL(URL);
  reader = MCRSQLConnection.justDoQuery( 
    new MCRSQLStatement( tableCategLabel )
    .setCondition( "ID",   ID   )
    .setCondition( "CLID", CLID )
    .toSelectStatement() );
  if( ! reader.next() ) return null;
  do {
    String lang = reader.getString( "LANG" );
    String text = reader.getString( "TEXT" );
    String desc = reader.getString( "MCRDESC" );
    c.addData(lang,text,desc);
    } while (reader.next());
  return c;
  }
  
/**
 * The method return a MCRCategoryItem from the datastore.
 *
 * @param CLID the ID of the MCRClassificationItem
 * @param labeltext   the label text of the MCRCategoryItem
 **/
public MCRCategoryItem retrieveCategoryItemForLabelText(String CLID,
  String labeltext)
  {
  MCRSQLRowReader reader = MCRSQLConnection.justDoQuery( 
    new MCRSQLStatement( tableCategLabel )
    .setCondition( "TEXT", labeltext )
    .setCondition( "CLID", CLID )
    .toSelectStatement() );
  if( ! reader.next() ) return null;
  String ID = reader.getString( "ID" );
  String lang = reader.getString( "LANG" );
  String text = reader.getString( "TEXT" );
  String desc = reader.getString( "MCRDESC" );
  MCRCategoryItem c = new MCRCategoryItem( ID, CLID, "" );
  c.addData(lang,text,desc);
  reader = MCRSQLConnection.justDoQuery( 
    new MCRSQLStatement( tableCateg )
    .setCondition( "ID",   ID   )
    .setCondition( "CLID", CLID )
    .toSelectStatement() );
  if( ! reader.next() ) return null;
  String URL = "";
  try { URL = reader.getString( "URL" ); } catch(Exception e) { URL = ""; }
  if (URL == null) { URL = ""; }
  c.setURL(URL);
  return c;
  }

/**
 * The method return if the MCRCategoryItem is in the datastore.
 *
 * @param CLID the ID of the MCRClassificationItem
 * @param ID   the ID of the MCRCategoryItem
 * @return true if the MCRCategoryItem was found, else false
 **/
public final boolean categoryItemExists( String CLID, String ID )
  {
  return MCRSQLConnection.justCheckExists( new MCRSQLStatement( tableCateg )
    .setCondition( "ID",   ID   )
    .setCondition( "CLID", CLID )
    .toRowSelector() );
  }

/**
 * The method return an Vector of MCRCategoryItems from the datastore.
 *
 * @param CLID the ID of the MCRClassificationItem
 * @param PID  the parent ID of the MCRCategoryItem
 * @return a list of MCRCategoryItem children
 **/
public final ArrayList retrieveChildren( String CLID, String PID )
  {
  ArrayList children = new ArrayList();
  MCRSQLRowReader reader = MCRSQLConnection.justDoQuery( 
    new MCRSQLStatement( tableCateg )
    .setCondition( "PID",  PID  )
    .setCondition( "CLID", CLID )
    .toSelectStatement() );
  while( reader.next() ) {
    String ID    = reader.getString( "ID"    );
    String URL   = "";
    try { URL = reader.getString( "URL" ); } catch(Exception e) { URL = ""; }
    if (URL == null) { URL = ""; }
    MCRCategoryItem child = new MCRCategoryItem( ID, CLID, PID );
    child.setURL(URL);
    children.add( child );
    }
  for (int i=0;i<children.size();i++) {
    reader = MCRSQLConnection.justDoQuery( 
      new MCRSQLStatement( tableCategLabel )
      .setCondition( "ID", ((MCRCategoryItem)children.get(i)).getID())
      .setCondition( "CLID", CLID )
      .toSelectStatement() );
    while( reader.next() ) {
      String lang = reader.getString( "LANG" );
      String text = reader.getString( "TEXT" );
      String desc = reader.getString( "MCRDESC" );
      ((MCRCategoryItem)children.get(i)).addData(lang,text,desc);
      }
    }
  return children;
  }
  
/**
 * The method return the number of MCRCategoryItems from the datastore.
 *
 * @param CLID the ID of the MCRClassificationItem
 * @param PID  the parent ID of the MCRCategoryItem
 * @return the number of MCRCategoryItem children
 **/
public final int retrieveNumberOfChildren( String CLID, String PID )
  {
  return MCRSQLConnection.justCountRows( new MCRSQLStatement( tableCateg )
    .setCondition( "PID",  PID  )
    .setCondition( "CLID", CLID )
    .toRowSelector() );
  }
  
/**
 * The method returns all availiable classification ID's they are loaded.
 * 
 * @return a list of classification ID's as String array 
 **/
public final String [] getAllClassificationID()
  {
  int len = MCRSQLConnection.justCountRows( new MCRSQLStatement( tableClass )
	.addColumn("ID")
//    .setCondition( "ID",  "*"  )
    .toRowSelector() );
  logger.debug("Number of classifications = "+Integer.toString(len));
  MCRSQLRowReader reader = MCRSQLConnection.justDoQuery( 
    new MCRSQLStatement( tableClass )
	.addColumn("ID")
//  .setCondition( "ID",  "*"  )
    .toSelectStatement() );
  String ID [] = new String[len];
  int i = 0;
  while( reader.next() ) {
    ID[i] = reader.getString( "ID" );
    logger.debug("ID of classifications["+Integer.toString(i)+"] = "+ID[i]);
    i++;
    }
  return ID;
  }
}

