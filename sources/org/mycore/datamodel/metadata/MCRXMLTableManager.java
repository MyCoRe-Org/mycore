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

package org.mycore.datamodel.metadata;

import java.util.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.mycore.common.*;

/**
 * This class manage  all accesses to the XML table database. This database
 * holds all informations about the MCRObjectID and the corresponding XML file.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRXMLTableManager
{

/** The link table manager singleton */
protected static MCRXMLTableManager singleton;

// logger
static Logger logger=Logger.getLogger(MCRLinkTableManager.class.getName());

// the list of link table 
private String persistclassname = null;
private Hashtable tablelist;

/**
 * Returns the link table manager singleton.
 **/
public static synchronized MCRXMLTableManager instance()
  {
  if( singleton == null ) singleton = new MCRXMLTableManager();
  return singleton;
  }

/**
 * The constructor of this class.
 **/
protected MCRXMLTableManager()
  {
  MCRConfiguration config = MCRConfiguration.instance();
  // set the logger property
  PropertyConfigurator.configure(config.getLoggingProperties());
  // Load the persistence class
  persistclassname = config.getString("MCR.xml_store_class");
  tablelist = new Hashtable();
  }


/**
 * The method check the type.
 *
 * @param type the table type
 * @exception if the store for the given type could not find or loaded.
 **/
private final MCRXMLTableInterface checkType(String type)
  {
  if ((type == null) || ((type = type.trim()).length() ==0)) {
    throw new MCRException("The type is null or empty."); }
  MCRXMLTableInterface store = (MCRXMLTableInterface)tablelist.get(type);
  if (store != null) { return store; }
  Object obj = new Object();
  try {
    obj = Class.forName(persistclassname).newInstance(); }
  catch (ClassNotFoundException e) {
    throw new MCRException(persistclassname+" ClassNotFoundException for "+
      type); }
  catch (IllegalAccessException e) {
    throw new MCRException(persistclassname+" IllegalAccessException for "+
      type); }
  catch (InstantiationException e) {
    throw new MCRException(persistclassname+" InstantiationException for "+
      type); }
  try {
    ((MCRXMLTableInterface)obj).init(type); }
  catch (Exception e) {
    throw new MCRException("Could not find or loaded a XML store for the type "
    +type,e); 
    }
  tablelist.put(type,obj);
  return (MCRXMLTableInterface)obj;
  }

/**
 * The method create a new item in the datastore.
 *
 * @param type the type of the metadata
 * @param mcrid a MCRObjectID
 * @param xml a JDOM Document 
 * @exception if the method arguments are not correct
 **/
public final void create(String type, MCRObjectID mcrid, org.jdom.Document xml)
  throws MCRException
  { checkType(type).create(mcrid,MCRUtils.getByteArray(xml),1); }

/**
 * The method create a new item in the datastore.
 *
 * @param type the type of the metadata
 * @param mcrid a MCRObjectID
 * @param xml a byte array with the XML file
 * @exception if the method arguments are not correct
 **/
public final void create(String type, MCRObjectID mcrid, byte[] xml)
  throws MCRException
  { checkType(type).create(mcrid,xml,1); }

/**
 * The method remove a item for the MCRObjectID from the datastore.
 *
 * @param type the type of the metadata
 * @param mcrid a MCRObjectID
 * @exception if the method argument is not correct
 **/
public final void delete( String type, MCRObjectID mcrid )
  throws MCRException
  { checkType(type).delete(mcrid,1); }

/**
 * The method update an item in the datastore.
 *
 * @param type the type of the metadata
 * @param mcrid a MCRObjectID
 * @param xml a byte array with the XML file
 * @exception if the method arguments are not correct
 **/
public final void update( String type, MCRObjectID mcrid, org.jdom.Document xml)
  throws MCRException
  { checkType(type).update(mcrid,MCRUtils.getByteArray(xml),1); }

/**
 * The method update an item in the datastore.
 *
 * @param type the type of the metadata
 * @param mcrid a MCRObjectID
 * @param xml a byte array with the XML file
 * @exception if the method arguments are not correct
 **/
public final void update( String type, MCRObjectID mcrid, byte[] xml)
  throws MCRException
  { checkType(type).update(mcrid,xml,1); }

/**
 * The method retrieve a dataset for the given MCRObjectID and returns
 * the corresponding XML file as byte array.
 *
 * @param mcrid a MCRObjectID
 * @exception if the method arguments are not correct
 **/
public final byte[] retrieve( String type, MCRObjectID mcrid)
  throws MCRException
  { return checkType(type).retrieve(mcrid,1); }

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
public final int getNextFreeIdInt( String type, String idproject, String idtype )
  throws MCRPersistenceException
  { return checkType(type).getNextFreeIdInt(idproject,idtype); }

/**
 * This method check that the MCRObjectID exist in this store.
 *
 * @param mcrid a MCRObjectID
 * @return true if the MCRObjectID exist, else return false
 **/
public final boolean exist( String type, MCRObjectID mcrid)
  { return checkType(type).exist(mcrid,1); }

/**
 * The method return a Array list with all stored MCRObjectID's of the
 * XML table of a MCRObjectID type.
 *
 * @param type a MCRObjectID type string
 * @return a ArrayList of MCRObjectID's
 **/
public ArrayList retrieveAllIDs(String type)
  { return checkType(type).retrieveAllIDs(type); }

}

