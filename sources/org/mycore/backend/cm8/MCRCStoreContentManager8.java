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
  protected final int MAX_ATTRIBUTE_FILE_LENGTH = 128;

  /** The name of the attribute that stores the creation timestamp */
  protected String attributeTime;
  protected final int MAX_ATTRIBUTE_TIME_LENGTH = 128;

  /** The temporary store of the files **/
  protected String [] STORE_TYPE_LIST = { "none","memory" };
  protected String storeTempType = "";
  protected int storeTempSize = 4;
  protected String storeTempDir = "";

  /**
   * The method initialized the CM8 content store with data of the
   * property files.
   * If StoreTemp.Type is not set, "none" was set.
   * The following types are nessesary:<br />
   * <ul>
   * <li> none - it use the InputStream.available() method to get the
   *      length of the stream.</li>
   * <li> memory - it write the InputStream in the memory to get the
   *      length of the stream. Attention, this is only fo short files with
   *      maximal size in MB of the StoreTemp.MemSize value! Files they
   *      are bigger was temporary stored in a directory that is defined
   *      in the prperty variable StoreTemp.Dir. </li>
   * </ul>
   *
   * @param storeID the IFS store ID
   **/
  public void init( String storeID )
  {
    super.init( storeID );
    MCRConfiguration config = MCRConfiguration.instance();
    itemTypeName   = config.getString( prefix + "ItemType"       );
    attributeTime  = config.getString( prefix + "Attribute.File" );
    attributeFile  = config.getString( prefix + "Attribute.Time" );
    storeTempType  = config.getString( prefix + "StoreTemp.Type", "none" );
    storeTempSize  = config.getInt( prefix + "StoreTemp.MemSize", 4 );
    storeTempDir   = config.getString( prefix + "StoreTemp.Dir", "/tmp" );
    for (int i=0;i<STORE_TYPE_LIST.length;i++) {
      if (storeTempType.equals(STORE_TYPE_LIST[i])) { return; } }
    storeTempType = "none";
  }

  protected String doStoreContent( MCRFileReader file, MCRContentInputStream source )
    throws Exception
  {
    Logger logger = MCRCM8ConnectionPool.getLogger(); 
    DKDatastoreICM connection = null;
    try 
    {
      logger.debug( "Get a connection to CM8 connection pool." );
      connection = MCRCM8ConnectionPool.instance().getConnection();
      
      DKTextICM ddo = null;
      try{ ddo = (DKTextICM)connection.createDDO(itemTypeName,DK_CM_ITEM); }
      catch( Exception ex ) 
      {
        createStore( connection ); 
        ddo = (DKTextICM)connection.createDDO(itemTypeName,DK_CM_ITEM); 
      }
      logger.debug("A new DKTextICM was created.");
      
      logger.debug("MCRFile ID = "+ file.getID() );
      short dataId = ((DKDDO)ddo).dataId(DK_CM_NAMESPACE_ATTR,attributeFile);
      ((DKDDO)ddo).setData(dataId, file.getID() );
      
      String timestamp = buildNextTimestamp();
      dataId = ((DKDDO)ddo).dataId(DK_CM_NAMESPACE_ATTR,attributeTime);
      ((DKDDO)ddo).setData(dataId,timestamp);
      
      logger.debug("MimeType = "+file.getContentType().getMimeType());
      ddo.setMimeType(file.getContentType().getMimeType());
      ddo.setTextSearchableFlag(true);
      
      int filesize = 0;
      if (storeTempType.equals("none")) {
        filesize = source.available(); 
        logger.debug("Set the MCRContentInputStream with available() length "
          +filesize+".");
        ddo.add((InputStream)source,filesize);
        }
      if (storeTempType.equals("memory")) {
        byte [] buffer = new byte[storeTempSize*1024*1024+16];
        try {
          filesize = source.read(buffer,0,storeTempSize*1024*1024+16); }
        catch (IOException e) {
          throw new MCRException("Cant read File with ID "+file.getID(),e); }
        if (filesize <= storeTempSize*1024*1024) {
          logger.debug("Set the MCRContentInputStream with memory length "
            +filesize+".");
          ddo.add((InputStream)(new ByteArrayInputStream(buffer)),filesize);
          }
        else {
          int si = filesize;
          File tmp = new File(storeTempDir,file.getID());
          FileOutputStream ftmp = new FileOutputStream(tmp);
          try {
            ftmp.write(buffer,0,filesize);
            }
          catch (IOException e) {
            throw new MCRException("Cant write File with ID "+file.getID()+
              " to "+storeTempDir,e); 
            }
          while (true) {
            try {
              si = source.read(buffer,0,storeTempSize*1024*1024+16); }
            catch (IOException e) {
              throw new MCRException("Cant read File with ID "+file.getID(),
                e);
              }
            if (si == -1) { break; }
            filesize += si;
            try {
              ftmp.write(buffer,0,si); }
            catch (IOException e) {
              throw new MCRException("Cant write File with ID "+file.getID()+
                " to "+storeTempDir,e); 
              }
            }
          ftmp.close();
          logger.debug("Set the MCRContentInputStream with stream length "
            +filesize+".");
          ddo.add((InputStream)(new FileInputStream(tmp)),filesize);
          try { tmp.delete(); } catch (SecurityException e) { }
          }
        }
      logger.debug("Add the DKTextICM.");
      
      String storageID = ddo.getPidObject().pidString();
      logger.debug("StorageID = "+storageID);
      logger.debug("The file was stored under CM8 Ressource Manager.");
      return storageID;
    }
    finally{ MCRCM8ConnectionPool.instance().releaseConnection(connection); }
  }

  /**
   * the method removes the content for the given IFS storageID.
   *
   * @param storageID the IFS storage ID
   * @exception if an error was occured.
   **/
  protected void doDeleteContent( String storageID )
    throws Exception
  {
    Logger logger = MCRCM8ConnectionPool.getLogger(); 
    logger.debug("StorageID = "+storageID);
  
    DKDatastoreICM connection = MCRCM8ConnectionPool.instance().getConnection();
    try 
    {
      DKTextICM ddo = (DKTextICM)connection.createDDO(storageID);
      ddo.del();
      logger.debug("The file was deleted from CM8 Ressource Manager.");
    }
    finally{ MCRCM8ConnectionPool.instance().releaseConnection( connection ); }
  }

  protected void doRetrieveContent( MCRFileReader file, OutputStream target )
    throws Exception
  {
    Logger logger = MCRCM8ConnectionPool.getLogger(); 
    logger.debug("StorageID = "+ file.getStorageID() );
    
    DKDatastoreICM connection = MCRCM8ConnectionPool.instance().getConnection();
    try 
    {
      DKTextICM ddo = (DKTextICM)connection.createDDO( file.getStorageID() );
      ddo.retrieve(DK_CM_CONTENT_NO);
      
      String url = ddo.getContentURL(-1,-1,-1);
      logger.debug("URL = "+url);
      InputStream is = new URL( url ).openStream();
      MCRUtils.copyStream( is, target );
      
      logger.debug("The file was retrieved from CM8 Ressource Manager.");
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
    if (!MCRCM8ItemTypeCommon.createAttributeVarChar(connection,attributeFile, 
      MAX_ATTRIBUTE_FILE_LENGTH,false))
      logger.warn("CM8 Datastore Creation attribute "+attributeFile+
        " already exists.");
    // create the Attribute for IFS Time 
    if (!MCRCM8ItemTypeCommon.createAttributeVarChar(connection,attributeTime, 
      MAX_ATTRIBUTE_TIME_LENGTH,false))
      logger.warn("CM8 Datastore Creation attribute "+attributeTime+
        " already exists.");

    // create a text search definition
    DKTextIndexDefICM mcr_item_text_index = 
      MCRCM8ItemTypeCommon.getTextDefinition();
    mcr_item_text_index.setUDFName("ICMfetchFilter");
    mcr_item_text_index.setUDFSchema("icmadmin");

    // create the root itemtype
    logger.info("Create the ItemType "+itemTypeName);
    DKItemTypeDefICM item_type = new DKItemTypeDefICM(connection);
    item_type.setName(itemTypeName);
    item_type.setDescription(itemTypeName);
    item_type.setDeleteRule(DK_ICM_DELETE_RULE_CASCADE);
    item_type.setClassification(DK_ICM_ITEMTYPE_CLASS_RESOURCE_ITEM);
    item_type.setXDOClassName(DK_ICM_XDO_TEXT_CLASS_NAME);
    item_type.setXDOClassID(DK_ICM_XDO_TEXT_CLASS_ID);
    item_type.setTextIndexDef(mcr_item_text_index);
    item_type.setTextSearchable(true);
    DKDatastoreDefICM dsDefICM = new DKDatastoreDefICM(connection);
    DKAttrDefICM attr = (DKAttrDefICM) dsDefICM.retrieveAttr(attributeFile);
    attr.setNullable(false);
    attr.setUnique(false);
    item_type.addAttr(attr);
    attr = (DKAttrDefICM) dsDefICM.retrieveAttr(attributeTime);
    attr.setNullable(false);
    attr.setUnique(false);
    item_type.addAttr(attr);
    short rmcode = 1; // the default
    item_type.setDefaultRMCode(rmcode);
    short smscode = 1; // the default
    item_type.setDefaultCollCode(smscode);
    item_type.add();
    logger.info("The ItemType "+itemTypeName+" for IFS CM8 store is created."); 
  }

}

