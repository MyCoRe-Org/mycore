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

package org.mycore.backend.jdom;

import org.mycore.common.*;
import org.mycore.datamodel.metadata.*;

import org.apache.log4j.Logger;

/**
 * This class is the persistence layer for a nativ memory JDOM databases.
 *
 * @author Jens Kupferschmidt
 **/
public final class MCRJDOMSearchStore implements MCRObjectSearchStoreInterface {

static final private Logger logger = Logger.getLogger( MCRJDOMSearchStore.class.getName() );
static final private MCRConfiguration config = MCRConfiguration.instance();

/**
 * The constructor of this class.
 **/
public MCRJDOMSearchStore() {
  }

/**
 * The methode create an object in the search store.
 *
 * @param obj    the MCRObject to put in the search store
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void create(MCRBase obj) throws MCRConfigurationException, MCRPersistenceException
  {
  MCRObjectID mcr_id = obj.getId();
  logger.debug("MCRJDOMSearchStore create: MCRObjectID : "+mcr_id.getId());
  org.jdom.Element root = obj.createXML().getRootElement();
  root.detach();
  MCRJDOMMemoryStore.instance().addElementOfType(mcr_id.getTypeId(),root);
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
  { logger.info("This feature does not exist for this store."); }

/**
 * The methode update an object in the data store.
 *
 * @param obj    the MCRObject to put in the search store
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public final void update(MCRBase obj) throws MCRConfigurationException, MCRPersistenceException
  {
  MCRObjectID mcr_id = obj.getId();
  logger.debug("MCRJDOMSearchStore update: MCRObjectID : "+mcr_id.getId());
  org.jdom.Element root = obj.createXML().getRootElement();
  root.detach();
  MCRJDOMMemoryStore.instance().removeElementOfType(mcr_id.getTypeId(),mcr_id);
  MCRJDOMMemoryStore.instance().addElementOfType(mcr_id.getTypeId(),root);
  }

/**
 * The methode delete an object from the data store.
 * @param mcr_id      the object id
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 */
public void delete(MCRObjectID mcr_id) throws MCRPersistenceException {
  logger.debug("MCRJDOMSearchStore delete: MCRObjectID : "+mcr_id.getId());
  MCRJDOMMemoryStore.instance().removeElementOfType(mcr_id.getTypeId(),mcr_id);
  }

}

