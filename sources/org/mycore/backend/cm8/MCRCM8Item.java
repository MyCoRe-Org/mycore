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
  private ArrayList ddolist = new ArrayList();
  private ArrayList ddopath = new ArrayList();
  private ArrayList ddocoll = new ArrayList();
  private String itemtypename = "";

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
    ddolist = new ArrayList();
    ddopath = new ArrayList();
    ddocoll = new ArrayList();
    ddolist.add(connection.createDDO(itemtypename,DK_CM_DOCUMENT));
    ddopath.add("/");
    ddocoll.add(null);
    this.itemtypename = itemtypename;
    }

  /**
   * Constructor for a new Item as result for a search of a MCRObjectID.
   * 
   * @param id                  the MCRObjectID as string
   * @param connection          the given CM connection
   * @param itemtypename        the given ItemType name
   * @param itemtypeprefix      the given ItemType name
   * @exception MCRPersistenceException Exceptions of MyCoRe
   * @exception DKException     Exceptions of CM
   * @exception Exception       Exceptions of JDK
   **/
  public MCRCM8Item(String id, DKDatastoreICM connection, String itemtypename,
    String itemtypeprefix)
    throws DKException, MCRPersistenceException, Exception
    {
    if (id==null) {
      throw new MCRPersistenceException("MCRCM7Item constructor error."); }
    ddolist = new ArrayList();
    ddopath = new ArrayList();
    ddocoll = new ArrayList();
    ddolist.add(connection.createDDO(itemtypename,DK_CM_DOCUMENT));
    ddopath.add("/");
    ddocoll.add(null);

    StringBuffer qs = new StringBuffer(128);
    qs.append('/').append(itemtypename).append("[@").append(itemtypeprefix)
      .append("ID=\"").append(id).append("\"]");

    DKResults results = (DKResults)connection.evaluate(qs.toString(),
      DK_CM_XQPE_QL_TYPE,null);

    dkIterator iter = results.createIterator();
    if (! iter.more()) throw new MCRPersistenceException(
      "There is no item in Item Type " + itemtypename +
      " that matches the MCRObjectID (" + id + ")" );
    ddolist.set(0,(DKDDO)iter.next());
    this.itemtypename = itemtypename;
    }

  /**
   * This methode lock the item in the datastore.
   *
   * @param connection          the connection to ICM
   * @exception DKException     Exceptions of CM
   * @exception Exception       Exceptions of JDK
   **/
  public final void lock(DKDatastoreICM connection) throws DKException, 
    Exception
    { connection.checkOut((DKDDO)ddolist.get(0)); }

  /**
   * This methode creates the item in the datastore.
   *
   * @exception DKException     Exceptions of CM
   * @exception Exception       Exceptions of JDK
   **/
  public final void create() throws DKException, Exception
    { ((DKDDO)ddolist.get(0)).add(); } 

  /**
   * This methode retrievs the item from the datastore.
   *
   * @exception DKException     Exceptions of CM
   * @exception Exception       Exceptions of JDK
   **/
  public final void retrieve() throws DKException, Exception
    { ((DKDDO)ddolist.get(0)).retrieve(); }

  /**
   * This methode updates the item in the datastore.
   *
   * @exception DKException     Exceptions of CM
   * @exception Exception       Exceptions of JDK
   **/
  public final void update() throws DKException, Exception
    { ((DKDDO)ddolist.get(0)).update(DK_CM_CHECKIN); }

  /**
   * This methode updates the item in the datastore with an option.
   *
   * @param option              The update option
   * @exception DKException     Exceptions of CM
   * @exception Exception       Exceptions of JDK
   **/
  public final void update(short option) throws DKException, Exception
    { ((DKDDO)ddolist.get(0)).update(option); }

  /**
   * This methode deletes the item from the datastore.
   *
   * @exception DKException     Exceptions of CM
   * @exception Exception       Exceptions of JDK
   **/
  public final void delete() throws DKException, Exception
    { ((DKDDO)ddolist.get(0)).del(); }

  /**
   * This method add a child component to the existing DKDDO with the
   * given path name list.
   *
   * @exception Exception       Exceptions of JDK
   * @exception DKException     Exceptions of CM
   **/
  public final void setChild(DKDatastoreICM connection, String parentname, 
    String childname, String parentpath, String childpath) throws DKException, 
    Exception
    {
System.out.println("Parent = "+parentname+"   Child = "+childname+"   Parentpath = "+parentpath+"  Childpath = "+childpath);
    // Check for existing child component path
    int poschild = -1;
    for (int i = 0; i < ddopath.size(); i++) {
      if (((String)ddopath.get(i)).equals(childpath)) {
        poschild = i; break; }
      }
    if (poschild != -1) { return; }
    // Check for existing parent component path
    int posparent = -1;
    for (int i = 0; i < ddopath.size(); i++) {
      if (((String)ddopath.get(i)).equals(parentpath)) {
        posparent = i; break; }
      }
    if (posparent == -1) {
      throw new MCRPersistenceException( 
        "Path name error for parent in MCRCM8Item.setAttribute()."); }
    // Create if it does not exist
    if (poschild == -1) {
      DKDDO ddochild = connection.createChildDDO(itemtypename,childname);
      DKChildCollection colchild = new DKChildCollection();
      ((DKDDO)ddolist.get(posparent)).setData(((DKDDO)ddolist.get(posparent))
        .dataId(DK_CM_NAMESPACE_CHILD,ddochild.getObjectType()),colchild);
      colchild.addElement(ddochild);
      ddolist.add(ddochild);
      ddopath.add(childpath);
      ddocoll.add(colchild);
      } 
    }
   
  /**
   * This methode set the value of the named attribute for the given path
   * in an existing root DKDDO.
   * 
   * @param pathname            the path to the attribute
   * @param attrname            the name of the object
   * @param value               the value for this keyfield
   * @exception Exception       Exceptions of JDK
   * @exception DKException     Exceptions of CM
   **/
  public final void setAttribute(String pathname, String attrname, 
    Object value) throws DKException, Exception
    {
    if ((pathname==null)||((pathname = pathname.trim()).length() ==0)) {
      throw new MCRPersistenceException( 
        "Path name error in MCRCM8Item.setAttribute()."); }
    if ((attrname==null)||((attrname = attrname.trim()).length() ==0)) {
      throw new MCRPersistenceException( 
        "Attribute name error in MCRCM8Item.setAttribute()."); }
    // Check for existing component path
    int pos = -1;
    for (int i = 0; i < ddopath.size(); i++) {
      if (((String)ddopath.get(i)).equals(pathname)) {
        pos = i; break; }
      }
    if (pos == -1) {
      throw new MCRPersistenceException( 
        "Path name error in MCRCM8Item.setAttribute()."); }
    // Set attribute
    short dataId = ((DKDDO)ddolist.get(pos)).dataId(DK_CM_NAMESPACE_ATTR,
      attrname);
    ((DKDDO)ddolist.get(pos)).setData(dataId,value);
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
    // Check for existing component path
    int pos = -1;
    for (int i = 0; i < ddopath.size(); i++) {
      if (((String)ddopath.get(i)).equals(pathname)) {
        pos = i; break; }
      }
    if (pos == -1) {
      throw new MCRPersistenceException( 
        "Path name error in MCRCM8Item.setAttribute()."); }
    // Set attribute
    short dataId = ((DKDDO)ddolist.get(pos)).dataId(DK_CM_NAMESPACE_ATTR,
      attrname);
    return (byte []) ((DKDDO)ddolist.get(pos)).getData(dataId);
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
    // Check for existing componet path
    int pos = -1;
    for (int i = 0; i < ddopath.size(); i++) {
      if (((String)ddopath.get(i)).equals(pathname)) {
        pos = i; break; }
      }
    if (pos == -1) {
      throw new MCRPersistenceException( 
        "Path name error in MCRCM8Item.setAttribute()."); }
    // Set attribute
    short dataId = ((DKDDO)ddolist.get(pos)).dataId(DK_CM_NAMESPACE_ATTR,
      attrname);
    return (String) ((DKDDO)ddolist.get(pos)).getData(dataId);
    }

  }

