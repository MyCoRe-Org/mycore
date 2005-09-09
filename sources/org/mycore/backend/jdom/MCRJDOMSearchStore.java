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

import org.apache.log4j.Logger;

import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectSearchStoreInterface;
import org.mycore.datamodel.metadata.MCRNormalizeText;

/**
 * This class is the persistence layer for a nativ memory JDOM databases.
 * 
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 */
public final class MCRJDOMSearchStore implements MCRObjectSearchStoreInterface {
	private static final Logger logger = Logger
			.getLogger(MCRJDOMSearchStore.class.getName());

	private static final MCRJDOMMemoryStore store = MCRJDOMMemoryStore
			.instance();

	/**
	 * Creates a new JDOM search store
	 */
	public MCRJDOMSearchStore() {
	}

	/**
	 * Creates an object in the search store.
	 * 
	 * @param obj
	 *            the MCRObject to put in the search store
	 * @exception MCRConfigurationException
	 *                if the configuration is not correct
	 * @exception MCRPersistenceException
	 *                if a persistence problem is occured
	 */
	public void create(MCRBase obj) throws MCRConfigurationException,
			MCRPersistenceException {
		MCRObjectID mcr_id = obj.getId();
		MCRNormalizeText.normalizeMCRObject(obj);
		org.jdom.Element root = obj.createXML().detachRootElement();
		store.addElement(mcr_id, root);
		logger.debug("MCRJDOMSearchStore create: MCRObjectID : "
				+ mcr_id.getId());
	}

	/**
	 * This operation is not necessary for this store, so the method does
	 * nothing.
	 */
	public void createDataBase(String mcr_type, org.jdom.Document mcr_conf) {
		logger.info("Create database operation is not needed for this store.");
	}

	/**
	 * Updates an object in the search store.
	 * 
	 * @param obj
	 *            the MCRObject to update in the search store
	 * @exception MCRConfigurationException
	 *                if the configuration is not correct
	 * @exception MCRPersistenceException
	 *                if a persistence problem occured
	 */
	public void update(MCRBase obj) throws MCRConfigurationException,
			MCRPersistenceException {
		MCRObjectID mcr_id = obj.getId();
		MCRNormalizeText.normalizeMCRObject(obj);
		org.jdom.Element root = obj.createXML().detachRootElement();
		store.removeElement(mcr_id);
		store.addElement(mcr_id, root);
		logger.debug("MCRJDOMSearchStore update: MCRObjectID : "
				+ mcr_id.getId());
	}

	/**
	 * Deletes an object from the search store.
	 * 
	 * @param mcr_id
	 *            the id of the object that should be deleted
	 * @exception MCRConfigurationException
	 *                if the configuration is not correct
	 * @exception MCRPersistenceException
	 *                if a persistence problem occured
	 */
	public void delete(MCRObjectID mcr_id) throws MCRPersistenceException {
		logger.debug("MCRJDOMSearchStore delete: MCRObjectID : "
				+ mcr_id.getId());
		store.removeElement(mcr_id);
	}
}
