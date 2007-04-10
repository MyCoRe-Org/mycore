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

package org.mycore.common.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.services.fieldquery.MCRSearcherFactory;

/**
 * Acts as a multiplexer to forward events that are created to all registered
 * event handlers, in the order that is configured in mycore properties. For
 * information how to configure, see MCREventHandler javadocs.
 * 
 * @see MCREventHandler
 * @see MCREventHandlerBase
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
        if (instance == null) {
            instance = new MCREventManager();
        }

        return instance;
    }

    /** Table of all configured event handlers * */
    private Hashtable handlers;

    private MCREventManager() {
        handlers = new Hashtable();

        MCRConfiguration config = MCRConfiguration.instance();

        String prefix = "MCR.EventHandler.";

        Properties props = config.getProperties(prefix);

        if (props == null) {
            return;
        }

        List names = new ArrayList();
        names.addAll(props.keySet());
        Collections.sort(names);

        List instances = null;

        for (int i = 0; i < names.size(); i++) {
            String name = (String) (names.get(i));

            StringTokenizer st = new StringTokenizer(name, ".");
            st.nextToken();
            st.nextToken();

            String type = st.nextToken();
            int nr = Integer.parseInt(st.nextToken());
            String mode = st.nextToken(); // "Class" or "Indexer"

            if (nr == 1) {
                instances = new ArrayList();
                handlers.put(type, instances);
            }

            logger.debug("EventManager instantiating handler " + config.getString(name) + " for type " + type);

            Object handler = null;

            if ("Class".equals(mode)) {
                handler = config.getSingleInstanceOf(name);
            } else { // "Indexer"
                handler = MCRSearcherFactory.getSearcher(config.getString(name));
            }

            if (!(handler instanceof MCREventHandler)) {
                String msg = "Error: Class does not implement MCREventHandler: " + name;
                throw new MCRConfigurationException(msg);
            }

            instances.add(handler);
        }
    }

    /** Call event handlers in forward direction (create, update) */
    public final static boolean FORWARD = true;

    /** Call event handlers in backward direction (delete) */
    public final static boolean BACKWARD = false;

    /**
     * This method is called by the component that created the event and acts as
     * a multiplexer that invokes all registered event handlers doHandleEvent
     * methods. If something goes wrong and an exception is caught, the
     * undoHandleEvent methods of all event handlers that are at a position
     * BEFORE the failed one, will be called in reversed order. The parameter
     * direction controls the order in which the event handlers are called.
     * 
     * @see MCREventHandler#doHandleEvent
     * @see MCREventHandlerBase
     * 
     * @param evt
     *            the event that happened
     * @param direction
     *            the order in which the event handlers are called
     */
    public void handleEvent(MCREvent evt, boolean direction) {
        List list = (List) (handlers.get(evt.getObjectType()));
        if (list == null)
            return;

        int first = (direction ? 0 : list.size() - 1);
        int last = (direction ? list.size() - 1 : 0);
        int step = (direction ? 1 : -1);
        int undoPos = first;

        for (int i = first; i != last + step; i += step) {
            MCREventHandler eh = (MCREventHandler) (list.get(i));
            logger.debug("EventManager " + evt.getObjectType() + " " + evt.getEventType() + " calling handler " + eh.getClass().getName());

            try {
                eh.doHandleEvent(evt);
            } catch (Exception ex) {
                logger.info("Exception caught while calling event handler", ex);
                logger.info("Trying rollback by calling undo method of event handlers");

                undoPos = i;

                break;
            }
        }

        // Rollback by calling undo of successfull handlers
        for (int i = undoPos - step; i != first - step; i -= step) {
            MCREventHandler eh = (MCREventHandler) (list.get(i));
            logger.debug("EventManager " + evt.getObjectType() + " " + evt.getEventType() + " calling undo of handler " + eh.getClass().getName());

            try {
                eh.undoHandleEvent(evt);
            } catch (Exception ex) {
                logger.info("Exception caught while calling undo of event handler", ex);
            }
        }
    }

    /** Same as handleEvent( evt, MCREventManager.FORWARD ) */
    public void handleEvent(MCREvent evt) throws MCRException {
        handleEvent(evt, MCREventManager.FORWARD);
    }
}
