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

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.mycore.common.*;
import org.mycore.datamodel.metadata.*;
import org.mycore.datamodel.classifications.*;

/** 
 * This class implements the MCRLinkTableInterface as a presistence
 * layer for the store of a table with link connections under the SQL database.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRSQLLinkTableStore implements MCRLinkTableInterface
{

// logger
static Logger logger=Logger.getLogger(MCRSQLLinkTableStore.class.getName());

// internal data
private String tableName;
private String mytype;
private int lengthClassID  = MCRMetaClassification.MAX_CLASSID_LENGTH;
private int lengthCategID  = MCRMetaClassification.MAX_CATEGID_LENGTH;
private int lengthObjectID = MCRObjectID.MAX_LENGTH;

/**
 * The constructor for the class MCRSQLLinkTableStore.
 **/
public MCRSQLLinkTableStore()
  { }

/**
 * The initializer for the class MCRSQLLinkTableStore. It reads 
 * the classification configuration and checks the table names.
 *
 * @exception throws if the type is not correct
 **/
public final void init(String type)
  throws MCRPersistenceException
  { 
  MCRConfiguration config = MCRConfiguration.instance();
  // Check the parameter
  if ((type == null) || ((type = type.trim()).length() ==0)) {
     throw new MCRPersistenceException("The type of the constructor"+
       " is null or empty.");
     }
  boolean test = false;
  for (int i=0; i < MCRLinkTableManager.LINK_TABLE_TYPES.length; i++) {
    if (type.equals(MCRLinkTableManager.LINK_TABLE_TYPES[i])) { 
      test = true; break; }
    }
  if (!test) {
     throw new MCRPersistenceException("The type of the constructor"+
       " is false.");
     }
  mytype = type;
  // set configuration
  tableName = config.getString( 
    "MCR.linktable_store_sql_table_"+type,"MCRLINKTABLE" );
  if(! MCRSQLConnection.doesTableExist(tableName)) {
    logger.info("Create table "+tableName);
    createLinkTable(); 
    logger.info("Done."); }
  }
  
/**
 * The method drop the table.
 **/
public final void dropTables()
  {
  MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
  try { 
    c.doUpdate( "DROP TABLE " + tableName );
    }
  finally{ c.release(); }
  }

/**
 * The method create a table for classification.
 **/
private final void createLinkTable()
  {
  MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
  try { 
    if (mytype.equals("class")) {
      c.doUpdate( new MCRSQLStatement( tableName )
       .addColumn( "MCRFROM VARCHAR("+Integer.toString(lengthObjectID)+
         ") NOT NULL" )
       .addColumn( "MCRTO VARCHAR("+
         Integer.toString(lengthClassID+lengthCategID+2)+
         ") NOT NULL" )
       .addColumn( "PRIMARY KEY (MCRFROM,MCRTO)" )
       .toCreateTableStatement() );
      c.doUpdate (new MCRSQLStatement(tableName)
       .addColumn("MCRFROM").addColumn("MCRTO").toIndexStatement());
      }
    else {
      c.doUpdate( new MCRSQLStatement( tableName )
       .addColumn( "MCRFROM VARCHAR("+
         Integer.toString(lengthObjectID)+") NOT NULL" )
       .addColumn( "MCRTO VARCHAR("+
         Integer.toString(lengthObjectID)+") NOT NULL" )
       .addColumn( "PRIMARY KEY (MCRFROM,MCRTO)" )
       .toCreateTableStatement() );
      c.doUpdate (new MCRSQLStatement(tableName)
       .addColumn("MCRFROM").addColumn("MCRTO").toIndexStatement());
      }
    }
  finally{ c.release(); }
  }
    
/**
 * The method create a new item in the datastore.
 *
 * @param from a string with the link ID MCRFROM
 * @param to a string with the link ID TO
 **/
public final void create(String from, String to)
  {
  if ((from == null) || ((from = from.trim()).length() ==0)) {
     throw new MCRPersistenceException("The from value is null or empty.");
     }
  if ((to == null) || ((to = to.trim()).length() ==0)) {
     throw new MCRPersistenceException("The to value is null or empty.");
     }
  MCRSQLConnection.justDoUpdate( new MCRSQLStatement( tableName )
    .setValue( "MCRFROM", from)
    .setValue( "MCRTO", to)
    .toInsertStatement() );
  }
  
/**
 * The method remove a item for the from ID from the datastore.
 *
 * @param from a string with the link ID MCRFROM
 **/
public final void delete( String from )
  {
  if ((from == null) || ((from = from.trim()).length() ==0)) {
     throw new MCRPersistenceException("The from value is null or empty.");
     }
  MCRSQLConnection.justDoUpdate( new MCRSQLStatement( tableName )
    .setCondition( "MCRFROM", from )
    .toDeleteStatement() );
  }
  
/**
 * The method count the number of references to the 'to' value of the table.
 *
 * @param to the object ID as String, they was referenced
 * @return the number of references
 **/
  public final int countTo( String to )
  {
    String sql = new MCRSQLStatement( tableName ).setCondition( "MCRTO", to ).toCountStatement();
    MCRSQLRowReader reader = MCRSQLConnection.justDoQuery( sql );
    int num = 0;
    try
    {
      if( reader.next() ) num = reader.getInt( "NUMBER" );
      return num;   
    }   
    catch( Exception e ) 
    { throw new MCRException("SQL counter error",e); } 
    finally
    { reader.close(); }
  }
}

