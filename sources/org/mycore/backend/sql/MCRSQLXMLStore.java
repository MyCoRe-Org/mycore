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

import java.sql.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.mycore.common.*;
import org.mycore.datamodel.metadata.*;

/** 
 * This class implements the MCRXMLInterface as a presistence
 * layer for the store of a table with a MCRObjectID and the 
 * corresponding XML file.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRSQLXMLStore implements MCRXMLTableInterface
{

// logger
static Logger logger=Logger.getLogger(MCRSQLXMLStore.class.getName());

// internal data
private String tableName;
private String mytype;
private int lengthObjectID = MCRObjectID.MAX_LENGTH;
private int lengthXML  = MCRDefaults.MAX_XML_FILE_LENGTH;

/**
 * The constructor for the class MCRSQLXMLStore.
 **/
public MCRSQLXMLStore()
  { }

/**
 * The initializer for the class MCRSQLXMLStore. It reads 
 * the configuration and checks the table names and create the
 * table if they does'n exist..
 *
 * @param type the type String of the MCRObjectID
 * @exception throws if the type is not correct
 **/
public final void init(String type)
  throws MCRPersistenceException
  { 
  MCRConfiguration config = MCRConfiguration.instance();
  // set the logger property
  PropertyConfigurator.configure(config.getLoggingProperties());
  // Check the parameter
  if ((type == null) || ((type = type.trim()).length() ==0)) {
     throw new MCRPersistenceException("The type of the constructor"+
       " is null or empty.");
     }
  boolean test = config.getBoolean("MCR.type_"+type,false);
  if (!test) {
     throw new MCRPersistenceException("The type "+type+" of the constructor"+
       " is false.");
     }
  mytype = type;
  // set configuration
  tableName = config.getString( 
    "MCR.xml_store_sql_table_"+mytype,"MCRXMLTABLE" );
  if(! MCRSQLConnection.doesTableExist(tableName)) {
    logger.info("Create table "+tableName);
    createXMLTable(); 
    logger.info("Done."); }
  }
  
/**
 * The method create a table for the XML store.
 **/
private synchronized final void createXMLTable()
  {
  MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
  try { 
    c.doUpdate( new MCRSQLStatement( tableName )
     .addColumn( "MCRID VARCHAR("+
       Integer.toString(lengthObjectID)+") NOT NULL" )
     .addColumn( "MCRVERSION INTEGER NOT NULL" )
     .addColumn( "MCRTYPE VARCHAR("+
       Integer.toString(lengthObjectID)+") NOT NULL" )
     .addColumn( "MCRXML BLOB" )
     .addColumn("PRIMARY KEY(MCRID,MCRVERSION)")
     .toCreateTableStatement() );
    }
  finally{ c.release(); }
  }
    
/**
 * The method create a new item in the datastore.
 *
 * @param mcrid a MCRObjectID
 * @param xml a byte array with the XML file
 * @param version the version of the XML Blob as integer
 * @exception if the method arguments are not correct
 **/
public synchronized final void create(MCRObjectID mcrid, byte[] xml,
  int version) throws MCRPersistenceException
  {
  if (mcrid == null) {
     throw new MCRPersistenceException("The MCRObjectID is null."); }
  if ((xml == null) || (xml.length ==0)) {
     throw new MCRPersistenceException("The XML arrax is null or empty."); }
  MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();
  try {
    connection.getJDBCConnection().setAutoCommit(false);
    StringBuffer sb = new StringBuffer(1024);
    sb.append("INSERT INTO ").append(tableName)
      .append(" (MCRID,MCRVERSION,MCRTYPE,MCRXML) VALUES (?,?,?,?)");
    PreparedStatement statement = connection.getJDBCConnection()
      .prepareStatement(sb.toString());
    statement.setString(1,mcrid.getId());
    statement.setInt(2,version);
    statement.setString(3,mcrid.getTypeId());
    statement.setBytes(4,xml);
    statement.execute();
    statement.close();
    connection.getJDBCConnection().commit();
    connection.getJDBCConnection().setAutoCommit(true);
    }
  catch(Exception ex) {
    try{ connection.getJDBCConnection().rollback(); }
    catch(SQLException ignored){}
    throw new MCRException("Error in MCRXMLStore table create "+tableName+".",
      ex);
  }
  finally
    { connection.release(); }
  }
  
/**
 * The method remove a item for the MCRObjectID from the datastore.
 *
 * @param mcrid a MCRObjectID
 * @param version the version of the XML Blob as integer
 * @exception if the method argument is not correct
 **/
public synchronized final void delete( MCRObjectID mcrid, int version )
  throws MCRPersistenceException
  {
  if (mcrid == null){
     throw new MCRPersistenceException("The MCRObjectID is null."); }
  MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();
  try {
    connection.getJDBCConnection().setAutoCommit(false);
    StringBuffer sb = new StringBuffer(1024);
    sb.append("DELETE FROM ").append(tableName)
      .append("WHERE ( MCRID = ? AND MCRVERSION = ? )");
    PreparedStatement statement = connection.getJDBCConnection()
      .prepareStatement(sb.toString());
    statement.setString(1,mcrid.getId());
    statement.setInt(2,version);
    statement.execute();
    statement.close();
    connection.getJDBCConnection().commit();
    connection.getJDBCConnection().setAutoCommit(true);
    }
  catch(Exception ex) {
    try{ connection.getJDBCConnection().rollback(); }
    catch(SQLException ignored){}
    throw new MCRException("Error in MCRXMLStore table delete "+tableName+".",
      ex);
    }
  finally
    { connection.release(); }
  }
  
