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

package org.mycore.backend.cm8;

import java.util.*;
import java.io.*;
import java.net.*;

import com.ibm.mm.sdk.server.*;
import com.ibm.mm.sdk.common.*;

import org.apache.log4j.Logger;

import org.mycore.common.*;
import org.mycore.datamodel.ifs.*;

/**
 * This class implements the MCRContentStore interface to store the content of
 * MCRFile objects in a IBM Content Manager 7 index class. The index class, the
 * keyfield labels and maximum DKDDO size can be configured in mycore.properties
 *
 * <code>
 *   MCR.IFS.ContentStore.<StoreID>.ItemType        Index Class to use
 *   MCR.IFS.ContentStore.<StoreID>.Attribute.File  Name of file ID attribute
 *   MCR.IFS.ContentStore.<StoreID>.Attribute.Time  Name of timestamp attribute
 * </code>
 *
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRCStoreContentManager8 
  extends MCRContentStore implements DKConstantICM
{
  /** The ItemType name to store the content */
  protected String itemTypeName;

  /** The name of the attribute that stores the MCRFile.getID() */
  protected String attributeFile;
  protected final int MAX_ATTRIBUTE_FILE_LENGTH = 32;

  /** The name of the attribute that stores the creation timestamp */
  protected String attributeTime;
  protected final int MAX_ATTRIBUTE_TIME_LENGTH = 32;

  public void init( String storeID )
  {
    super.init( storeID );
    MCRConfiguration config = MCRConfiguration.instance();
    itemTypeName   = config.getString( prefix + "ItemType"       );
    attributeTime  = config.getString( prefix + "Attribute.File" );
    attributeFile  = config.getString( prefix + "Attribute.Time" );
  }

  public String storeContent( MCRFileReader file, MCRContentInputStream source )
    throws MCRPersistenceException
  {
    Logger logger = MCRCM8ConnectionPool.getLogger(); 
    DKDatastoreICM connection = null;
    try 
    {
      logger.debug( "Get a connection to CM8 connection pool." );
      connection = MCRCM8ConnectionPool.instance().getConnection();
      
      DKLobICM ddo = null;
      try{ ddo = (DKLobICM)connection.createDDO(itemTypeName,DK_CM_DOCUMENT); }
      catch( Exception ex ) 
      {
        createStore( connection ); 
        ddo = (DKLobICM)connection.createDDO(itemTypeName,DK_CM_DOCUMENT); 
      }
      logger.debug("A new DKLobICM was created.");
      
      logger.debug("MCRFile ID = "+ file.getID() );
      short dataId = ((DKDDO)ddo).dataId(DK_CM_NAMESPACE_ATTR,"ifsfile");
      ((DKDDO)ddo).setData(dataId, file.getID() );
      
      String timestamp = buildNextTimestamp();
      dataId = ((DKDDO)ddo).dataId(DK_CM_NAMESPACE_ATTR,"ifstime");
      ((DKDDO)ddo).setData(dataId,timestamp);
      
      logger.debug("MimeType = "+file.getContentType().getMimeType());
      ddo.setMimeType(file.getContentType().getMimeType());
      
      logger.debug("Set the MCRContentInputStream with length "+source.available()+".");
      ddo.add((InputStream)source,source.available());
      logger.debug("Add the DKLobICM.");
      
      String storageID = ddo.getPidObject().pidString();
      logger.debug("StorageID = "+storageID);
      logger.debug("The file was stored under CM8 Ressource Manager.");
      return storageID;
    }
    catch (Exception ex) 
    {
      String msg = "Error while storing data in IFS CM8 store.";
      throw new MCRPersistenceException( msg, ex );
    }
    finally{ MCRCM8ConnectionPool.instance().releaseConnection(connection); }
  }

  public void deleteContent( String storageID )
    throws MCRPersistenceException
  {
    Logger logger = MCRCM8ConnectionPool.getLogger(); 
    logger.debug("StorageID = "+storageID);
  
    DKDatastoreICM connection = MCRCM8ConnectionPool.instance().getConnection();
    try 
    {
      DKLobICM ddo = (DKLobICM)connection.createDDO(storageID);
      ddo.del();
      logger.debug("The file was deleted from CM8 Ressource Manager.");
    }
    catch(Exception ex) 
    {
      String msg = "Error deleting parts of ContentManager item " + storageID;
      throw new MCRPersistenceException(msg,ex);
    }
    finally{ MCRCM8ConnectionPool.instance().releaseConnection( connection ); }
  }

  public void retrieveContent( MCRFileReader file, OutputStream target )
    throws MCRPersistenceException
  {
    Logger logger = MCRCM8ConnectionPool.getLogger(); 
    logger.debug("StorageID = "+ file.getStorageID() );
    
    DKDatastoreICM connection = MCRCM8ConnectionPool.instance().getConnection();
    try 
    {
      DKLobICM ddo = (DKLobICM)connection.createDDO( file.getStorageID() );
      ddo.retrieve(DK_CM_CONTENT_NO);
      
      String url = ddo.getContentURL(-1,-1,-1);
      logger.debug("URL = "+url);
      InputStream is = new URL( url ).openStream();
      MCRUtils.copyStream( is, target );
      
      logger.debug("The file was retrieved from CM8 Ressource Manager.");
    }
    catch( Exception ex ) 
    {
      String msg = "Error while retrieving data from CM8 item " + file.getStorageID();
      throw new MCRPersistenceException( msg, ex );
    }
    finally{ MCRCM8ConnectionPool.instance().releaseConnection( connection ); }
  }

 /**
  * This method creates a new ItemType to store ressource data under CM8.
  *
  * @param connection the DKDatastoreICM connection
  **/
  private void createStore(DKDatastoreICM connection) throws Exception
  {
    Logger logger = MCRCM8ConnectionPool.getLogger(); 
    // create the Attribute for IFS File ID
    if (!createAttributeVarChar(connection,"ifsfile", MAX_ATTRIBUTE_FILE_LENGTH))
      logger.warn("CM8 Datastore Creation attribute ifsfile already exists.");
    // create the Attribute for MCR_Label
    if (!createAttributeVarChar(connection,"ifstime", MAX_ATTRIBUTE_TIME_LENGTH))
      logger.warn("CM8 Datastore Creation attribute ifstime already exists.");

    // create the root itemtype
    logger.info("Create the ItemType "+itemTypeName);
    DKItemTypeDefICM item_type = new DKItemTypeDefICM(connection);
    item_type.setName(itemTypeName);
    item_type.setDescription(itemTypeName);
    item_type.setDeleteRule(DK_ICM_DELETE_RULE_CASCADE);
    DKDatastoreDefICM dsDefICM = new DKDatastoreDefICM(connection);
    DKAttrDefICM attr = (DKAttrDefICM) dsDefICM.retrieveAttr("ifsfile");
    attr.setNullable(false);
    attr.setUnique(false);
    item_type.addAttr(attr);
    attr = (DKAttrDefICM) dsDefICM.retrieveAttr("ifstime");
    attr.setNullable(false);
    attr.setUnique(false);
    item_type.addAttr(attr);
    item_type.setClassification(DK_ICM_ITEMTYPE_CLASS_RESOURCE_ITEM);
    item_type.setXDOClassName(DK_ICM_XDO_LOB_CLASS_NAME);
    item_type.setXDOClassID(DK_ICM_XDO_LOB_CLASS_ID);
    short rmcode = 1; // the default
    item_type.setDefaultRMCode(rmcode);
    short smscode = 1; // the default
    item_type.setDefaultCollCode(smscode);
    item_type.add();
    logger.info("The ItemType "+itemTypeName+" for IFS CM8 store is created."); 
  }

/**
 * This method is internal and creates a DK_CM_VARCHAR attribute.
 *
 * @param connection the connection to the database
 * @param name the name of the attribute
 * @param len the len of the character field
 * @param search ist true, if the attribute should text searchable
 * @return If the attribute exists, false was returned, else true.
 **/
  public static final boolean createAttributeVarChar(DKDatastoreICM connection,
    String name, int len) throws Exception
  {
    DKAttrDefICM attr = new DKAttrDefICM(connection);
    try 
    {
      attr.setName(name);
      attr.setType(DK_CM_VARCHAR);
      attr.setStringType(DK_CM_ATTR_VAR_ALPHANUM_EXT);
      attr.setSize(len);
      attr.setTextSearchable(false);
      attr.setNullable(true);
      attr.setUnique(false);
      attr.add(); 
    }
    catch (DKException e) { return false; }
    return true;
  }
}

