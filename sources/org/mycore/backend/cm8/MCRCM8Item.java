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

import com.ibm.mm.sdk.server.*;
import com.ibm.mm.sdk.common.*;
import java.util.*;
import java.io.File;
import mycore.common.MCRPersistenceException;

/**
 * <B>This class implements the access routines for a IBM Content Manager 8
 * item.</B>
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public final class MCRCM8Item implements DKConstantICM
  {
  private DKDDO ddo;
  private DKChildCollection children;

  /**
   * Constructor for a new Item.
   * 
   * @param ddo                 a DKDDO object
   * @exception DKException     Exceptions of CM
   **/
  public MCRCM8Item( DKDDO ddo ) throws DKException
    { this.ddo = ddo; }

  /**
   * Constructor for a new Item with given CM connection.
   * 
   * @param connection          the given CM connection
   * @param itemTypeName        the name of ItemType for the new item
   * @exception DKException     Exceptions of CM
   **/
  public MCRCM8Item(DKDatastoreICM connection, String itemtypename)
    throws DKException, Exception
    {
    ddo = connection.createDDO(itemtypename,DK_CM_ITEM);
    children = null;
    }

  /**
   * Constructor for a new Item as result for a search of a MCRObjectID.
   * 
   * @param id                  the MCRObjectID as string
   * @param connection          the given CM connection
   * @param itemtypename        the given ItemType name
   * @exception MCRPersistenceException Exceptions of MyCoRe
   * @exception DKException     Exceptions of CM
   * @exception Exception       Exceptions of JDK
   **/
  public MCRCM8Item(String id, DKDatastoreICM connection, String itemtypename)
    throws DKException, MCRPersistenceException, Exception
    {
    if (id==null) {
      throw new MCRPersistenceException("MCRCM7Item constructor error."); }
    ddo = connection.createDDO(itemtypename,DK_CM_ITEM);
    children = null;

    StringBuffer qs = new StringBuffer(128);
    qs.append('/').append(itemtypename).append("[@ID=\"").append(id)
      .append("\"]");

    DKResults results = (DKResults)connection.evaluate(qs.toString(),
      DK_CM_XQPE_QL_TYPE,null);

    dkIterator iter = results.createIterator();
    if (! iter.more()) throw new MCRPersistenceException(
      "There is no item in Item Type " + itemtypename +
      " that matches the MCRObjectID (" + id + ")" );
    this.ddo = ( DKDDO ) iter.next();
    }

  /**
   * This methode creates the item in the datastore.
   *
   * @exception DKException     Exceptions of CM
   * @exception Exception       Exceptions of JDK
   **/
  public final void create() throws DKException, Exception
    { ddo.add(); } 

  /**
   * This methode retrievs the item from the datastore.
   *
   * @exception DKException     Exceptions of CM
   * @exception Exception       Exceptions of JDK
   **/
  public final void retrieve() throws DKException, Exception
    { ddo.retrieve(); }

  /**
   * This methode updates the item in the datastore.
   *
   * @exception DKException     Exceptions of CM
   * @exception Exception       Exceptions of JDK
   **/
  public final void update() throws DKException, Exception
    { ddo.update(); }

  /**
   * This methode deletes the item from the datastore.
   *
   * @exception DKException     Exceptions of CM
   * @exception Exception       Exceptions of JDK
   **/
  public final void delete() throws DKException, Exception
    { ddo.del(); }

  /**
   * This methode set the string value of the named attribute in the
   * given DKDDO.
   * 
   * @param pathname            the path to the attribute
   * @param attrname            the name of the object
   * @param value               the value for this keyfield
   * @exception Exception       Exceptions of JDK
   * @exception DKException     Exceptions of CM
   **/
  public final void setAttribute(String pathname, String attrname, 
    String value) throws DKException, Exception
    {
    if ((pathname==null)||((pathname = pathname.trim()).length() ==0)) {
      throw new MCRPersistenceException( 
        "Path name error in MCRCM8Item.setAttribute()."); }
    if ((attrname==null)||((attrname = attrname.trim()).length() ==0)) {
      throw new MCRPersistenceException( 
        "Attribute name error in MCRCM8Item.setAttribute()."); }
    if (value != null) {
      value = value.trim(); if ( value.length() == 0 ) value = null; }
    // Do enything if path is not /
    if (!pathname.equals("/")) {
      }
    short dataId = ddo.dataId(DK_CM_NAMESPACE_ATTR,attrname);
    ddo.setData(dataId,value);
    }

  /**
   * This methode set the byte array value of the named attribute in the
   * given DKDDO.
   * 
   * @param pathname            the path to the attribute
   * @param attrname            the name of the object
   * @param value               the value for this keyfield
   * @exception Exception       Exceptions of JDK
   * @exception DKException     Exceptions of CM
   **/
  public final void setAttribute(String pathname, String attrname, 
    byte [] value) throws DKException, Exception
    {
    if ((pathname==null)||((pathname = pathname.trim()).length() ==0)) {
      throw new MCRPersistenceException( 
        "Path name error in MCRCM8Item.setAttribute()."); }
    if ((attrname==null)||((attrname = attrname.trim()).length() ==0)) {
      throw new MCRPersistenceException( 
        "Attribute name error in MCRCM8Item.setAttribute()."); }
    // Do enything if path is not /
    if (!pathname.equals("/")) {
      }
    short dataId = ddo.dataId(DK_CM_NAMESPACE_ATTR,attrname);
    ddo.setData(dataId,value);
    }

  /**
   * The method returns a byte array for the given CM8 attribute.
   *
   * @param pathname            the path to the attribute
   * @param attrname            the name of the object
   * @exception Exception       Exceptions of JDK
   * @exception DKException     Exceptions of CM
   **/
  public final byte [] getBlob(String pathname, String attrname)
    throws DKException, Exception
    {
    if ((pathname==null)||((pathname = pathname.trim()).length() ==0)) {
      throw new MCRPersistenceException( 
        "Path name error in MCRCM8Item.setAttribute()."); }
    if ((attrname==null)||((attrname = attrname.trim()).length() ==0)) {
      throw new MCRPersistenceException( 
        "Attribute name error in MCRCM8Item.setAttribute()."); }
    // Do enything if path is not /
    if (!pathname.equals("/")) {
      }
    short dataId = ddo.dataId(DK_CM_NAMESPACE_ATTR,attrname);
    return (byte []) ddo.getData(dataId);
    }

  /**
   * The method returns a String value for the given CM8 attribute.
   *
   * @param pathname            the path to the attribute
   * @param attrname            the name of the object
   * @exception Exception       Exceptions of JDK
   * @exception DKException     Exceptions of CM
   **/
  public final String getString(String pathname, String attrname)
    throws DKException, Exception
    {
    if ((pathname==null)||((pathname = pathname.trim()).length() ==0)) {
      throw new MCRPersistenceException( 
        "Path name error in MCRCM8Item.setAttribute()."); }
    if ((attrname==null)||((attrname = attrname.trim()).length() ==0)) {
      throw new MCRPersistenceException( 
        "Attribute name error in MCRCM8Item.setAttribute()."); }
    // Do enything if path is not /
    if (!pathname.equals("/")) {
      }
    short dataId = ddo.dataId(DK_CM_NAMESPACE_ATTR,attrname);
    return (String) ddo.getData(dataId);
    }

  }

