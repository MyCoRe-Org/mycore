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
import mycore.datamodel.MCRTypedContent;
import mycore.cm8.MCRCM8ConnectionPool;

/**
 * This class implements all methode for handling the ItemType for a
 * MCRObjectID type on IBM Content Manager 8.
 *
 * @author Jens Kupferschmidt
 *
 * @version $Revision$ $Date$
 **/
public final class MCRCM8ItemType
{

// from configuration

/**
 * The constructor of this class.
 **/
public MCRCM8ItemType()
  {
  }

/**
 * The methode create a new datastore based of given configuration.
 *
 * @param mcr_type    the MCRObjectID type as string
 * @param mcr_conf    the configuration XML stream as JDOM tree
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
protected static void create(String mcr_type, org.jdom.Document mcr_conf)
  throws MCRConfigurationException, MCRPersistenceException
  {
  try {
  // read the configuration
  String sb = new String("MCR.persistence_cm8_"+mcr_type);
  String mcr_item_type_name = MCRConfiguration.instance().getString(sb); 
  // connect to server
  DKDatastoreICM connection = null;
  try {
    connection = MCRCM8ConnectionPool.getConnection();
    System.out.println("Info CM8 Datastore Creation: connected.");
    // create the Attribut for MCRObjectID
    if (!createAttributeVarChar(connection,"ID",MCRObjectID.MAX_LENGTH,false)) {
      System.out.println(
        "Warning CM8 Datastore Creation: attribute ID already exists."); }
    // create the Attribut for MCR_Label
    if (!createAttributeVarChar(connection,"label",MCRObject.MAX_LABEL_LENGTH,
      false)) {
      System.out.println(
        "Warning CM8 Datastore Creation: attribute label already exists."); }
    // create the Attribut for the XML byte array
    if (!createAttributeBlob(connection,"xml",200*1024,false)) {
      System.out.println(
        "Warning CM8 Datastore Creation: attribute xml already exists."); }
    // check for the root itemtype
    try {
      DKDatastoreDefICM dsDefICM = new DKDatastoreDefICM(connection);
      DKItemTypeDefICM isitemTypeDef = (DKItemTypeDefICM) dsDefICM
        .retrieveEntity(mcr_item_type_name);
      if (isitemTypeDef != null) {
        System.out.println("Warning CM8 Datastore Creation: itemtype "
          +mcr_item_type_name+" exist.");
        return; }
      }
    catch (DKException e) { 
      System.out.println("Warning CM8 Datastore Creation: itemtype "
        +mcr_item_type_name+" exist.");
      return; }
    // create the root itemtype
    DKItemTypeDefICM item_type = new DKItemTypeDefICM(connection);
    System.out.println("Info CM8 Datastore Creation: "+mcr_item_type_name);
    item_type.setName(mcr_item_type_name);
    item_type.setDescription("MyCoRe ItemType");
    DKDatastoreDefICM dsDefICM = new DKDatastoreDefICM(connection);
    DKAttrDefICM attr = (DKAttrDefICM) dsDefICM.retrieveAttr("ID");
    item_type.addAttr(attr);
    attr = (DKAttrDefICM) dsDefICM.retrieveAttr("label");
    item_type.addAttr(attr);
    attr = (DKAttrDefICM) dsDefICM.retrieveAttr("xml");
    item_type.addAttr(attr);
    item_type.add(); 
    }
  finally {
    MCRCM8ConnectionPool.releaseConnection(connection); }
    }
  catch (Exception e) {
    System.out.println(e.getMessage());
    throw new MCRPersistenceException(e.getMessage(),e); }
  }

/**
 * This methode is internal and create a DK_CM_VARCHAR attribute.
 *
 * @param connection the connection to the database
 * @param name the name of the attribute
 * @param len the len of the character field
 * @param search ist true, if the attribute should text searchable
 * @return If the attribute exists, false was returned, else true.
 **/
private static final boolean createAttributeVarChar(DKDatastoreICM connection,
  String name, int len, boolean search) throws Exception
  {
  DKAttrDefICM attr = new DKAttrDefICM(connection);
  try {
    attr.setName(name);
    attr.setType(DKConstant.DK_CM_VARCHAR);
    attr.setSize(len);
    attr.setTextSearchable(search);
    attr.setNullable(false);
    attr.setUnique(false);
    attr.add(); }
  catch (DKException e) { return false; }
  return true;
  }

/**
 * This methode is internal and create a DK_CM_BLOB attribute.
 *
 * @param connection the connection to the database
 * @param name the name of the attribute
 * @param len the len of the character field
 * @param search ist true, if the attribute should text searchable
 * @return If the attribute exists, false was returned, else true.
 **/
private static final boolean createAttributeBlob(DKDatastoreICM connection,
  String name, int len, boolean search) throws Exception
  {
  DKAttrDefICM attr = new DKAttrDefICM(connection);
  try {
    attr.setName(name);
    attr.setType(DKConstant.DK_CM_BLOB);
    attr.setSize(len);
    attr.setTextSearchable(search);
    attr.setNullable(false);
    attr.setUnique(false);
    attr.add(); }
  catch (DKException e) { return false; }
  return true;
  }

}