/**
 * The method update an item in the datastore.
 *
 * @param mcrid a MCRObjectID
 * @param xml a byte array with the XML file
 * @param version the version of the XML Blob as integer
 * @exception if the method arguments are not correct
 **/
public synchronized final void update(MCRObjectID mcrid, byte[] xml,
  int version) 
  throws MCRPersistenceException
  {
  if (mcrid == null) {
    throw new MCRPersistenceException("The MCRObjectID is null."); }
  if ((xml == null) || (xml.length ==0)) {
    throw new MCRPersistenceException("The XML arrax is null or empty."); }
  MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();
  try {
    connection.getJDBCConnection().setAutoCommit(false);
    StringBuffer sb = new StringBuffer(1024);
    sb.append("UPDATE ").append(tableName)
      .append(" SET MCRID = ?, MCRVERSION = ?, MCRTYPE = ?, MCRXML = ? ")
      .append("WHERE (MCRID = ? AND MCRVERSION = ? )");
    PreparedStatement statement = connection.getJDBCConnection()
      .prepareStatement(sb.toString());
    statement.setString(1,mcrid.getId());
    statement.setInt(2,version);
    statement.setString(3,mcrid.getTypeId());
    statement.setBytes(4,xml);
    statement.setString(5,mcrid.getId());
    statement.setInt(6,version);
    statement.execute();
    statement.close();
    connection.getJDBCConnection().commit();
    connection.getJDBCConnection().setAutoCommit(true);
    }
  catch(Exception ex) {
      try{ connection.getJDBCConnection().rollback(); }
      catch(SQLException ignored){}
      throw new MCRException("Error in MCRXMLStore table update "+tableName+".",ex);
    }
  finally
    { connection.release(); }
  }
  
/**
 * The method retrieve a dataset for the given MCRObjectID and returns
 * the corresponding XML file as byte array.
 *
 * @param mcrid a MCRObjectID
 * @param version the version of the XML Blob as integer
 * @return the XML-File as byte array or null
 * @exception if the method arguments are not correct
 **/
public final byte[] retrieve(MCRObjectID mcrid, int version)
  throws MCRPersistenceException
  {
  if (mcrid == null) {
    throw new MCRPersistenceException("The MCRObjectID is null."); }
  MCRSQLConnection connection = MCRSQLConnectionPool.instance()
    .getConnection();
  try {
    StringBuffer sb = new StringBuffer("SELECT MCRXML FROM ").append(tableName)
      .append(" WHERE ( MCRID = '").append(mcrid.getId()).append("'")
      .append(" AND MCRVERSION = ").append(version).append(" )");
    Statement statement = connection.getJDBCConnection().createStatement();
    ResultSet rs = statement.executeQuery(sb.toString());
    if(!rs.next()) {
      String msg = "There is no dataset with MCRID = " + mcrid.getId();
      logger.debug(msg);
      return null;
      }
    byte[] xml = rs.getBytes(1);
    rs.close();
    return xml;
    }
  catch (Exception ex) {
    throw new MCRException("Error in MCRXMLStore.",ex); }
  finally{ connection.release(); }
  }

/**
  * This method returns the next free ID number for a given
  * MCRObjectID base. This method ensures that any invocation
  * returns a new, exclusive ID by remembering the highest ID
  * ever returned and comparing it with the highest ID stored
  * in the related index class.
  *
  * @param project_ID   the project ID part of the MCRObjectID base
  * @param type_ID      the type ID part of the MCRObjectID base
  *
  * @exception MCRPersistenceException if a persistence problem is occured
  *
  * @return the next free ID number as a String
  **/
public final int getNextFreeIdInt( String project, String type )
  throws MCRPersistenceException
  {
  String query = new StringBuffer()
        .append( "SELECT MAX(MCRID) FROM " )
        .append( tableName ).toString();
  try {
    return (new MCRObjectID(MCRSQLConnection.justGetSingleValue(query)))
      .getNumberAsInteger() + 1;
    }
  catch(Exception e) { }
  return 1;
  }

/**
 * This method check that the MCRObjectID exist in this store.
 *
 * @param mcrid a MCRObjectID
 * @param version the version of the XML Blob as integer
 * @return true if the MCRObjectID exist, else return false
 **/
public final boolean exist(MCRObjectID mcrid, int version)
  {
  MCRSQLConnection connection = MCRSQLConnectionPool.instance()
    .getConnection();
  boolean test = false;
  try {
    StringBuffer sb = new StringBuffer("SELECT MCRID FROM ").append(tableName)
      .append(" WHERE ( MCRID = '").append(mcrid.getId()).append("'")
      .append(" AND MCRVERSION = ").append(version).append(" )");
    Statement statement = connection.getJDBCConnection().createStatement();
    ResultSet rs = statement.executeQuery(sb.toString());
    if(rs.next()) { test = true; } 
    rs.close();
    }
  catch (Exception ex) {
    throw new MCRException("Error in MCRXMLStore.",ex); }
  finally{ connection.release(); }
  return test;
  }

}

