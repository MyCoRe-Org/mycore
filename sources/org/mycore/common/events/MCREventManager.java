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

import java.util.*;

import org.apache.log4j.Logger;
import org.mycore.common.*;

/**
 * Acts as a multiplexer to forward events that are created to all registered
 * event handlers, in the order that is configured in mycore properties. For
 * information how to configure, see MCREventHandler javadocs.
 * 
 * @see MCREventHandler
 * 
 * @author Frank Lützenkirchen
 */
public class MCREventManager {
    private static Logger logger = Logger.getLogger(MCREventManager.class);

    private static MCREventManager instance;

    /**
     * The singleton manager instance
     * 
     * @return the single event manager
     */
    public static synchronized MCREventManager instance() {
        if (instance == null)
            instance = new MCREventManager();
        return instance;
    }

    /** Ordered list of all configured event handlers * */
    private List handlers;

    private MCREventManager() {
        handlers = new ArrayList();
        MCRConfiguration config = MCRConfiguration.instance();

        String prefix = "MCR.EventHandler.";
        String suffix = ".class";

        for (int i = 1;; i++) {
            String prop = prefix + i + suffix;
            String name = config.getString(prop, null);
            if (name == null)
                break;
            logger.debug("EventManager instantiating handler " + name);
            handlers.add(config.getInstanceOf(prop));
        }
    }

    /**
     * This method is called by the component that created the event and acts as
     * a multiplexer that invokes all registered event handlers doHandleEvent
     * methods.
     * 
     * @see MCREvent#doHandleEvent
     * 
     * @param evt
     *            the event that happened
     */
    public void handleEvent(MCREvent evt) throws MCRException {
        for (int i = 0; i < handlers.size(); i++) {
            MCREventHandler eh = (MCREventHandler) (handlers.get(i));
            logger.debug("EventManager calling handler "
                    + eh.getClass().getName());
            eh.doHandleEvent(evt);
        }
    }
}