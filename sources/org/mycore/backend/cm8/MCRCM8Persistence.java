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

package mycore.cm8;

import java.io.*;
import java.util.*;
import com.ibm.mm.sdk.server.*;
import com.ibm.mm.sdk.common.*;
import mycore.common.MCRConfiguration;
import mycore.common.MCRConfigurationException;
import mycore.common.MCRPersistenceException;
import mycore.datamodel.MCRObject;
import mycore.datamodel.MCRObjectID;
import mycore.datamodel.MCRObjectStructure;
import mycore.datamodel.MCRObjectMetadata;
import mycore.datamodel.MCRObjectService;
import mycore.datamodel.MCRObjectPersistenceInterface;
import mycore.datamodel.MCRTypedContent;
import mycore.cm8.MCRCM8ConnectionPool;

/**
 * This class implements all methode for handling the data to the data store
 * based on IBM Content Manager 8.
 *
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 *
 * @version $Revision$ $Date$
 **/
public final class MCRCM8Persistence implements MCRObjectPersistenceInterface
{

// from configuration

/**
 * The constructor of this class.
 **/
public MCRCM8Persistence()
  {
  }

/**
 * The methode create an object in the data store. The index class
 * is determinated by the type of the object ID. This <b>must</b>
 * correspond with the lower case configuration name.<br>
 * As example: Document --> MCR.persistence_cm8_document
 *
 * @param mcr_tc      the typed content array
 * @param xml         the XML stream from the object
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void create(MCRTypedContent mcr_tc, byte [] xml)
  throws MCRConfigurationException, MCRPersistenceException
  {
  // get root data
  MCRObjectID mcr_id = null;
  String mcr_label = null;
  for (int i=0;i<mcr_tc.getSize();i++) {
    if (mcr_tc.getNameElement(i).equals("ID")) {
      mcr_id = new MCRObjectID((String)mcr_tc.getValueElement(i)); 
      mcr_label = (String)mcr_tc.getValueElement(i+1); }
    }
  // Read the item type name from the configuration
  StringBuffer sb = new StringBuffer("MCR.persistence_cm8_");
  sb.append(mcr_id.getTypeId().toLowerCase());
  String itemtypename = MCRConfiguration.instance().getString(sb.toString()); 
  // set up data to item
  DKDatastoreICM connection = null;
  try {
    connection = MCRCM8ConnectionPool.getConnection();
    boolean test = false;
    try {
      MCRCM8Item checkitem = new MCRCM8Item(mcr_id.getId(),connection,
        itemtypename); 
      test = true; }
    catch (MCRPersistenceException e) { }
    if (test) {
      throw new MCRPersistenceException("A object with ID "+mcr_id.getId()+
        " exists."); }
    MCRCM8Item item = new MCRCM8Item(connection,itemtypename);
    item.setAttribute("/","ID",mcr_id.getId());
    item.setAttribute("/","label",mcr_label);
    item.setAttribute("/","xml",xml);
    item.create();
    System.out.println("Item was created.");
    }
  catch (Exception e) {
    throw new MCRPersistenceException(e.getMessage()); }
  finally {
    MCRCM8ConnectionPool.releaseConnection(connection); }
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
  { MCRCM8ItemType.create(mcr_type,mcr_conf); }

/**
 * The methode delete an object from the data store. The index class
 * is determinated by the type of the object ID. This <b>must</b>
 * correspond with the lower case configuration name.<br>
 * As example: Document --> MCR.persistence_cm8_document
 *
 * @param mcr_id      the object id
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void delete(MCRObjectID mcr_id)
  throws MCRConfigurationException, MCRPersistenceException
  {
  // Read the item type name from the configuration
  StringBuffer sb = new StringBuffer("MCR.persistence_cm8_");
  sb.append(mcr_id.getTypeId().toLowerCase());
  String itemtypename = MCRConfiguration.instance().getString(sb.toString()); 
  // delete data item
  DKDatastoreICM connection = null;
  try {
    connection = MCRCM8ConnectionPool.getConnection();
    MCRCM8Item item = null;
    try {
      item = new MCRCM8Item(mcr_id.getId(),connection,itemtypename); }
    catch (MCRPersistenceException e) {
      throw new MCRPersistenceException("A object with ID "+mcr_id.getId()+
        " does not exist."); }
    item.delete();
    System.out.println("Item was deleted.");
    }
  catch (Exception e) {
    throw new MCRPersistenceException(e.getMessage()); }
  finally {
    MCRCM8ConnectionPool.releaseConnection(connection); }
  }

/**
 * The methode receive an object from the data store and return the object
 * as a XML stream. The index class
 * is determinated by the type of the object ID. This <b>must</b>
 * correspond with the lower case configuration name.<br>
 * As example: Document --> MCR.persistence_cm8_document
 *
 * @param mcr_id      the object id
 * @return the XML stream of the object as string
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final byte[] receive(MCRObjectID mcr_id)
  throws MCRConfigurationException, MCRPersistenceException
  {
  byte [] xml = null;
  // Read the item type name from the configuration
  StringBuffer sb = new StringBuffer("MCR.persistence_cm8_");
  sb.append(mcr_id.getTypeId().toLowerCase());
  String itemtypename = MCRConfiguration.instance().getString(sb.toString()); 
  // retrieve the XML byte stream
  DKDatastoreICM connection = null;
  try {
    connection = MCRCM8ConnectionPool.getConnection();
    MCRCM8Item item = null;
    try {
      item = new MCRCM8Item(mcr_id.getId(),connection,itemtypename);
      item.retrieve();
      xml = item.getBlob("/","xml");
      }
    catch (MCRPersistenceException e) {
      throw new MCRPersistenceException("A object with ID "+mcr_id.getId()+
        " does not exist."); }
    }
  catch (Exception e) {
    throw new MCRPersistenceException(e.getMessage()); }
  finally {
    MCRCM8ConnectionPool.releaseConnection(connection); }
  return xml;
  }

/**
 * The methode receive an object from the data store and return the 
 * creation data the object. The index class is determinated by the type
 * of the object ID. This <b>must</b> correspond with the lower case 
 * configuration name.<br>
 * As example: Document --> MCR.persistence_cm8_document
 *
 * @param mcr_id      the object id
 * @return the GregorianCalendar data of the object
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final GregorianCalendar receiveCreateDate(MCRObjectID mcr_id)
  throws MCRConfigurationException, MCRPersistenceException
  {
  GregorianCalendar create = new GregorianCalendar();
  DKDatastoreICM connection = null;
  try {
    connection = MCRCM8ConnectionPool.getConnection();
    }
  catch (Exception e) {
    throw new MCRPersistenceException(e.getMessage()); }
  finally {
    MCRCM8ConnectionPool.releaseConnection(connection); }
  return create;
  }

/**
 * The methode receive an object from the data store and return the 
 * label of the object. The index class is determinated by the type
 * of the object ID. This <b>must</b> correspond with the lower case 
 * configuration name.<br>
 * As example: Document --> MCR.persistence_cm8_document
 *
 * @param mcr_id      the object id
 * @return the label of the object
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final String receiveLabel(MCRObjectID mcr_id)
  throws MCRConfigurationException, MCRPersistenceException
  {
  String label = new String("");
  // Read the item type name from the configuration
  StringBuffer sb = new StringBuffer("MCR.persistence_cm8_");
  sb.append(mcr_id.getTypeId().toLowerCase());
  String itemtypename = MCRConfiguration.instance().getString(sb.toString()); 
  // receive the label
  DKDatastoreICM connection = null;
  try {
    connection = MCRCM8ConnectionPool.getConnection();
    MCRCM8Item item = null;
    try {
      item = new MCRCM8Item(mcr_id.getId(),connection,itemtypename);
      item.retrieve();
      label = item.getString("/","label");
      }
    catch (MCRPersistenceException e) {
      throw new MCRPersistenceException("A object with ID "+mcr_id.getId()+
        " does not exist."); }
    }
  catch (Exception e) {
    throw new MCRPersistenceException(e.getMessage()); }
  finally {
    MCRCM8ConnectionPool.releaseConnection(connection); }
  return label;
  }

/**
 * The methode update an object in the data store. The index class
 * is determinated by the type of the object ID. This <b>must</b>
 * correspond with the lower case configuration name.<br>
 * As example: Document --> MCR.persistence_cm8_document
 *
 * @param mcr_tc      the typed content array
 * @param xml         the XML stream from the object
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void update(MCRTypedContent mcr_tc, byte [] xml)
  throws MCRConfigurationException, MCRPersistenceException
  {
  // get root data
  MCRObjectID mcr_id = null;
  String mcr_label = null;
  for (int i=0;i<mcr_tc.getSize();i++) {
    if (mcr_tc.getNameElement(i).equals("ID")) {
      mcr_id = new MCRObjectID((String)mcr_tc.getValueElement(i)); 
      mcr_label = (String)mcr_tc.getValueElement(i+1); }
    }
  // Read the item type name from the configuration
  StringBuffer sb = new StringBuffer("MCR.persistence_cm8_");
  sb.append(mcr_id.getTypeId().toLowerCase());
  String itemtypename = MCRConfiguration.instance().getString(sb.toString()); 
  // set up data to item
  DKDatastoreICM connection = null;
  try {
    connection = MCRCM8ConnectionPool.getConnection();
    MCRCM8Item item = null;
    try {
      item = new MCRCM8Item(mcr_id.getId(),connection,itemtypename); 
      item.setAttribute("/","ID",mcr_id.getId());
      item.setAttribute("/","label",mcr_label);
      item.setAttribute("/","xml",xml);
      item.update(); }
    catch (MCRPersistenceException e) {
      throw new MCRPersistenceException("A object with ID "+mcr_id.getId()+
        " does not exist."); }
    System.out.println("Item was updated.");
    }
  catch (Exception e) {
    throw new MCRPersistenceException(e.getMessage()); }
  finally {
    MCRCM8ConnectionPool.releaseConnection(connection); }
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
    return "";
  }

  /** This table stores the highest IDs delivered by getNextFreeId() */
  protected static Properties highestIDs = new Properties();

}

