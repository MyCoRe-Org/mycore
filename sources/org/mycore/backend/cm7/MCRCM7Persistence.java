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

import java.io.*;
import java.util.*;

import com.ibm.mm.sdk.server.*;
import com.ibm.mm.sdk.common.*;

import org.apache.log4j.Logger;

import org.mycore.common.*;
import org.mycore.datamodel.metadata.*;
import org.mycore.backend.sql.*;

/**
 * This class implements all methode for handling the data to the data store
 * based on IBM Content Manager 7.
 *
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 *
 * @version $Revision$ $Date$
 **/
public final class MCRCM7Persistence implements MCRObjectPersistenceInterface
{

// from configuration
private static String mcr_id_name = null;
private static String mcr_label_name = null;
private static String mcr_flag_name = null;
private static String mcr_create_name = null;
private static String mcr_modify_name = null;
private static String mcr_ts_server = null;
private static String mcr_ts_lang = null;
private static int mcr_ts_part = 1;
private static int mcr_xml_part = 2;
private static MCRConfiguration conf = null;

private String mcr_index_class = null;
private String mcr_ts_index = null;

private static String NL =
  new String((System.getProperties()).getProperty("line.separator"));

/**
 * The static part that read the configuration
 **/
static
  {
  conf = MCRConfiguration.instance();
  mcr_id_name = conf.getString("MCR.persistence_cm7_field_id");
  mcr_label_name = conf.getString("MCR.persistence_cm7_field_label");
  mcr_flag_name = conf.getString("MCR.persistence_cm7_field_flag");
  mcr_create_name = conf.getString("MCR.persistence_cm7_field_datecreate");
  mcr_modify_name = conf.getString("MCR.persistence_cm7_field_datemodify");
  mcr_ts_part = conf.getInt("MCR.persistence_cm7_part_ts");
  mcr_xml_part = conf.getInt("MCR.persistence_cm7_part_xml");
  mcr_ts_server = conf.getString("MCR.persistence_cm7_textsearch_server");
  mcr_ts_lang = conf.getString("MCR.persistence_cm7_textsearch_lang");
  if ((mcr_id_name == null) || (mcr_label_name == null) || 
      (mcr_flag_name == null) ||
      (mcr_create_name == null) || (mcr_modify_name == null)) {
    throw new MCRConfigurationException("A indexclass field name is false."); }
  }

/**
 * The constructor of this class.
 **/
public MCRCM7Persistence()
  { }

/**
 * The methode create an object in the data store. The index class
 * is determinated by the type of the object ID. This <b>must</b>
 * correspond with the lower case configuration name.<br>
 * As example: Document --> MCR.persistence_cm7_document
 *
 * @param mcr_tc      the typed content array
 * @param xml         the XML stream from the object as JDOM
 * @param mcr_ts_in   the text search string
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void create(MCRTypedContent mcr_tc, org.jdom.Document jdom,
  String mcr_ts_in)
  throws MCRConfigurationException, MCRPersistenceException
  {
  Logger logger = MCRCM7ConnectionPool.getLogger();
  // convert the JDOM tree
  byte [] xml = MCRUtils.getByteArray(jdom);
  // extract index data from typed content
  MCRObjectID mcr_id = null;
  String mcr_label = null;
  String mcr_flags = "";
  GregorianCalendar mcr_create = null;
  GregorianCalendar mcr_modify = null;
  String mcr_ts = null;
  for (int i=0;i<mcr_tc.getSize();i++) {
    if (mcr_tc.getNameElement(i).equals("ID")) {
      mcr_id = new MCRObjectID((String)mcr_tc.getValueElement(i)); 
      mcr_label = (String)mcr_tc.getValueElement(i+1); }
    }
  for (int i=0;i<mcr_tc.getSize();i++) {
    if (mcr_tc.getNameElement(i).toLowerCase().equals("service")) {
      for (int j=i;j<mcr_tc.getSize();j++) {
        if (mcr_tc.getNameElement(j).toLowerCase().equals("servdate")) {
          if (mcr_tc.getValueElement(j+3).equals("createdate")) {
            mcr_create = (GregorianCalendar) mcr_tc.getValueElement(j+1);
            continue; }
          if (mcr_tc.getValueElement(j+3).equals("modifydate")) {
            mcr_modify = (GregorianCalendar) mcr_tc.getValueElement(j+1);
            continue; }
          }
        if (mcr_tc.getNameElement(j).toLowerCase().equals("servflag")) {
          mcr_flags = mcr_flags + " " + (String)mcr_tc.getValueElement(j+1); }
        }
      break;
      }
    }
  mcr_ts = createTS(mcr_tc)+NL+mcr_ts_in.toUpperCase();
  // read configuration
  StringBuffer sb = new StringBuffer("MCR.persistence_cm7_");
  sb.append(mcr_id.getTypeId().toLowerCase());
  mcr_index_class = conf.getString(sb.toString()); 
  sb.append("_ts");
  mcr_ts_index = conf.getString(sb.toString());
  // store the data
  DKDatastoreDL connection = null;
  try {
    connection = MCRCM7ConnectionPool.instance().getConnection();
    boolean test = false;
    try {
      MCRCM7Item checkitem = getItem(mcr_id.getId(),mcr_index_class,
        connection);
      test = true; }
    catch (MCRPersistenceException e) { }
    if (test) {
      throw new MCRPersistenceException(
        "A object with ID "+mcr_id.getId()+" exists."); }
    MCRCM7Item item = new MCRCM7Item(connection,mcr_index_class,
      DKConstant.DK_DOCUMENT);
    item.setKeyfield(mcr_id_name,mcr_id.getId());
    item.setKeyfield(mcr_label_name,mcr_label);
    item.setKeyfield(mcr_flag_name,mcr_flags);
    item.setKeyfield(mcr_create_name,mcr_create);
    item.setKeyfield(mcr_modify_name,mcr_modify);
    item.setPart(mcr_xml_part,xml,xml.length);
    item.setPart(mcr_ts_part,mcr_ts,mcr_ts_server,mcr_ts_index,mcr_ts_lang);
    item.create();
    updateTextIndexQueue(connection,mcr_ts_index);
    logger.info("Item "+mcr_id.getId()+" was created.");
    }
  catch (Exception e) {
    throw new MCRPersistenceException(e.getMessage()); }
  finally {
    MCRCM7ConnectionPool.instance().releaseConnection(connection); }
  }

/**
 * The methode create a new datastore based of given configuration. It create
 * a new data table for storing MCRObjects with the same MCRObjectID type.
 *
 * @param mcr_type    the MCRObjectID type as string
 * @param mcr_conf    the configuration XML stream as JDOM tree
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public void createDataBase(String mcr_type, org.jdom.Document mcr_conf)
  throws MCRConfigurationException, MCRPersistenceException
  { 
  Logger logger = MCRCM7ConnectionPool.getLogger();
  logger.info("This feature exist not for this store.");
  }

/**
 * The methode delete an object from the data store. The index class
 * is determinated by the type of the object ID. This <b>must</b>
 * correspond with the lower case configuration name.<br>
 * As example: Document --> MCR.persistence_cm7_document
 *
 * @param mcr_id      the object id
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void delete(MCRObjectID mcr_id)
  throws MCRConfigurationException, MCRPersistenceException
  {
  Logger logger = MCRCM7ConnectionPool.getLogger();
  StringBuffer sb = new StringBuffer("MCR.persistence_cm7_");
  sb.append(mcr_id.getTypeId().toLowerCase());
  mcr_index_class = conf.getString(sb.toString()); 
  sb.append("_ts");
  mcr_ts_index = conf.getString(sb.toString());
  DKDatastoreDL connection = null;
  try {
    connection = MCRCM7ConnectionPool.instance().getConnection();
    MCRCM7Item item = getItem(mcr_id.getId(),mcr_index_class,connection);
    item.delete();
    updateTextIndexQueue(connection,mcr_ts_index);
    logger.info("Item "+mcr_id.getId()+" was deleted.");
    }
  catch (MCRPersistenceException e) {
    throw new MCRPersistenceException(
      "A object with ID "+mcr_id.getId()+" does not exists."); }
  catch (Exception e) {
    throw new MCRPersistenceException(e.getMessage(),e); }
  finally {
    MCRCM7ConnectionPool.instance().releaseConnection(connection); }
  }

/**
 * The methode return true if an object  for the MCRObjectId exists.
 * The index class is determinated by the type
 * of the object ID. This <b>must</b> correspond with the lower case 
 * configuration name.<br>
 * As example: Document --> MCR.persistence_cm7_document
 *
 * @param mcr_id      the object id
 * @return true if the object exists, else false
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final boolean exist(MCRObjectID mcr_id)
  throws MCRConfigurationException, MCRPersistenceException
  {
  StringBuffer sb = new StringBuffer("MCR.persistence_cm7_");
  sb.append(mcr_id.getTypeId().toLowerCase());
  mcr_index_class = conf.getString(sb.toString()); 
  try {
    DKDatastoreDL connection = null;
    try {
      connection = MCRCM7ConnectionPool.instance().getConnection();
      try {
        MCRCM7Item item = getItem(mcr_id.getId(),mcr_index_class,connection); }
      catch (MCRPersistenceException e) { return false; }
      }
    finally {
      MCRCM7ConnectionPool.instance().releaseConnection(connection); }
    }
  catch (Exception e) {
    throw new MCRPersistenceException(e.getMessage()); }
  return true;
  }

/**
 * The methode receive an object from the data store and return the object
 * as a XML stream. The index class
 * is determinated by the type of the object ID. This <b>must</b>
 * correspond with the lower case configuration name.<br>
 * As example: Document --> MCR.persistence_cm7_document
 *
 * @param mcr_id      the object id
 * @return the XML stream of the object as string
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final byte [] receive(MCRObjectID mcr_id)
  throws MCRConfigurationException, MCRPersistenceException
  {
  StringBuffer sb = new StringBuffer("MCR.persistence_cm7_");
  sb.append(mcr_id.getTypeId().toLowerCase());
  mcr_index_class = conf.getString(sb.toString()); 
  DKDatastoreDL connection = null;
  byte [] xml = null;
  try {
    connection = MCRCM7ConnectionPool.instance().getConnection();
    try {
      MCRCM7Item item = getItem(mcr_id.getId(),mcr_index_class,connection);
      xml = item.getPartToBytes(mcr_xml_part);
      }
    catch (MCRPersistenceException e) {
      throw new MCRPersistenceException(
        "A object with ID "+mcr_id.getId()+" does not exists."); }
    }
  catch (Exception e) {
    throw new MCRPersistenceException(e.getMessage(),e); }
  finally {
    MCRCM7ConnectionPool.instance().releaseConnection(connection); }
  return xml;
  }

/**
 * The methode update an object in the data store. The index class
 * is determinated by the type of the object ID. This <b>must</b>
 * correspond with the lower case configuration name.<br>
 * As example: Document --> MCR.persistence_cm7_document
 *
 * @param mcr_tc      the typed content array
 * @param xml         the XML stream from the object as JDOM
 * @param mcr_ts_in   the text search string
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void update(MCRTypedContent mcr_tc, org.jdom.Document jdom,
  String mcr_ts_in) throws MCRConfigurationException, MCRPersistenceException
  {
  Logger logger = MCRCM7ConnectionPool.getLogger();
  // convert the JDOM tree
  byte [] xml = MCRUtils.getByteArray(jdom);
  // extract index data from typed content
  MCRObjectID mcr_id = null;
  String mcr_label = null;
  String mcr_flags = "";
  GregorianCalendar mcr_create = null;
  GregorianCalendar mcr_modify = null;
  String mcr_ts = null;
  for (int i=0;i<mcr_tc.getSize();i++) {
    if (mcr_tc.getNameElement(i).equals("ID")) {
      mcr_id = new MCRObjectID((String)mcr_tc.getValueElement(i)); 
      mcr_label = (String)mcr_tc.getValueElement(i+1); }
    }
  for (int i=0;i<mcr_tc.getSize();i++) {
    if (mcr_tc.getNameElement(i).toLowerCase().equals("service")) {
      for (int j=i;j<mcr_tc.getSize();j++) {
        if (mcr_tc.getNameElement(j).toLowerCase().equals("servdate")) {
          if (mcr_tc.getValueElement(j+3).equals("createdate")) {
            mcr_create = (GregorianCalendar) mcr_tc.getValueElement(j+1);
            continue; }
          if (mcr_tc.getValueElement(j+3).equals("modifydate")) {
            mcr_modify = (GregorianCalendar) mcr_tc.getValueElement(j+1);
            continue; }
          }
        if (mcr_tc.getNameElement(j).toLowerCase().equals("servflag")) {
          mcr_flags = mcr_flags + " " + (String)mcr_tc.getValueElement(j+1); }
        }
      break;
      }
    }
  mcr_ts = createTS(mcr_tc)+NL+mcr_ts_in.toUpperCase();
  // read configuration
  StringBuffer sb = new StringBuffer("MCR.persistence_cm7_");
  sb.append(mcr_id.getTypeId().toLowerCase());
  mcr_index_class = conf.getString(sb.toString()); 
  sb.append("_ts");
  mcr_ts_index = conf.getString(sb.toString());
  // store the data
  try {
    DKDatastoreDL connection = null;
    if ((mcr_id_name == null) || (mcr_label_name == null) || 
        (mcr_flag_name == null) ||
        (mcr_create_name == null) || (mcr_modify_name == null)) {
      throw new MCRConfigurationException("Indexclass field name is false."); }
    try {
      connection = MCRCM7ConnectionPool.instance().getConnection();
      try {
        MCRCM7Item item = getItem(mcr_id.getId(),mcr_index_class,
          connection);
        item.setKeyfield(mcr_id_name,mcr_id.getId());
        item.setKeyfield(mcr_label_name,mcr_label);
        item.setKeyfield(mcr_flag_name,mcr_flags);
        item.setKeyfield(mcr_create_name,mcr_create);
        item.setKeyfield(mcr_modify_name,mcr_modify);
        item.setPart(mcr_xml_part,xml,xml.length);
        item.setPart(mcr_ts_part,mcr_ts,mcr_ts_server,mcr_ts_index,mcr_ts_lang);
        item.update();
        updateTextIndexQueue(connection,mcr_ts_index);
        logger.info("Item "+mcr_id.getId()+" was updated.");
        }
      catch (MCRPersistenceException e) { 
        throw new MCRPersistenceException(
          "A object with ID "+mcr_id.getId()+" does not exists."); }
      }
    finally {
      MCRCM7ConnectionPool.instance().releaseConnection(connection); }
    }
  catch (Exception e) {
    throw new MCRPersistenceException(e.getMessage()); }
  }

/**
 * Get a DDO
 **/
private final MCRCM7Item getItem(String id, String indexclass, 
  DKDatastoreDL connection) throws Exception
  {
  StringBuffer sb = new StringBuffer(128);
  sb.append('\'').append(mcr_id_name).append("' == \"")
    .append(id.trim().toUpperCase()).append('\"');
  return new MCRCM7Item(sb.toString(),mcr_index_class,connection );
  }

/**
 * Update the Text Search Queue
 **/
private final void updateTextIndexQueue(DKDatastoreDL connection, 
  String mcr_ts_index)
  {
  try {
    connection.invokeSearchEngine("SM",mcr_ts_server+"-"+mcr_ts_index); }
  catch( Exception ex ) {
    Logger logger = MCRCM7ConnectionPool.getLogger();
    logger.error( "Problem while calling TSE user exit." );
    }
  finally { 
    MCRCM7ConnectionPool.instance().releaseConnection(connection); }

  DKDatastoreTS connectionTSE = null;
  try {
    connectionTSE = new DKDatastoreTS();
    connectionTSE.connect( mcr_ts_server, "", "", "" );
    DKDatastoreDefTS definition = 
      (DKDatastoreDefTS)( connectionTSE.datastoreDef() );
    DKDatastoreAdminTS administration = 
      (DKDatastoreAdminTS)( definition.datastoreAdmin() );
      administration.startUpdateIndex(mcr_ts_index);
    }
  catch( Exception ex ) { 
    Logger logger = MCRCM7ConnectionPool.getLogger();
    logger.error("Problem while starting text search index update."); 
    }
  finally {
    if( connectionTSE != null ) 
    try{ connectionTSE.disconnect(); }
    catch( Exception ignored ){}
    }
  }

/**
 * Create the TextSearch from MCRTypedContent.
 **/
private final String createTS(MCRTypedContent mcr_tc)
  {
  String NL = System.getProperty("line.separator");
  StringBuffer sb = new StringBuffer(2048);
  MCRCM7TransformToText ttt = new MCRCM7TransformToText();
  int maxtag = 0;
  int tagdiff = MCRTypedContent.TYPE_LASTTAG-MCRTypedContent.TYPE_MASTERTAG+2;
  String  [] tag = new String [tagdiff];
  tag[0] = new String("MYCOREOBJECT");
  // MCRObject data
  sb.append("XXX").append(mcr_tc.getNameElement(0).toUpperCase()).append("XXX")
    .append("XXX").append(mcr_tc.getNameElement(1).toUpperCase()).append("XXX")
    .append(ttt.createSearchStringAttr((String)mcr_tc.getValueElement(1)))
    .append("XXX").append(NL);
  sb.append("XXX").append(mcr_tc.getNameElement(0).toUpperCase()).append("XXX")
    .append("XXX").append(mcr_tc.getNameElement(2).toUpperCase()).append("XXX")
    .append(ttt.createSearchStringAttr((String)mcr_tc.getValueElement(2)))
    .append("XXX").append(NL);
  int i = 3; 
  while (i<mcr_tc.getSize()) {
    if (mcr_tc.getTypeElement(i)>=MCRTypedContent.TYPE_MASTERTAG) {   
      tagdiff = mcr_tc.getTypeElement(i) - MCRTypedContent.TYPE_MASTERTAG+1;
      tag[tagdiff] = ((String)mcr_tc.getNameElement(i)).toUpperCase();
      maxtag = tagdiff + 1;
      i++; continue; 
      }
    if (mcr_tc.getTypeElement(i)==MCRTypedContent.TYPE_VALUE) {   
      if (mcr_tc.getFormatElement(i)==MCRTypedContent.FORMAT_BOOLEAN) {
        sb.append("XXX");
        for (int j=0;j<maxtag;j++) { sb.append(tag[j]).append("XXX"); }
        int j = i+1;
        int k = 1;
        while (mcr_tc.getTypeElement(j)==MCRTypedContent.TYPE_ATTRIBUTE) {
          if (!mcr_tc.getNameElement(j).equals("lang")) {
            sb.append("XXX")
              .append(mcr_tc.getNameElement(j).toUpperCase()).append("XXX")
              .append(((String)mcr_tc.getValueElement(j)).toUpperCase())
              .append("XXX"); }
          j++; 
          if (j>=mcr_tc.getSize()) { break; } else { k++; }
          }
        sb.append(ttt.createSearchStringBoolean(((Boolean)mcr_tc
            .getValueElement(i)).booleanValue()));
        sb.append(NL);
        i += k; continue;
        }
      if (mcr_tc.getFormatElement(i)==MCRTypedContent.FORMAT_DATE) {
        sb.append("XXX");
        for (int j=0;j<maxtag;j++) { sb.append(tag[j]).append("XXX"); }
        int j = i+1;
        int k = 1;
        while (mcr_tc.getTypeElement(j)==MCRTypedContent.TYPE_ATTRIBUTE) {
          if (!mcr_tc.getNameElement(j).equals("lang")) {
            sb.append("XXX")
              .append(mcr_tc.getNameElement(j).toUpperCase()).append("XXX")
              .append(((String)mcr_tc.getValueElement(j)).toUpperCase())
              .append("XXX"); }
          j++; 
          if (j>=mcr_tc.getSize()) { break; } else { k++; }
          }
        sb.append(ttt.createSearchStringDate((GregorianCalendar)mcr_tc
            .getValueElement(i)));
        sb.append(NL);
        i += k; continue;
        }
      if (mcr_tc.getFormatElement(i)==MCRTypedContent.FORMAT_NUMBER) {
        sb.append("XXX");
        for (int j=0;j<maxtag;j++) { sb.append(tag[j]).append("XXX"); }
        int j = i+1;
        int k = 1;
        while (mcr_tc.getTypeElement(j)==MCRTypedContent.TYPE_ATTRIBUTE) {
          if (!mcr_tc.getNameElement(j).equals("lang")) {
            sb.append("XXX")
              .append(mcr_tc.getNameElement(j).toUpperCase()).append("XXX")
              .append(((String)mcr_tc.getValueElement(j)).toUpperCase())
              .append("XXX"); }
          j++; 
          if (j>=mcr_tc.getSize()) { break; } else { k++; }
          }
        sb.append(ttt.createSearchStringDouble(((Double)mcr_tc
            .getValueElement(i)).doubleValue()));
        sb.append(NL);
        i += k; continue;
        }
      if (mcr_tc.getFormatElement(i)==MCRTypedContent.FORMAT_STRING) {
        sb.append("XXX");
        for (int j=0;j<maxtag;j++) { sb.append(tag[j]).append("XXX"); }
        int j = i+1;
        int k = 1;
        while (mcr_tc.getTypeElement(j)==MCRTypedContent.TYPE_ATTRIBUTE) {
          if (!mcr_tc.getNameElement(j).equals("lang")) {
            sb.append("XXX")
              .append(mcr_tc.getNameElement(j).toUpperCase()).append("XXX")
              .append(((String)mcr_tc.getValueElement(j)).toUpperCase())
              .append("XXX"); }
          j++;
          if (j>=mcr_tc.getSize()) { break; } else { k++; }
          }
        sb.append(' ').append(ttt.createSearchStringText((String)mcr_tc
            .getValueElement(i)));
        sb.append(NL);
        i += k; continue;
        }
      }
    if (mcr_tc.getTypeElement(i)==MCRTypedContent.TYPE_ATTRIBUTE) {   
      if (mcr_tc.getFormatElement(i)==MCRTypedContent.FORMAT_CLASSID) {
        sb.append("XXX");
        for (int j=0;j<maxtag;j++) { sb.append(tag[j]).append("XXX"); }
        sb.append("XXX")
          .append(mcr_tc.getNameElement(i).toUpperCase()).append("XXX")
          .append(((String)mcr_tc.getValueElement(i)).toUpperCase()
            .replace('.','X').replace('_','X'))
          .append("XXX"); 
        i++;
        sb.append("XXX")
          .append(mcr_tc.getNameElement(i).toUpperCase()).append("XXX")
          .append(((String)mcr_tc.getValueElement(i)).toUpperCase()
            .replace('.','X').replace('_','X'))
          .append("XXX"); 
        i++;
        sb.append(NL);
        continue;
        }
      if (mcr_tc.getFormatElement(i)==MCRTypedContent.FORMAT_LINK) {
        sb.append("XXX");
        for (int j=0;j<maxtag;j++) { sb.append(tag[j]).append("XXX"); }
/*
        sb.append("XXX")
          .append(ttt.createSearchStringText((String)mcr_tc
            .getValueElement(i)))
          .append("XXX");
*/
        if (((String)mcr_tc.getValueElement(i)).toLowerCase()
          .equals("locator")) {
          sb.append("XXXHREFXXX")
            .append(ttt.createSearchStringText((String)mcr_tc
              .getValueElement(i+1)))
            .append("XXX ")
/*
            .append(ttt.createSearchStringText((String)mcr_tc
              .getValueElement(i+2)))
*/
            .append(' ')
            .append(ttt.createSearchStringText((String)mcr_tc
              .getValueElement(i+3)));
          }
        else {
          sb.append("XXXFROMXXX")
            .append(ttt.createSearchStringText((String)mcr_tc
              .getValueElement(i+1)))
            .append("XXX")
            .append("XXXTOXXX")
            .append(ttt.createSearchStringText((String)mcr_tc
              .getValueElement(i+2)))
            .append("XXX ")
            .append(' ')
            .append(ttt.createSearchStringText((String)mcr_tc
              .getValueElement(i+3)));
          }
        sb.append(NL);
        i += 4; continue;
        }
      }
    i++;
    }
  return sb.toString();
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
  public synchronized String getNextFreeId( String project_ID, String type_ID ) 
    throws MCRPersistenceException
  { 
    String prefix     = project_ID + "_" + type_ID;
    String property   = "MCR.persistence_cm7_" + type_ID.toLowerCase().trim();
    String indexclass = conf.getString( property );

    // What table and column name is this in DB2?
    String table  = MCRCM7Bypass.getTableName ( indexclass  );
    String column = MCRCM7Bypass.getColumnName( mcr_id_name );

    int storedID = 0;

    // If there are any entries in the index class, get the highest ID stored:

    if( MCRSQLConnection.justCheckExists( table ) )
    {
      int offset = prefix.length() + 2;
      String query = new StringBuffer()
        .append( "SELECT MAX( INTEGER( SUBSTR( " )
        .append( column )
        .append( ", " )
        .append( offset )
        .append( " ))) FROM " )
        .append( table ).toString();
      storedID = Integer.parseInt( MCRSQLConnection.justGetSingleValue( query ) );
    }

    // Build a new ID from the highest stored and the highest returned so far:

    String ID = highestIDs.getProperty( prefix );
    int memoryID = ( ID == null ? 0 : Integer.parseInt( ID ) );

    ID = String.valueOf( Math.max( storedID, memoryID ) + 1 );
    highestIDs.put( prefix, ID );

    return ID;
  }

  /** This table stores the highest IDs delivered by getNextFreeId() */
  protected static Properties highestIDs = new Properties();

/**
 * This methode let a system command run.
 *
 * @param command        the system command
 * @see IOException
 * @see InterruptedException
 **/
private static void exec (String command)
  throws IOException, InterruptedException
  {
  Runtime r = Runtime.getRuntime();
  Process p = r.exec(command);
  p.waitFor();
  }

}

