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

package mycore.datamodel;

import java.util.*;
import mycore.common.MCRConfigurationException;
import mycore.common.MCRPersistenceException;
import mycore.datamodel.MCRObjectID;
import mycore.datamodel.MCRTypedContent;

/**
 * This interface is designed to choose the Persistence for the project.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public interface MCRObjectPersistenceInterface
{

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
  throws MCRConfigurationException, MCRPersistenceException;

/**
 * The methode create a object in the data store.
 *
 * @param mcr_tc      the typed content
 * @param xml         the XML stream as JDOM object
 * @param mcr_ts      the text search string
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public void create(MCRTypedContent mcr_tc, org.jdom.Document xml, String mcr_ts)
  throws MCRConfigurationException, MCRPersistenceException;

/**
 * The methode delete a object from the data store.
 *
 * @param mcr_id   the MyCoRe object ID
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public void delete(MCRObjectID mcr_id)
  throws MCRConfigurationException, MCRPersistenceException;

/**
 * The methode return true if a object exits in the data store, else return
 * false.
 *
 * @param mcr_id   the MyCoRe object ID
 * @return true if the object exists, else false
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public boolean exist(MCRObjectID mcr_id)
  throws MCRConfigurationException, MCRPersistenceException;

/**
 * The methode receive a object as XML stream from the data store.
 *
 * @param mcr_id   the MyCoRe object ID
 * @return the XML stream as byte array of the object as string
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public byte [] receive(MCRObjectID mcr_id)
  throws MCRConfigurationException, MCRPersistenceException;

/**
 * The methode update a object in the data store.
 *
 * @param mcr_tc      the typed content
 * @param xml         the XML stream as JDOM object
 * @param mcr_ts      the text search string
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public void update(MCRTypedContent mcr_tc, org.jdom.Document xml, String mcr_ts)
  throws MCRConfigurationException, MCRPersistenceException;

/**
 * This methode return the next free number for a given MCRObjectId
 * base.
 *
 * @param project_id   the project ID
 * @param type_id      the type ID
 * @exception MCRPersistenceException if a persistence problem is occured
 * @return the number a string
 **/
public String getNextFreeId  (String project_id, String type_id) 
  throws MCRPersistenceException;

}

