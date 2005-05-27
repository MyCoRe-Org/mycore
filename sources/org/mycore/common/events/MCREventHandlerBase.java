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

package org.mycore.common.events;

import org.apache.log4j.Logger;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * Abstract helper class that can be subclassed to implement event handlers more
 * easily.
 * 
 * @author Frank Lützenkirchen
 */
public abstract class MCREventHandlerBase implements MCREventHandler {
    private static Logger logger = Logger.getLogger(MCREventHandlerBase.class);

    public void doHandleEvent(MCREvent evt) {
        MCRObject obj = (MCRObject) (evt.get("object"));
        if (obj != null) {
            logger.debug(getClass().getName() + " handling "
                    + obj.getId().getId() + evt.getTypeString());
        }

        switch (evt.getType()) {
        case MCREvent.OBJECT_CREATED:
            handleObjectCreated(evt, obj);
            break;
        case MCREvent.OBJECT_UPDATED:
            handleObjectUpdated(evt, obj);
            break;
        case MCREvent.OBJECT_DELETED:
            handleObjectDeleted(evt, obj);
            break;
        }
    }

    /**
     * Handles object created events. This implementation does nothing and
     * should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        logger.debug("This default handler implementation does nothing");
    }

    /**
     * Handles object updated events. This implementation does nothing and
     * should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        logger.debug("This default handler implementation does nothing");
    }

    /**
     * Handles object deleted events. This implementation does nothing and
     * should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        logger.debug("This default handler implementation does nothing");
    }
}