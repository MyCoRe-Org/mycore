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
import mycore.common.MCRPersistenceException;

/**
 * This class implements the interface for the CM8 persistence layer for
 * the data model type MetaDate.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/

public class MCRCM8MetaDate implements DKConstantICM, MCRCM8MetaInterface
{

/**
 * This method create a DKComponentTypeDefICM to create a complete
 * ItemType from the configuration.
 *
 * @param element  a MCR datamodel element as JDOM Element
 * @param connection the connection to the CM8 datastore
 * @param dsDefICM the datastore definition
 * @param prefix the prefix name for the item type
 * @return a DKComponentTypeDefICM for the MCR datamodel element
 * @exception MCRPersistenceException general Exception of MyCoRe
 **/
public DKComponentTypeDefICM createItemType(org.jdom.Element element,
  DKDatastoreICM connection, DKDatastoreDefICM dsDefICM, String prefix)
  throws MCRPersistenceException
  {
  String subtagname = prefix+(String)element.getAttribute("name").getValue();
System.out.println("MCRCM8MetaDate "+subtagname);
  DKComponentTypeDefICM lt = new DKComponentTypeDefICM(connection);
  try {
    // create component child
    lt.setName(subtagname);
    lt.setDeleteRule(DK_ICM_DELETE_RULE_CASCADE);
    // add lang attribute
    DKAttrDefICM attr = (DKAttrDefICM) dsDefICM.retrieveAttr(prefix+"lang");
    lt.addAttr(attr);
    // add type attribute
    attr = (DKAttrDefICM) dsDefICM.retrieveAttr(prefix+"type");
    lt.addAttr(attr);
    // create the attribute for the data content in date form
    if (!MCRCM8ItemType.createAttributeDate(connection,subtagname)) {
      System.out.println( "Warning CM8 Datastore Creation: attribute "+
        subtagname+" already exists."); }
    // add the value attribute
    attr = (DKAttrDefICM) dsDefICM.retrieveAttr(subtagname);
    lt.addAttr(attr);
    }
  catch (Exception e) {
    throw new MCRPersistenceException(e.getMessage(),e); }
  return lt;
  }

}
