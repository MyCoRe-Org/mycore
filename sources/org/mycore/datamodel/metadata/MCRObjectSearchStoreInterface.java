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
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRPersistenceException;

/**
 * This interface is designed to choose the search store for the project.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public interface MCRObjectSearchStoreInterface
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
 * The methode create a object in the search stores.
 *
 * @param obj         the MCRBase to put in the search stores
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public void create(MCRBase obj) throws MCRConfigurationException, MCRPersistenceException;

/**
 * The methode delete a object from the search store.
 *
 * @param mcr_id   the MyCoRe object ID
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public void delete(MCRObjectID mcr_id)
  throws MCRConfigurationException, MCRPersistenceException;

/**
 * The methode update a object in the search store.
 *
 * @param obj         the MCRObject to put in the search stores
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public void update(MCRBase obj) throws MCRConfigurationException, MCRPersistenceException;

}

