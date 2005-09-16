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
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRNormalizeText;

/**
 * This class builds indexes from mycore meta data in a temporary XMLDB store.
 * 
 * @author Jens Kupferschmidt
 */
public class MCRXMLDBEventHandlerIndexMeta extends MCREventHandlerBase {

  // the LOGGER
  private static Logger LOGGER = Logger.getLogger(MCRXMLDBEventHandlerIndexMeta.class);

  // the configuration
  private static final MCRConfiguration config = MCRConfiguration.instance();

  /**
   * Creates a new MCRXMLDBEventHandlerIndexMeta.
   */
  public MCRXMLDBEventHandlerIndexMeta() throws MCRPersistenceException {
    MCRXMLDBConnectionPool.instance();
    }

  /**
   * This class create an index of meta data objects in the temporary XMLDB tree.
   * 
   * @param evt the event that occured
   * @param obj the MCRObject that caused the event
   */
  protected final void handleObjectCreated(MCREvent evt, MCRObject obj) {
    // save the start time
    long t1 = System.currentTimeMillis();
    // store in the collection
    Collection collection = null;
    try {
      MCRObjectID mcr_id = obj.getId();
      LOGGER.debug("MCRXMLDBEventHandlerIndexMeta create: MCRObjectID : "+ mcr_id.getId()+" - "+obj.getLabel());
      // normalize text fields
      MCRNormalizeText.normalizeMCRObject(obj);
      // open the collection
      collection = MCRXMLDBConnectionPool.instance().getConnection(obj.getId().getTypeId());
      // check that the item not exist
      XMLResource res = (XMLResource) collection.getResource(obj.getId().getId());
      if (res != null) {
        throw new MCRPersistenceException("An object with ID " + obj.getId().getId() + " exists.");
        }
      // create a new item
      res = (XMLResource) collection.createResource(mcr_id.getId(),XMLResource.RESOURCE_TYPE);
      SAXOutputter outputter = new SAXOutputter(res.setContentAsSAX());
      outputter.output(obj.createXML());
      collection.storeResource(res);
      } catch (Exception e) {
        throw new MCRPersistenceException(e.getMessage(), e);
      }
    // save the stop time
    long t2 = System.currentTimeMillis();
    double diff = (double) (t2 - t1) / 1000.0;
    LOGGER.debug("MCRXMLDBEventHandlerIndexMeta create: done in "+diff+" sec.");
    }

  /**
   * This class update an index of meta data objects in the temporary XMLDB tree.
   * 
   * @param evt the event that occured
   * @param obj the MCRObject that caused the event
   */
  protected final void handleObjectUpdated(MCREvent evt, MCRObject obj) {
    // save the start time
    long t1 = System.currentTimeMillis();
    // store in the collection
    Collection collection = null;
    try {
      MCRObjectID mcr_id = obj.getId();
      LOGGER.debug("MCRXMLDBEventHandlerIndexMeta update: MCRObjectID : "+ mcr_id.getId()+" - "+obj.getLabel());
      // normalize text fields
      MCRNormalizeText.normalizeMCRObject(obj);
      // open the collection
      collection = MCRXMLDBConnectionPool.instance().getConnection(obj.getId().getTypeId());
      // check that the item not exist
      XMLResource res = (XMLResource) collection.getResource(obj.getId().getId());
      if (res == null) {
        LOGGER.warn("An object with ID " + obj.getId().getId() + " does not exist.");
        }
      else {
        handleObjectDeleted(evt,obj);
        }
      // create the new item
      res = (XMLResource) collection.createResource(mcr_id.getId(),XMLResource.RESOURCE_TYPE);
      SAXOutputter outputter = new SAXOutputter(res.setContentAsSAX());
      outputter.output(obj.createXML());
      collection.storeResource(res);
      } catch (Exception e) {
        throw new MCRPersistenceException(e.getMessage(), e);
      }
    // save the stop time
    long t2 = System.currentTimeMillis();
    double diff = (double) (t2 - t1) / 1000.0;
    LOGGER.debug("MCRXMLDBEventHandlerIndexMeta update: done in "+diff+" sec.");
    }

  /**
   * This class delete an index of meta data objects from the temporary XMLDB tree.
   * 
   * @param evt the event that occured
   * @param obj the MCRObject that caused the event
   */
  protected final void handleObjectDeleted(MCREvent evt, MCRObject obj) {
    // save the start time
    long t1 = System.currentTimeMillis();
    // delete the item
    Collection collection = null;
    try {
      MCRObjectID mcr_id = obj.getId();
      LOGGER.debug("MCRXMLDBEventHandlerIndexMeta delete: MCRObjectID : " + mcr_id.getId());
      collection = MCRXMLDBConnectionPool.instance().getConnection(mcr_id.getTypeId());
      Resource document = collection.getResource(mcr_id.getId());
      if (null != document) {
        collection.removeResource(document);
        } else {
        LOGGER.warn("An object with ID " + mcr_id.getId() + " does not exist.");
        }
      } catch (Exception e) {
        throw new MCRPersistenceException(e.getMessage(), e);
      }
    // save the stop time
    long t2 = System.currentTimeMillis();
    double diff = (double) (t2 - t1) / 1000.0;
    LOGGER.debug("MCRXMLDBEventHandlerIndexMeta delete: done in "+diff+" sec.");
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


