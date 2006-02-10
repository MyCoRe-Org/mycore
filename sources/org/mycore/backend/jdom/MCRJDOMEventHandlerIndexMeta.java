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

package org.mycore.backend.jdom;

import org.apache.log4j.Logger;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This class builds indexes from mycore meta data in a temporary JDOM store.
 * 
 * @author Jens Kupferschmidt
 */
public class MCRJDOMEventHandlerIndexMeta extends MCREventHandlerBase {
    // the logger
    private static Logger LOGGER = Logger.getLogger(MCRJDOMEventHandlerIndexMeta.class);

    // the temporary store
    private static final MCRJDOMMemoryStore store = MCRJDOMMemoryStore.instance();

    /**
     * This class create an index of meta data objects in the temporary JDOM
     * tree.
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
        LOGGER.debug("MCRJDOMEventHandlerIndexMeta create: MCRObjectID : " + mcr_id.getId());

        org.jdom.Element root = obj.createXML().detachRootElement();
        store.addElement(mcr_id, root);

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (t2 - t1) / 1000.0;
        LOGGER.debug("MCRJDOMEventHandlerIndexMeta create: done in " + diff + " sec.");
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
        LOGGER.debug("MCRJDOMEventHandlerIndexMeta update: MCRObjectID : " + mcr_id.getId());

        org.jdom.Element root = obj.createXML().detachRootElement();
        store.removeElement(mcr_id);
        store.addElement(mcr_id, root);

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (t2 - t1) / 1000.0;
        LOGGER.debug("MCRJDOMEventHandlerIndexMeta update: done in " + diff + " sec.");
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
        LOGGER.debug("MCRJDOMEventHandlerIndexMeta delete: MCRObjectID : " + mcr_id.getId());
        store.removeElement(mcr_id);

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (t2 - t1) / 1000.0;
        LOGGER.debug("MCRJDOMEventHandlerIndexMeta delete: done in " + diff + " sec.");
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
        LOGGER.debug("MCRJDOMEventHandlerIndexMeta repair: MCRObjectID : " + mcr_id.getId());

        org.jdom.Element root = obj.createXML().detachRootElement();
        store.removeElement(mcr_id);
        store.addElement(mcr_id, root);

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (t2 - t1) / 1000.0;
        LOGGER.debug("MCRJDOMEventHandlerIndexMeta repair: done in " + diff + " sec.");
    }

}
