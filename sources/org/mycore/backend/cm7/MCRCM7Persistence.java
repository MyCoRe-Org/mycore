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
import mycore.datamodel.MCRObjectPersistenceInterface;
import mycore.cm7.MCRCM7ConnectionPool;
import mycore.cm7.MCRCM7Item;

/**
 * This class implements all methode for handling the data to the data store
 * based on IBM Content Manager 7.
 *
 * @author Jens Kupferschmidt
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
  }

/**
 * The methode create an object in the data store. The index class
 * is determinated by the type of the object ID. This <b>must</b>
 * correspond with the lower case configuration name.<br>
 * As example: Document --> MCR.persistence_cm7_document
 *
 * @param mcr_id      the object id
 * @param mcr_label   the object label
 * @param mcr_service the service class for the object
 * @param xml         the XML stream from the object
 * @param ts          the text search stream from the object
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void create(MCRObjectID mcr_id, String mcr_label, 
  MCRObjectService mcr_service, String xml, String ts) 
  throws MCRConfigurationException, MCRPersistenceException
  {
  StringBuffer sb = new StringBuffer("MCR.persistence_cm7_");
  sb.append(mcr_id.getTypeId().toLowerCase());
  mcr_index_class = MCRConfiguration.instance().getString(sb.toString()); 
  sb.append("_ts");
  mcr_ts_index = MCRConfiguration.instance().getString(sb.toString());
  try {
    createCM7(mcr_id,mcr_label,mcr_service,xml,ts); }
  catch (Exception e) {
    throw new MCRPersistenceException(e.getMessage(),e); }
  }

/**
 * The methode create internal a object in the data store.
 *
 * @param mcr_id      the object id
 * @param mcr_label   the object label
 * @param mcr_service the service class for the object
 * @param xml         the XML stream from the object
 * @param ts          the text search stream from the object
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception DKException if an error in the CM7 is occured
 * @exception Exception if an general error is occured
 **/
