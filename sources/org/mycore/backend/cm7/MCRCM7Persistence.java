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

import java.io.*;
import java.util.*;
import com.ibm.mm.sdk.server.*;
import com.ibm.mm.sdk.common.*;
import mycore.common.MCRConfiguration;
import mycore.common.MCRConfigurationException;
import mycore.common.MCRPersistenceException;
import mycore.datamodel.MCRObjectID;
import mycore.datamodel.MCRObjectService;
import mycore.datamodel.MCRTypedContent;
import mycore.datamodel.MCRObjectPersistenceInterface;
import mycore.cm7.MCRCM7ConnectionPool;
import mycore.cm7.MCRCM7Item;
import mycore.sql.MCRSQLConnection;

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
private String mcr_id_name = null;
private String mcr_label_name = null;
private String mcr_flag_name = null;
private String mcr_create_name = null;
private String mcr_modify_name = null;
private String mcr_index_class = null;
private String mcr_ts_server = null;
private String mcr_ts_index = null;
private String mcr_ts_lang = null;
private int mcr_ts_part = 1;
private int mcr_xml_part = 2;

/**
 * The constructor of this class.
 **/
public MCRCM7Persistence()
  {
  mcr_id_name = MCRConfiguration.instance()
    .getString("MCR.persistence_cm7_field_id");
  mcr_label_name = MCRConfiguration.instance()
    .getString("MCR.persistence_cm7_field_label");
  mcr_flag_name = MCRConfiguration.instance()
    .getString("MCR.persistence_cm7_field_flag");
  mcr_create_name = MCRConfiguration.instance()
    .getString("MCR.persistence_cm7_field_datecreate");
  mcr_modify_name = MCRConfiguration.instance()
    .getString("MCR.persistence_cm7_field_datemodify");
  mcr_ts_part = MCRConfiguration.instance()
    .getInt("MCR.persistence_cm7_part_ts");
  mcr_xml_part = MCRConfiguration.instance()
    .getInt("MCR.persistence_cm7_part_xml");
  mcr_ts_server = MCRConfiguration.instance()
    .getString("MCR.persistence_cm7_textsearch_server");
  mcr_ts_lang = MCRConfiguration.instance()
    .getString("MCR.persistence_cm7_textsearch_lang");
  if ((mcr_id_name == null) || (mcr_label_name == null) || 
      (mcr_flag_name == null) ||
      (mcr_create_name == null) || (mcr_modify_name == null)) {
    throw new MCRConfigurationException("A indexclass field name is false."); }
  }

/**
 * The methode create an object in the data store. The index class
 * is determinated by the type of the object ID. This <b>must</b>
 * correspond with the lower case configuration name.<br>
 * As example: Document --> MCR.persistence_cm7_document
 *
 * @param mcr_tc      the typed content array
 * @param xml         the XML stream from the object
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void create(MCRTypedContent mcr_tc, String xml)
  throws MCRConfigurationException, MCRPersistenceException
  {
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
        if (mcr_tc.getNameElement(j).toLowerCase().equals("date")) {
          if (mcr_tc.getValueElement(j+3).equals("createdate")) {
            mcr_create = (GregorianCalendar) mcr_tc.getValueElement(j+1);
            continue; }
          if (mcr_tc.getValueElement(j+3).equals("modifydate")) {
            mcr_modify = (GregorianCalendar) mcr_tc.getValueElement(j+1);
            continue; }
          }
        if (mcr_tc.getNameElement(j).toLowerCase().equals("flag")) {
          mcr_flags = mcr_flags + " " + (String)mcr_tc.getValueElement(j+1); }
        }
      break;
      }
    }
  mcr_ts = createTS(mcr_tc);
  // read configuration
  StringBuffer sb = new StringBuffer("MCR.persistence_cm7_");
  sb.append(mcr_id.getTypeId().toLowerCase());
  mcr_index_class = MCRConfiguration.instance().getString(sb.toString()); 
  sb.append("_ts");
  mcr_ts_index = MCRConfiguration.instance().getString(sb.toString());
  // store the data
  try {
    DKDatastoreDL connection = null;
    try {
      connection = MCRCM7ConnectionPool.getConnection();
      try {
        MCRCM7Item checkitem = getItem(mcr_id.getId(),mcr_index_class,
          connection);
        throw new MCRPersistenceException(
          "A object with ID "+mcr_id.getId()+" exists."); }
      catch (MCRPersistenceException e) { }
      MCRCM7Item item = new MCRCM7Item(connection,mcr_index_class,
        DKConstant.DK_DOCUMENT);
      item.setKeyfield(mcr_id_name,mcr_id.getId());
      item.setKeyfield(mcr_label_name,mcr_label);
      item.setKeyfield(mcr_flag_name,mcr_flags);
      item.setKeyfield(mcr_create_name,mcr_create);
      item.setKeyfield(mcr_modify_name,mcr_modify);
      item.setPart(mcr_xml_part,xml);
      item.setPart(mcr_ts_part,mcr_ts,mcr_ts_server,mcr_ts_index,mcr_ts_lang);
      item.create();
      exec("imlupdix -s "+mcr_ts_server+" -x "+mcr_ts_index);
      }
    finally {
      MCRCM7ConnectionPool.releaseConnection(connection); }
    }
  catch (Exception e) {
    throw new MCRPersistenceException(e.getMessage()); }
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
  StringBuffer sb = new StringBuffer("MCR.persistence_cm7_");
  sb.append(mcr_id.getTypeId().toLowerCase());
  mcr_index_class = MCRConfiguration.instance().getString(sb.toString()); 
  sb.append("_ts");
  mcr_ts_index = MCRConfiguration.instance().getString(sb.toString());
  try {
    deleteCM7(mcr_id); }
  catch (Exception e) {
    throw new MCRPersistenceException(e.getMessage(),e); }
  }

/**
 * The methode delete internal a object from the data store.
 *
 * @param mcr_id      the object id
 * @exception DKException if an error in the CM7 is occured
 * @exception Exception if an general error is occured
 **/
