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

/**
 * This interface is designed to choose the Persistence for the project.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public interface MCRObjectPersistenceInterface
{

/**
 * The methode create a object in the data store.
 *
 * @param mcr_id      the object id
 * @param mcr_label   the object label
 * @param mcr_service the service class for the object
 * @param xml         the XML stream from the object
 * @param ts          the text search stream from the object
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public void create(MCRObjectID mcr_id, String mcr_label, 
  MCRObjectService mcr_service, String xml, String ts) 
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
 * The methode receive a object as XML stream from the data store.
 *
 * @param mcr_id   the MyCoRe object ID
 * @return the XML stream of the object as string
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public String receive(MCRObjectID mcr_id)
  throws MCRConfigurationException, MCRPersistenceException;

/**
 * The methode receive the creation date of a object from the data store.
 *
 * @param mcr_id   the MyCoRe object ID
 * @return the creation date
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public GregorianCalendar receiveCreateDate(MCRObjectID mcr_id)
  throws MCRConfigurationException, MCRPersistenceException;

/**
 * The methode receive the label of object from the data store.
 *
 * @param mcr_id   the MyCoRe object ID
 * @return the label
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public String receiveLabel(MCRObjectID mcr_id)
  throws MCRConfigurationException, MCRPersistenceException;

/**
 * The methode update a object in the data store.
 *
 * @param mcr_id      the object id
 * @param mcr_label   the object label
 * @param mcr_service the service class for the object
 * @param xml         the XML stream from the object
 * @param ts          the text search stream from the object
 * @exception MCRConfigurationException if the configuration is not correct
 * @exception MCRPersistenceException if a persistence problem is occured
 **/
public void update(MCRObjectID mcr_id, String mcr_label, 
  MCRObjectService mcr_service, String xml, String ts) 
  throws MCRConfigurationException, MCRPersistenceException;

}

