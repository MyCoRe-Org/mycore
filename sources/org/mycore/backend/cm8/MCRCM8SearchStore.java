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

import java.text.*;
import java.util.*;

import com.ibm.mm.sdk.server.*;
import com.ibm.mm.sdk.common.*;

import org.apache.log4j.Logger;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectSearchStoreInterface;
import org.mycore.datamodel.metadata.MCRTypedContent;

/**
 * This class implements all methode for handling the data to the data store
 * based on IBM Content Manager 8.
 *
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 *
 * @version $Revision$ $Date$
 **/
public final class MCRCM8SearchStore implements MCRObjectSearchStoreInterface
{

// from configuration

/**
 * The constructor of this class.
 **/
public MCRCM8SearchStore()
  {
  }

/**
 * The methode create an object in the search store. The index class
 * is determinated by the type of the object ID. This <b>must</b>
 * correspond with the lower case configuration name.<br>
 * As example: Document --> MCR.persistence_cm8_document
 *
 * @param obj    the MCRObject to put in the search store
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void create(MCRBase obj) throws MCRConfigurationException, MCRPersistenceException
  {
  Logger logger = MCRCM8ConnectionPool.getLogger();
  // get root data
  MCRObjectID mcr_id = obj.getId();
  String mcr_label = obj.getLabel();
  MCRTypedContent mcr_tc = obj.createTypedContent();
  String mcr_ts = obj.createTextSearch();
  int mcr_tc_counter = 0;
  for (int i=0;i<mcr_tc.getSize();i++) {
    if (mcr_tc.getNameElement(i).equals("ID")) {
      mcr_tc_counter = i+2; }
    }
  // Read the item type name from the configuration
  StringBuffer sb = new StringBuffer("MCR.persistence_cm8_");
  sb.append(mcr_id.getTypeId().toLowerCase());
  String itemtypename = MCRConfiguration.instance().getString(sb.toString()); 
  String itemtypeprefix = MCRConfiguration.instance().getString(sb+"_prefix");
  // set up data to item
  DKDatastoreICM connection = null;
  try {
    connection = MCRCM8ConnectionPool.instance().getConnection();
    boolean test = false;
    try {
      if (new MCRCM8Item(mcr_id.getId(),connection,itemtypename,itemtypeprefix) != null) 
      	test = true;
    }
    catch (MCRPersistenceException e) { }
    if (test) {
      throw new MCRPersistenceException("A object with ID "+mcr_id.getId()+
        " exists."); }
    MCRCM8Item item = new MCRCM8Item(connection,itemtypename);
    item.setAttribute("/",itemtypeprefix+"ID",mcr_id.getId());
    item.setAttribute("/",itemtypeprefix+"label",mcr_label);
    item.setAttribute("/",itemtypeprefix+"ts",mcr_ts);

    String [] xmlpath = new String[MCRTypedContent.TYPE_LASTTAG+1];
    int lastpath = 0;

    // set the metadata children data
    for (int i=mcr_tc_counter;i<mcr_tc.getSize();i++) {
      // tag is 'metadata'
      if ((mcr_tc.getNameElement(i).equals("metadata")) &&
          (mcr_tc.getTypeElement(i) == MCRTypedContent.TYPE_MASTERTAG)) {
        xmlpath[MCRTypedContent.TYPE_MASTERTAG] = itemtypeprefix+"metadata";
        lastpath = MCRTypedContent.TYPE_MASTERTAG;
        item.setChild(connection,itemtypename,xmlpath[lastpath],"/",
          "/"+xmlpath[lastpath]+"/");
        item.setAttribute("/"+xmlpath[lastpath]+"/",itemtypeprefix+"lang",
          mcr_tc.getValueElement(i+1));
        i++;
        continue; 
        }
      // tag is 'structure'
      if ((mcr_tc.getNameElement(i).equals("structure")) &&
          (mcr_tc.getTypeElement(i) == MCRTypedContent.TYPE_MASTERTAG)) {
        xmlpath[MCRTypedContent.TYPE_MASTERTAG] = itemtypeprefix+"structure";
        lastpath = MCRTypedContent.TYPE_MASTERTAG;
        item.setChild(connection,itemtypename,xmlpath[lastpath],"/",
          "/"+xmlpath[lastpath]+"/");
        continue; 
        }
      // tag is 'service'
      if ((mcr_tc.getNameElement(i).equals("service")) &&
          (mcr_tc.getTypeElement(i) == MCRTypedContent.TYPE_MASTERTAG)) {
        xmlpath[MCRTypedContent.TYPE_MASTERTAG] = itemtypeprefix+"service";
        lastpath = MCRTypedContent.TYPE_MASTERTAG;
        item.setChild(connection,itemtypename,xmlpath[lastpath],"/",
          "/"+xmlpath[lastpath]+"/");
        continue; 
        }
      // tag is 'derivate'
      if ((mcr_tc.getNameElement(i).equals("derivate")) &&
          (mcr_tc.getTypeElement(i) == MCRTypedContent.TYPE_MASTERTAG)) {
        xmlpath[MCRTypedContent.TYPE_MASTERTAG] = itemtypeprefix+"derivate";
        lastpath = MCRTypedContent.TYPE_MASTERTAG;
        item.setChild(connection,itemtypename,xmlpath[lastpath],"/",
          "/"+xmlpath[lastpath]+"/");
        continue; 
        }
      // a path element
      if (mcr_tc.getTypeElement(i) > MCRTypedContent.TYPE_MASTERTAG) {
        xmlpath[mcr_tc.getTypeElement(i)] = new String(itemtypeprefix+
          mcr_tc.getNameElement(i));
        lastpath = mcr_tc.getTypeElement(i);
        sb = new StringBuffer(64);
        sb.append('/');
        for (int j=MCRTypedContent.TYPE_MASTERTAG;j<lastpath;j++) {
          sb.append(xmlpath[j]).append('/'); }
        item.setChild(connection,xmlpath[lastpath-1],xmlpath[lastpath],
          sb.toString(),
          sb.append(xmlpath[lastpath]).append('/').toString());
        continue; 
        }
      // set a  attribute or value
      sb = new StringBuffer(64);
      sb.append('/');
      String elname = xmlpath[lastpath];
      if (mcr_tc.getTypeElement(i) == MCRTypedContent.TYPE_ATTRIBUTE) {
        for (int j=MCRTypedContent.TYPE_MASTERTAG;j<lastpath+1;j++) {
          sb.append(xmlpath[j]).append('/'); }
        elname = itemtypeprefix+mcr_tc.getNameElement(i);
        }
      else {
        for (int j=MCRTypedContent.TYPE_MASTERTAG;j<lastpath+1;j++) {
          sb.append(xmlpath[j]).append('/'); }
        }
      Object valueobject = null;
      switch (mcr_tc.getFormatElement(i)) {
        case MCRTypedContent.FORMAT_STRING :
          valueobject = mcr_tc.getValueElement(i);
          logger.debug("Attribute : "+sb+"  "+elname+"  "+
            (String)mcr_tc.getValueElement(i));
          break;
        case MCRTypedContent.FORMAT_BOOLEAN :
          valueobject = mcr_tc.getValueElement(i);
          logger.debug("Attribute : "+sb+"  "+elname+"  "+
            (String)mcr_tc.getValueElement(i));
          break;
        case MCRTypedContent.FORMAT_DATE :
          GregorianCalendar cal = (GregorianCalendar)mcr_tc.getValueElement(i);
          int number = 0;
          if (cal.get(Calendar.ERA) == GregorianCalendar.AD) {
            number = (4000+cal.get(Calendar.YEAR))*10000 +
                     cal.get(Calendar.MONTH)*100 +
                     cal.get(Calendar.DAY_OF_MONTH); }
          else {
            number = (4000-cal.get(Calendar.YEAR))*10000 +
                     cal.get(Calendar.MONTH)*100 +
                     cal.get(Calendar.DAY_OF_MONTH); }
          valueobject = new Integer(number);
          // begin for debug
          Calendar calendar = (GregorianCalendar)mcr_tc.getValueElement(i);
          SimpleDateFormat formatter = new SimpleDateFormat ("yyyy-MM-dd");
          formatter.setCalendar(calendar);
          String datestamp = formatter.format(calendar.getTime());
          logger.debug("Attribute : "+sb+"  "+elname+"  "+datestamp);
          // end debug
          break;
        case MCRTypedContent.FORMAT_LINK :
          valueobject = mcr_tc.getValueElement(i);
          elname = itemtypeprefix+"xlink"+elname.substring(2,elname.length());
          logger.debug("Attribute : "+sb+"  "+elname+"  "+
            (String)mcr_tc.getValueElement(i));
          break;
        case MCRTypedContent.FORMAT_CLASSID :
          valueobject = mcr_tc.getValueElement(i);
          logger.debug("Attribute : "+sb+"  "+elname+"  "+
            (String)mcr_tc.getValueElement(i));
          break;
        case MCRTypedContent.FORMAT_CATEGID :
          valueobject = mcr_tc.getValueElement(i);
          logger.debug("Attribute : "+sb+"  "+elname+"  "+
            (String)mcr_tc.getValueElement(i));
          break;
        case MCRTypedContent.FORMAT_NUMBER :
          valueobject = mcr_tc.getValueElement(i);
          logger.debug("Attribute : "+sb+"  "+elname+"  "+
            ((Double)mcr_tc.getValueElement(i)).toString());
          break;
        }
      item.setAttribute(sb.toString(),elname,valueobject);
      }

    // create the item
    item.create();
    logger.info("Item "+mcr_id.getId()+" was created.");
    }
  catch (Exception e) {
    throw new MCRPersistenceException(
      "Error while creating data in CM8 store.",e); }
  finally {
    MCRCM8ConnectionPool.instance().releaseConnection(connection); }
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
  Logger logger = MCRCM8ConnectionPool.getLogger();
  // Read the item type name from the configuration
  StringBuffer sb = new StringBuffer("MCR.persistence_cm8_");
  sb.append(mcr_id.getTypeId().toLowerCase());
  String itemtypename = MCRConfiguration.instance().getString(sb.toString()); 
  String itemtypeprefix = MCRConfiguration.instance().getString(sb+"_prefix");
  // delete data item
  DKDatastoreICM connection = null;
  try {
    connection = MCRCM8ConnectionPool.instance().getConnection();
    MCRCM8Item item = null;
    try {
      item = new MCRCM8Item(mcr_id.getId(),connection,itemtypename,
        itemtypeprefix); 
      item.delete();
      logger.info("Item "+mcr_id.getId()+" was deleted.");
      }
    catch (MCRPersistenceException e) {
      logger.warn("A object with ID "+mcr_id.getId()+" does not exist."); }
    }
  catch (Exception e) {
    throw new MCRPersistenceException(e.getMessage()); }
  finally {
    MCRCM8ConnectionPool.instance().releaseConnection(connection); }
  }

/**
 * The methode update an object in the data store. The index class
 * is determinated by the type of the object ID. This <b>must</b>
 * correspond with the lower case configuration name.<br>
 * As example: Document --> MCR.persistence_cm8_document
 *
 * @param obj    the MCRObject to put in the search store
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void update(MCRBase obj) throws MCRConfigurationException, MCRPersistenceException
  {
  // delete the item with the MCRObjectID
  delete(obj.getId());
  // create the item with the MCRObject
  create(obj);
  }

}

