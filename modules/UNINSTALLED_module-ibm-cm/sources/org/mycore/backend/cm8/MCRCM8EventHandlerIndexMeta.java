/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.backend.cm8;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

import com.ibm.mm.sdk.server.DKDatastoreICM;

/**
 * This class builds indexes from mycore meta data in a temporary JDOM store.
 * 
 * @author Jens Kupferschmidt
 */
public class MCRCM8EventHandlerIndexMeta extends MCREventHandlerBase {
    // the logger
    private static Logger LOGGER = Logger.getLogger(MCRCM8EventHandlerIndexMeta.class);

    // the configuration
    private static MCRConfiguration CONFIG = MCRConfiguration.instance();

    /**
     * This class create an index of meta data objects in the CM8 metadata
     * store.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        // save the start time
        long t1 = System.currentTimeMillis();

        // create
        MCRObjectID mcr_id = obj.getId();
        LOGGER.debug("MCRCM8EventHandlerIndexMeta create: MCRObjectID : " + mcr_id.getId());

        // Read the item type name from the configuration
        StringBuffer sb = new StringBuffer("MCR.persistence_cm8_");
        sb.append(mcr_id.getTypeId().toLowerCase());
        String itemtypename = CONFIG.getString(sb.toString());
        String itemtypeprefix = CONFIG.getString(sb + "_prefix");

        // set up data to item
        DKDatastoreICM connection = null;

        try {
            connection = MCRCM8ConnectionPool.instance().getConnection();

            boolean test = false;

            try {
                if (new MCRCM8Item(mcr_id.getId(), connection, itemtypename, itemtypeprefix) != null) {
                    test = true;
                }
            } catch (MCRPersistenceException e) {
            }

            if (test) {
                throw new MCRPersistenceException("A object with ID " + mcr_id.getId() + " exists.");
            }

            MCRCM8Item item = new MCRCM8Item(connection, itemtypename);
            item.setAttribute("/", itemtypeprefix + "ID", mcr_id.getId());
            item.setAttribute("/", itemtypeprefix + "label", obj.getLabel());
            fillItem(item, obj, itemtypename, itemtypeprefix);

            // create the item
            item.create();
            LOGGER.info("MCRCM8EventHandlerIndexMeta Item " + mcr_id.getId() + " was created.");
        } catch (Exception e) {
            throw new MCRPersistenceException("Error while creating data in CM8 store.", e);
        } finally {
            MCRCM8ConnectionPool.instance().releaseConnection(connection);
        }

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (double) (t2 - t1) / 1000.0;
        LOGGER.debug("MCRCM8EventHandlerIndexMeta create: done in " + diff + " sec.");
    }

    /**
     * This class update an index of meta data objects in the temporary JDOM
     * tree.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        // save the start time
        long t1 = System.currentTimeMillis();

        // update
        MCRObjectID mcr_id = obj.getId();
        LOGGER.debug("MCRCM8EventHandlerIndexMeta update: MCRObjectID : " + mcr_id.getId());

        // delete the item with the MCRObjectID
        handleObjectDeleted(evt, obj);

        // create the item with the MCRObject
        handleObjectCreated(evt, obj);

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (double) (t2 - t1) / 1000.0;
        LOGGER.debug("MCRCM8EventHandlerIndexMeta update: done in " + diff + " sec.");
    }

    /**
     * This class delete an index of meta data objects from the temporary JDOM
     * tree.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        // save the start time
        long t1 = System.currentTimeMillis();

        // delete
        MCRObjectID mcr_id = obj.getId();
        LOGGER.debug("MCRCM8EventHandlerIndexMeta delete: MCRObjectID : " + mcr_id.getId());

        // Read the item type name from the configuration
        StringBuffer sb = new StringBuffer("MCR.persistence_cm8_");
        sb.append(mcr_id.getTypeId().toLowerCase());
        String itemtypename = MCRConfiguration.instance().getString(sb.toString());
        String itemtypeprefix = MCRConfiguration.instance().getString(sb + "_prefix");

        // delete data item
        DKDatastoreICM connection = null;
        try {
            connection = MCRCM8ConnectionPool.instance().getConnection();
            MCRCM8Item item = null;
            try {
                item = new MCRCM8Item(mcr_id.getId(), connection, itemtypename, itemtypeprefix);
                item.delete();
                LOGGER.info("MCRCM8EventHandlerIndexMeta Item " + mcr_id.getId() + " was deleted.");
            } catch (MCRPersistenceException e) {
                LOGGER.warn("MCRCM8EventHandlerIndexMeta A object with ID " + mcr_id.getId() + " does not exist.");
            }
        } catch (Exception e) {
            throw new MCRPersistenceException(e.getMessage());
        } finally {
            MCRCM8ConnectionPool.instance().releaseConnection(connection);
        }

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (double) (t2 - t1) / 1000.0;
        LOGGER.debug("MCRCM8EventHandlerIndexMeta delete: done in " + diff + " sec.");
    }

    /**
     * This class update an index of meta data objects in the temporary JDOM
     * tree.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        // save the start time
        long t1 = System.currentTimeMillis();

        // update
        MCRObjectID mcr_id = obj.getId();
        LOGGER.debug("MCRCM8EventHandlerIndexMeta repair: MCRObjectID : " + mcr_id.getId());

        // delete the item with the MCRObjectID
        handleObjectDeleted(evt, obj);

        // create the item with the MCRObject
        handleObjectCreated(evt, obj);

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (double) (t2 - t1) / 1000.0;
        LOGGER.debug("MCRCM8EventHandlerIndexMeta repair: done in " + diff + " sec.");
    }

    /**
     * This private method prepare the CM8 item to store.
     * 
     * @param item
     *            the CM8 item
     * @param obj
     *            the MCRObject to store
     * @return the complete item
     */
    private final void fillItem(MCRCM8Item item, MCRObject obj, String itemtypename, String itemtypeprefix) throws Exception {
        StringBuffer mcr_ts = new StringBuffer();
        mcr_ts.append(obj.createTextSearch());
        item.setAttribute("/", itemtypeprefix + "ts", mcr_ts.toString());

    }
}