private final void createCM7(MCRObjectID mcr_id, String mcr_label,
  MCRObjectService mcr_service, String xml, String ts) 
  throws MCRConfigurationException, DKException, Exception
  {
  if ((mcr_id_name == null) || (mcr_label_name == null) || 
      (mcr_flag_name == null) ||
      (mcr_create_name == null) || (mcr_modify_name == null)) {
    throw new MCRConfigurationException("A indexclass field name is false."); }
  DKDatastoreDL connection = null;
  try {
    connection = MCRCM7ConnectionPool.getConnection();
    try {
      MCRCM7Item checkitem = getItem(mcr_id.getId(),mcr_index_class,
        connection);
      throw new MCRPersistenceException(
        "A object with ID "+mcr_id.getId()+" exists."); }
    catch (MCRCM7PersistenceException e) { }
    MCRCM7Item item = new MCRCM7Item(connection,mcr_index_class,
      DKConstant.DK_DOCUMENT);
    item.setKeyfield(mcr_id_name,mcr_id.getId());
    item.setKeyfield(mcr_label_name,mcr_label);
    item.setKeyfield(mcr_flag_name,mcr_service.getFlags());
    item.setKeyfield(mcr_create_name,mcr_service.getDate("createdate"));
    item.setKeyfield(mcr_modify_name,mcr_service.getDate("modifydate"));
    item.setPart(mcr_xml_part,xml);
    item.setPart(mcr_ts_part,ts,mcr_ts_server,mcr_ts_index,mcr_ts_lang);
    item.create();
    exec("imlupdix -s "+mcr_ts_server+" -x "+mcr_ts_index);
    }
  finally {
    MCRCM7ConnectionPool.releaseConnection(connection); }
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
    catch (MCRCM7PersistenceException e) {
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
    catch (MCRCM7PersistenceException e) {
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
    catch (MCRCM7PersistenceException e) {
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
  try {
    return receiveLabelCM7(mcr_id); }
  catch (Exception e) {
    throw new MCRPersistenceException(e.getMessage(),e); }
  }

/**
 * The methode receive internal a Label of an object from the data store.
 *
 * @param mcr_id      the object id
 * @return the label of the object
 * @exception DKException if an error in the CM7 is occured
 * @exception Exception if an general error is occured
 **/
private final String receiveLabelCM7(MCRObjectID mcr_id)
  throws DKException, Exception
  {
  DKDatastoreDL connection = null;
  String label = new String();
  try {
    connection = MCRCM7ConnectionPool.getConnection();
    try {
      MCRCM7Item item = getItem(mcr_id.getId(),mcr_index_class,connection);
      label = item.getKeyfieldToString(mcr_label_name);
      }
    catch (MCRCM7PersistenceException e) {
      throw new MCRPersistenceException(
        "A object with ID "+mcr_id.getId()+"does not exists."); }
    }
  finally {
    MCRCM7ConnectionPool.releaseConnection(connection); }
  return label;
  }

/**
 * The methode update an object in the data store. The index class
 * is determinated by the type of the object ID. This <b>must</b>
 * correspond with the lower case configuration name.<br>
 * As example: Document --> MCR.persistence_cm7_document
 *
 * @param mcr_id      the object id
 * @param mcr_label   the object label
 * @param mcr_service the service class for the object
 * @param xml         the XML stream from the object
 * @param ts          the text search stream from the object
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void update(MCRObjectID mcr_id, String mcr_label, 
  MCRObjectService mcr_service, String xml, String ts) 
  throws MCRConfigurationException, MCRPersistenceException
  {
  StringBuffer sb = new StringBuffer("MCR.persistence_cm7_");
  sb.append(mcr_id.getTypeId().toLowerCase());
  mcr_index_class = MCRConfiguration.instance().getString(sb.toString()); 
  sb.append("_ts");
  mcr_ts_index = MCRConfiguration.instance().getString(sb.toString());
  try {
    updateCM7(mcr_id,mcr_label,mcr_service,xml,ts); }
  catch (Exception e) {
    throw new MCRPersistenceException(e.getMessage(),e); }
  }

/**
 * The methode update internal a object in the data store.
 *
 * @param mcr_id      the object id
 * @param mcr_label   the object label
 * @param mcr_service the service class for the object
 * @param xml         the XML stream from the object
 * @param ts          the text search stream from the object
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception DKException if an error in the CM7 is occured
 * @exception Exception if an general error is occured
 **/
private final void updateCM7(MCRObjectID mcr_id, String mcr_label,
  MCRObjectService mcr_service, String xml, String ts) 
  throws  MCRConfigurationException, DKException, Exception
  {
  DKDatastoreDL connection = null;
  if ((mcr_id_name == null) || (mcr_label_name == null) || 
      (mcr_flag_name == null) ||
      (mcr_create_name == null) || (mcr_modify_name == null)) {
    throw new MCRConfigurationException("A indexclass field name is false."); }
  try {
    connection = MCRCM7ConnectionPool.getConnection();
    try {
      MCRCM7Item item = getItem(mcr_id.getId(),mcr_index_class,
        connection);
      item.setKeyfield(mcr_id_name,mcr_id.getId());
      item.setKeyfield(mcr_label_name,mcr_label);
      item.setKeyfield(mcr_flag_name,mcr_service.getFlags());
      item.setKeyfield(mcr_create_name,mcr_service.getDate("createdate"));
      item.setKeyfield(mcr_modify_name,mcr_service.getDate("modifydate"));
      item.setPart(mcr_xml_part,xml);
      item.setPart(mcr_ts_part,ts,mcr_ts_server,mcr_ts_index,mcr_ts_lang);
      item.update();
      exec("imlupdix -s "+mcr_ts_server+" -x "+mcr_ts_index);
      }
    catch (MCRCM7PersistenceException e) { 
      throw new MCRPersistenceException(
        "A object with ID "+mcr_id.getId()+"does not exists."); }
    }
  finally {
    MCRCM7ConnectionPool.releaseConnection(connection); }
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

