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

package org.mycore.backend.xmldb;

import org.apache.log4j.Logger;
import org.jdom.input.SAXHandler;
import org.jdom.output.SAXOutputter;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.modules.XMLResource;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRNormalizeText;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectSearchStoreInterface;

/**
 * This class is the persistence layer for XML:DB databases.
 * 
 * @author Harald Richter
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public final class MCRXMLDBSearchStore implements MCRObjectSearchStoreInterface {
	static final private Logger logger = Logger
			.getLogger(MCRXMLDBSearchStore.class.getName());

	static final private MCRConfiguration config = MCRConfiguration.instance();

	/**
	 * Creates a new MCRXMLDBSearchStore.
	 */
	public MCRXMLDBSearchStore() throws MCRPersistenceException {
		MCRXMLDBConnectionPool.instance();
	}

	/**
	 * This method creates and stores the searchable data from MCRObject in the
	 * XMLDB datastore.
	 * 
	 * @param obj
	 *            the MCRObject to put in the search store
	 * @exception MCRPersistenceException
	 *                if an error was occured
	 */
	public final void create(MCRBase obj) throws MCRPersistenceException {
		MCRNormalizeText.normalizeMCRObject(obj);
		Collection collection = null;
		try {
			logger.debug("MCRXMLDBSearchStore create: MCRObjectID    : "
					+ obj.getId().getId());
			logger.debug("MCRXMLDBPersistence create: MCRLabel       : "
					+ obj.getLabel());
			// open the collection
			collection = MCRXMLDBConnectionPool.instance().getConnection(
					obj.getId().getTypeId());
			// check that the item not exist
			XMLResource res = (XMLResource) collection.getResource(obj.getId()
					.getId());
			if (res != null) {
				logger.debug("Check this!");
				throw new MCRPersistenceException("An object with ID "
						+ obj.getId().getId() + " exists.");
			}
			// create a new item
			res = (XMLResource) collection.createResource(obj.getId().getId(),
					XMLResource.RESOURCE_TYPE);
			SAXOutputter outputter = new SAXOutputter(res.setContentAsSAX());
			outputter.output(obj.createXML());
			collection.storeResource(res);
		} catch (Exception e) {
			throw new MCRPersistenceException(e.getMessage(), e);
		}
	}

	/**
	 * The methode create a new datastore based of given configuration. It
	 * create a new data table for storing MCRObjects with the same MCRObjectID
	 * type.
	 * 
	 * @param mcr_type
	 *            the MCRObjectID type as string
	 * @param mcr_conf
	 *            the configuration XML stream as JDOM tree
	 * @exception MCRConfigurationException
	 *                if the configuration is not correct
	 * @exception MCRPersistenceException
	 *                if a persistence problem is occured
	 */
	public final void createDataBase(String mcr_type, org.jdom.Document mcr_conf)
			throws MCRConfigurationException, MCRPersistenceException {
		logger.info("This feature exist not for this store.");
	}

	/**
	 * Updates the searchable content in the database. Currently this is the
	 * same like delete and then a new create. Should be made with XUpdate in
	 * the future.
	 * 
	 * @param obj
	 *            the MCRObject to put in the search store
	 * @exception MCRPersistenceException
	 *                if an error was occured
	 */
	public final void update(MCRBase obj) throws MCRPersistenceException {
		MCRNormalizeText.normalizeMCRObject(obj);
		Collection collection = null;
		try {
			logger.debug("MCRXMLDBSearchStore create: MCRObjectID    : "
					+ obj.getId().getId());
			logger.debug("MCRXMLDBPersistence create: MCRLabel       : "
					+ obj.getLabel());
			// open the collection
			collection = MCRXMLDBConnectionPool.instance().getConnection(
					obj.getId().getTypeId());
			// check that the item exist
			XMLResource res = (XMLResource) collection.getResource(obj.getId()
					.getId());
			if (res == null) {
				logger.warn("An object with ID " + obj.getId().getId()
						+ " does not exist.");
			} else {
				// delete the old item
				delete(obj.getId());
			}
			// create the new item
			res = (XMLResource) collection.createResource(obj.getId().getId(),
					XMLResource.RESOURCE_TYPE);
			SAXOutputter outputter = new SAXOutputter(res.setContentAsSAX());
			outputter.output(obj.createXML());
			collection.storeResource(res);
		} catch (Exception e) {
			throw new MCRPersistenceException(e.getMessage(), e);
		}
	}

	/**
	 * Deletes the object with the given object id in the datastore.
	 * 
	 * @param mcr_id
	 *            id of the object to delete
	 * 
	 * @throws MCRPersistenceException
	 *             something goes wrong during delete
	 */
	public final void delete(MCRObjectID mcr_id) throws MCRPersistenceException {
		Collection collection = null;
		logger.debug("MCRXMLDBPersistence delete: MCRObjectID    : "
				+ mcr_id.getId());
		try {
			collection = MCRXMLDBConnectionPool.instance().getConnection(
					mcr_id.getTypeId());
			Resource document = collection.getResource(mcr_id.getId());
			if (null != document) {
				collection.removeResource(document);
			} else {
				logger.warn("An object with ID " + mcr_id.getId()
						+ " does not exist.");
			}
		} catch (Exception e) {
			throw new MCRPersistenceException(e.getMessage(), e);
		}
	}

	/**
	 * A private method to convert the result in a dom tree.
	 * 
	 * @param res
	 *            the result
	 * @exception MCRPersistenceException
	 *                if an error was occured
	 * @return the DOM tree
	 */
	static final org.jdom.Document convertResToDoc(XMLResource res) {
		try {
			SAXHandler handler = new SAXHandler();
			res.getContentAsSAX(handler);
			return handler.getDocument();
		} catch (Exception e) {
			throw new MCRPersistenceException(e.getMessage(), e);
		}
	}
}