private final void deleteCM7(MCRObjectID mcr_id)
  throws DKException, Exception
  {
  DKDatastoreDL connection = null;
  try {
    connection = MCRCM7ConnectionPool.getConnection();
    try {
      MCRCM7Item item = getItem(mcr_id.getId(),mcr_index_class,connection);
      item.delete();
      exec("imlupdix -s "+mcr_ts_server+" -x "+mcr_ts_index);
      }
    catch (MCRPersistenceException e) {
      throw new MCRPersistenceException(
        "A object with ID "+mcr_id.getId()+"does not exists."); }
    }
  finally {
    MCRCM7ConnectionPool.releaseConnection(connection); }
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
public final String receive(MCRObjectID mcr_id)
  throws MCRConfigurationException, MCRPersistenceException
  {
  StringBuffer sb = new StringBuffer("MCR.persistence_cm7_");
  sb.append(mcr_id.getTypeId().toLowerCase());
  mcr_index_class = MCRConfiguration.instance().getString(sb.toString()); 
  try {
    return receiveCM7(mcr_id); }
  catch (Exception e) {
    throw new MCRPersistenceException(e.getMessage(),e); }
  }

/**
 * The methode receive internal a object from the data store.
 *
 * @param mcr_id      the object id
 * @exception DKException if an error in the CM7 is occured
 * @exception Exception if an general error is occured
 **/
private final String receiveCM7(MCRObjectID mcr_id)
  throws DKException, Exception
  {
  DKDatastoreDL connection = null;
  String xml = new String();
  try {
    connection = MCRCM7ConnectionPool.getConnection();
    try {
      MCRCM7Item item = getItem(mcr_id.getId(),mcr_index_class,connection);
      xml = item.getPart(mcr_xml_part);
      }
    catch (MCRPersistenceException e) {
      throw new MCRPersistenceException(
        "A object with ID "+mcr_id.getId()+"does not exists."); }
    }
  finally {
    MCRCM7ConnectionPool.releaseConnection(connection); }
  return xml;
  }

/**
 * The methode receive an object from the data store and return the 
 * creation data the object. The index class is determinated by the type
 * of the object ID. This <b>must</b> correspond with the lower case 
 * configuration name.<br>
 * As example: Document --> MCR.persistence_cm7_document
 *
 * @param mcr_id      the object id
 * @return the GregorianCalendar data of the object
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final GregorianCalendar receiveCreateDate(MCRObjectID mcr_id)
  throws MCRConfigurationException, MCRPersistenceException
  {
  StringBuffer sb = new StringBuffer("MCR.persistence_cm7_");
  sb.append(mcr_id.getTypeId().toLowerCase());
  mcr_index_class = MCRConfiguration.instance().getString(sb.toString()); 
  try {
    return receiveCreateDateCM7(mcr_id); }
  catch (Exception e) {
    throw new MCRPersistenceException(e.getMessage(),e); }
  }

/**
 * The methode receive internal a GregorianCalendar object from the data store.
 *
 * @param mcr_id      the object id
 * @return the GregorianCalendar data of the object
 * @exception DKException if an error in the CM7 is occured
 * @exception Exception if an general error is occured
 **/
private final GregorianCalendar receiveCreateDateCM7(MCRObjectID mcr_id)
  throws DKException, Exception
  {
  DKDatastoreDL connection = null;
  GregorianCalendar create = new GregorianCalendar();
  try {
    connection = MCRCM7ConnectionPool.getConnection();
    try {
      MCRCM7Item item = getItem(mcr_id.getId(),mcr_index_class,connection);
      create = item.getKeyfieldToDate(mcr_create_name);
      }
    catch (MCRPersistenceException e) {
      throw new MCRPersistenceException(
        "A object with ID "+mcr_id.getId()+"does not exists."); }
    }
  finally {
    MCRCM7ConnectionPool.releaseConnection(connection); }
  return create;
  }

/**
 * The methode receive an object from the data store and return the 
 * label of the object. The index class is determinated by the type
 * of the object ID. This <b>must</b> correspond with the lower case 
 * configuration name.<br>
 * As example: Document --> MCR.persistence_cm7_document
 *
 * @param mcr_id      the object id
 * @return the label of the object
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final String receiveLabel(MCRObjectID mcr_id)
  throws MCRConfigurationException, MCRPersistenceException
  {
  StringBuffer sb = new StringBuffer("MCR.persistence_cm7_");
  sb.append(mcr_id.getTypeId().toLowerCase());
  mcr_index_class = MCRConfiguration.instance().getString(sb.toString()); 
  String label = new String("");
  try {
    DKDatastoreDL connection = null;
    try {
      connection = MCRCM7ConnectionPool.getConnection();
      try {
        MCRCM7Item item = getItem(mcr_id.getId(),mcr_index_class,connection);
        label = item.getKeyfieldToString(mcr_label_name);
        }
      catch (MCRPersistenceException e) {
        throw new MCRPersistenceException(
          "A object with ID "+mcr_id.getId()+"does not exists."); }
      }
    finally {
      MCRCM7ConnectionPool.releaseConnection(connection); }
    }
  catch (Exception e) {
    throw new MCRPersistenceException(e.getMessage()); }
  return label;
  }

/**
 * The methode update an object in the data store. The index class
 * is determinated by the type of the object ID. This <b>must</b>
 * correspond with the lower case configuration name.<br>
 * As example: Document --> MCR.persistence_cm7_document
 *
 * @param mcr_tc      the typed content array
 * @param xml         the XML stream from the object
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void update(MCRTypedContent mcr_tc, String xml)
  throws MCRConfigurationException, MCRPersistenceException
  {
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
        if (mcr_tc.getNameElement(j).toLowerCase().equals("date")) {
          if (mcr_tc.getValueElement(j+3).equals("createdate")) {
            mcr_create = (GregorianCalendar) mcr_tc.getValueElement(j+1);
            continue; }
          if (mcr_tc.getValueElement(j+3).equals("modifydate")) {
            mcr_modify = (GregorianCalendar) mcr_tc.getValueElement(j+1);
            continue; }
          }
        if (mcr_tc.getNameElement(j).toLowerCase().equals("flag")) {
          mcr_flags = mcr_flags + " " + (String)mcr_tc.getValueElement(j+1); }
        }
      break;
      }
    }
  mcr_ts = createTS(mcr_tc);
  // read configuration
  StringBuffer sb = new StringBuffer("MCR.persistence_cm7_");
  sb.append(mcr_id.getTypeId().toLowerCase());
  mcr_index_class = MCRConfiguration.instance().getString(sb.toString()); 
  sb.append("_ts");
  mcr_ts_index = MCRConfiguration.instance().getString(sb.toString());
  // store the data
  try {
    DKDatastoreDL connection = null;
    if ((mcr_id_name == null) || (mcr_label_name == null) || 
        (mcr_flag_name == null) ||
        (mcr_create_name == null) || (mcr_modify_name == null)) {
      throw new MCRConfigurationException("Indexclass field name is false."); }
    try {
      connection = MCRCM7ConnectionPool.getConnection();
      try {
        MCRCM7Item item = getItem(mcr_id.getId(),mcr_index_class,
          connection);
        item.setKeyfield(mcr_id_name,mcr_id.getId());
        item.setKeyfield(mcr_label_name,mcr_label);
        item.setKeyfield(mcr_flag_name,mcr_flags);
        item.setKeyfield(mcr_create_name,mcr_create);
        item.setKeyfield(mcr_modify_name,mcr_modify);
        item.setPart(mcr_xml_part,xml);
        item.setPart(mcr_ts_part,mcr_ts,mcr_ts_server,mcr_ts_index,mcr_ts_lang);
        item.update();
        exec("imlupdix -s "+mcr_ts_server+" -x "+mcr_ts_index);
        }
      catch (MCRPersistenceException e) { 
        throw new MCRPersistenceException(
          "A object with ID "+mcr_id.getId()+"does not exists."); }
      }
    finally {
      MCRCM7ConnectionPool.releaseConnection(connection); }
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
 * Create the TextSearch from MCRTypedContent.
 **/
private final String createTS(MCRTypedContent mcr_tc)
  {
  String NL = System.getProperty("line.separator");
  StringBuffer sb = new StringBuffer(2048);
  MCRCM7TransformToText ttt = new MCRCM7TransformToText();
  int maxtag = 0;
  int tagdiff = MCRTypedContent.TYPE_LASTTAG-MCRTypedContent.TYPE_MASTERTAG+1;
  String  [] tag = new String [tagdiff];
  // MCRObject data
  sb.append("XXX").append(mcr_tc.getNameElement(0))
    .append("XXX").append(mcr_tc.getNameElement(1)).append("XXX ")
    .append(ttt.createSearchStringText((String)mcr_tc.getValueElement(1)))
    .append(NL);
  sb.append("XXX").append(mcr_tc.getNameElement(0))
    .append("XXX").append(mcr_tc.getNameElement(2)).append("XXX ")
    .append(ttt.createSearchStringText((String)mcr_tc.getValueElement(2)))
    .append(NL);
  int i = 3; 
  while (i<mcr_tc.getSize()) {
    if (mcr_tc.getTypeElement(i)>=MCRTypedContent.TYPE_MASTERTAG) {   
      tagdiff = mcr_tc.getTypeElement(i) - MCRTypedContent.TYPE_MASTERTAG;
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
          if (!mcr_tc.getNameElement(j).equals("xml:lang")) {
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
          if (!mcr_tc.getNameElement(j).equals("xml:lang")) {
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
      if (mcr_tc.getFormatElement(i)==MCRTypedContent.FORMAT_LINK) {
        sb.append("XXX");
        for (int j=0;j<maxtag;j++) { sb.append(tag[j]).append("XXX"); }
        sb.append("XXX")
          .append(ttt.createSearchStringText((String)mcr_tc
            .getValueElement(i+1)))
          .append("XXX ");
        if (((String)mcr_tc.getValueElement(i+1)).toLowerCase()
          .equals("locator")) {
          sb.append("XXXHREFXXX")
            .append(ttt.createSearchStringText((String)mcr_tc
              .getValueElement(i+2)))
            .append("XXX ")
            .append(ttt.createSearchStringText((String)mcr_tc
              .getValueElement(i+3)))
            .append(' ')
            .append(ttt.createSearchStringText((String)mcr_tc
              .getValueElement(i+4)));
          }
        else {
          sb.append("XXXFROMXXX")
            .append(ttt.createSearchStringText((String)mcr_tc
              .getValueElement(i+2)))
            .append("XXX ")
            .append("XXXTOXXX")
            .append(ttt.createSearchStringText((String)mcr_tc
              .getValueElement(i+3)))
            .append("XXX ")
            .append(ttt.createSearchStringText((String)mcr_tc
              .getValueElement(i+4)));
          }
        sb.append(NL);
        i += 5; continue;
        }
      if (mcr_tc.getFormatElement(i)==MCRTypedContent.FORMAT_NUMBER) {
        sb.append("XXX");
        for (int j=0;j<maxtag;j++) { sb.append(tag[j]).append("XXX"); }
        int j = i+1;
        int k = 1;
        while (mcr_tc.getTypeElement(j)==MCRTypedContent.TYPE_ATTRIBUTE) {
          if (!mcr_tc.getNameElement(j).equals("xml:lang")) {
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
          if (!mcr_tc.getNameElement(j).equals("xml:lang")) {
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
    String indexclass = MCRConfiguration.instance().getString( property );

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

