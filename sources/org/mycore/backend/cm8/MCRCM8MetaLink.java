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

import com.ibm.mm.sdk.server.*;
import com.ibm.mm.sdk.common.*;

import org.apache.log4j.Logger;

import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.metadata.MCRMetaDefault;

/**
 * This class implements the interface for the CM8 persistence layer for
 * the data model type MetaLink.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/

public class MCRCM8MetaLink implements DKConstantICM, MCRCM8MetaInterface
{

/**
 * This method create a DKComponentTypeDefICM to create a complete
 * ItemType from the configuration.
 *
 * @param element  a MCR datamodel element as JDOM Element
 * @param connection the connection to the CM8 datastore
 * @param dsDefICM the datastore definition
 * @param prefix the prefix name for the item type
 * @param textindex the definition of the text search index
 * @param textserach the flag to use textsearch as string 
 *                   (this is a dummy value)
 * @return a DKComponentTypeDefICM for the MCR datamodel element
 * @exception MCRPersistenceException general Exception of MyCoRe CM8
 **/
public DKComponentTypeDefICM createItemType(org.jdom.Element element,
  DKDatastoreICM connection, DKDatastoreDefICM dsDefICM, String prefix,
  DKTextIndexDefICM textindex,String textsearch) throws MCRPersistenceException
  {
  Logger logger = MCRCM8ConnectionPool.getLogger();
  String subtagname = prefix+(String)element.getAttribute("name").getValue();
  String typename = prefix+"xlinktype";
  String hrefname = prefix+"xlinkhref";
  String labelname = prefix+"xlinklabel";
  String titlename = prefix+"xlinktitle";
  String fromname = prefix+"xlinkfrom";
  String toname = prefix+"xlinkto";
  int typelen = org.mycore.datamodel.metadata.MCRMetaLink.MAX_XLINK_TYPE_LENGTH;
  int hreflen = org.mycore.datamodel.metadata.MCRMetaLink.MAX_XLINK_HREF_LENGTH;
  int labellen = org.mycore.datamodel.metadata.MCRMetaLink.MAX_XLINK_LABEL_LENGTH;
  int titlelen = org.mycore.datamodel.metadata.MCRMetaLink.MAX_XLINK_TITLE_LENGTH;
  int fromlen = org.mycore.datamodel.metadata.MCRMetaLink.MAX_XLINK_FROM_LENGTH;
  int tolen = org.mycore.datamodel.metadata.MCRMetaLink.MAX_XLINK_TO_LENGTH;

  DKComponentTypeDefICM lt = new DKComponentTypeDefICM(connection);
  try {
    // create component child
    lt.setName(subtagname);
    lt.setDeleteRule(DK_ICM_DELETE_RULE_CASCADE);
    DKAttrDefICM attr;
    // create the type attribute for the data content
    if (!MCRCM8ItemType.createAttributeVarChar(connection,typename,typelen,
      false)) {
      logger.warn( "CM8 Datastore Creation attribute "+
        typename+" already exists."); }
    // add the value attribute
    attr = (DKAttrDefICM) dsDefICM.retrieveAttr(typename);
    attr.setNullable(true);
    attr.setUnique(false);
    lt.addAttr(attr);
    // create the href attribute for the data content
    if (!MCRCM8ItemType.createAttributeVarChar(connection,hrefname,hreflen,
      false)) {
      logger.warn( "CM8 Datastore Creation attribute "+
        hrefname+" already exists."); }
    // add the value attribute
    attr = (DKAttrDefICM) dsDefICM.retrieveAttr(hrefname);
    attr.setNullable(true);
    attr.setUnique(false);
    lt.addAttr(attr);
    // create the label attribute for the data content
    if (!MCRCM8ItemType.createAttributeVarChar(connection,labelname,labellen,
      false)) {
      logger.warn( "CM8 Datastore Creation attribute "+
        labelname+" already exists."); }
    // add the value attribute
    attr = (DKAttrDefICM) dsDefICM.retrieveAttr(labelname);
    attr.setNullable(true);
    attr.setUnique(false);
    lt.addAttr(attr);
    // create the title attribute for the data content
    if (!MCRCM8ItemType.createAttributeVarChar(connection,titlename,titlelen,
      false)) {
      logger.warn( "CM8 Datastore Creation attribute "+
        titlename+" already exists."); }
    // add the value attribute
    attr = (DKAttrDefICM) dsDefICM.retrieveAttr(titlename);
    attr.setNullable(true);
    attr.setUnique(false);
    lt.addAttr(attr);
    // create the from attribute for the data content
    if (!MCRCM8ItemType.createAttributeVarChar(connection,fromname,fromlen,
      false)) {
      logger.warn( "CM8 Datastore Creation attribute "+
        fromname+" already exists."); }
    // add the value attribute
    attr = (DKAttrDefICM) dsDefICM.retrieveAttr(fromname);
    attr.setNullable(true);
    attr.setUnique(false);
    lt.addAttr(attr);
    // create the to attribute for the data content
    if (!MCRCM8ItemType.createAttributeVarChar(connection,toname,tolen,
      false)) {
      logger.warn( "CM8 Datastore Creation attribute "+
        toname+" already exists."); }
    // add the value attribute
    attr = (DKAttrDefICM) dsDefICM.retrieveAttr(toname);
    attr.setNullable(true);
    attr.setUnique(false);
    lt.addAttr(attr);
    }
  catch (Exception e) {
    throw new MCRPersistenceException(e.getMessage(),e); }
  return lt;
  }

}
