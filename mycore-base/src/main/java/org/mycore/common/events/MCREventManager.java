/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.common.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;

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

    private static Logger logger = LogManager.getLogger(MCREventManager.class);

    private static MCREventManager instance;

    public static final String CONFIG_PREFIX = "MCR.EventHandler.";

    /** Table of all configured event handlers * */
    private Hashtable<String, List<MCREventHandler>> handlers;

    /** Call event handlers in forward direction (create, update) */
    public static final boolean FORWARD = true;

    /** Call event handlers in backward direction (delete) */
    public static final boolean BACKWARD = false;

    /**
     * Parse the property key of event handlers, extract type and mode.
     * 
     * @see MCREventHandler
     * 
     * @author Huu Chi Vu
     *
     */
    private class EventHandlerProperty {

        private String type;

        private String mode;

        public EventHandlerProperty(String propertyKey) {
            String[] splitedKey = propertyKey.split("\\.");
            if (splitedKey.length != 5) {
                throw new MCRConfigurationException("Property key " + propertyKey + " for event handler not valid.");
            }

            this.setType(splitedKey[2]);
            this.setMode(splitedKey[4]);
        }

        private void setType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        private void setMode(String mode) {
            this.mode = mode;
        }

        public String getMode() {
            return mode;
        }

    }

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

    private MCREventManager() {
        handlers = new Hashtable<>();

        MCRConfiguration config = MCRConfiguration.instance();

        Map<String, String> props = config.getPropertiesMap(CONFIG_PREFIX);

        if (props == null) {
            return;
        }

        List<String> propertyKeyList = new ArrayList<>(props.size());
        for (Object name : props.keySet()) {
            String key = name.toString();
            if (!key.startsWith(CONFIG_PREFIX + "Mode.")) {
                propertyKeyList.add(key);
            }
        }
        Collections.sort(propertyKeyList);

        for (String propertyKey : propertyKeyList) {
            EventHandlerProperty eventHandlerProperty = new EventHandlerProperty(propertyKey);

            String type = eventHandlerProperty.getType();
            String mode = eventHandlerProperty.getMode();

            logger.debug("EventManager instantiating handler {} for type {}", config.getString(propertyKey), type);

            if (propKeyIsSet(propertyKey)) {
                addEventHandler(type, getEventHandler(mode, propertyKey));
            }
        }
    }

    private boolean propKeyIsSet(String propertyKey) {
        return MCRConfiguration.instance().getString(propertyKey).length() != 0;
    }

    private List<MCREventHandler> getOrCreateEventHandlerListOfType(String type) {
        return handlers.computeIfAbsent(type, k -> new ArrayList<>());
    }

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
        List<MCREventHandler> list = handlers.get(evt.getObjectType());
        if (list == null) {
            return;
        }

        int first = direction ? 0 : list.size() - 1;
        int last = direction ? list.size() - 1 : 0;
        int step = direction ? 1 : -1;
        int undoPos = first;

        Exception handleEventExceptionCaught = null;
        for (int i = first; i != last + step; i += step) {
            MCREventHandler eh = list.get(i);
            logger.debug("EventManager {} {} calling handler {}", evt.getObjectType(), evt.getEventType(),
                eh.getClass().getName());

            try {
                eh.doHandleEvent(evt);
            } catch (Exception ex) {
                handleEventExceptionCaught = ex;
                logger.error("Exception caught while calling event handler", ex);
                logger.error("Trying rollback by calling undo method of event handlers");

                undoPos = i;

                break;
            }
        }

        // Rollback by calling undo of successfull handlers
        for (int i = undoPos - step; i != first - step; i -= step) {
            MCREventHandler eh = list.get(i);
            logger.debug("EventManager {} {} calling undo of handler {}", evt.getObjectType(), evt.getEventType(),
                eh.getClass().getName());

            try {
                eh.undoHandleEvent(evt);
            } catch (Exception ex) {
                logger.error("Exception caught while calling undo of event handler", ex);
            }
        }

        if (handleEventExceptionCaught != null) {
            String msg = "Exception caught in EventHandler, rollback by calling undo of successfull handlers done.";
            throw new MCRException(msg, handleEventExceptionCaught);
        }
    }

    /** Same as handleEvent( evt, MCREventManager.FORWARD ) */
    public void handleEvent(MCREvent evt) throws MCRException {
        handleEvent(evt, MCREventManager.FORWARD);
    }

    /**
     * Appends the event handler to the end of the list.
     * 
     * @param type type of event e.g. MCRObject
     */
    public MCREventManager addEventHandler(String type, MCREventHandler handler) {
        getOrCreateEventHandlerListOfType(type).add(handler);
        return this;
    }

    /**
     * Inserts the event handler at the specified position.
     * 
     * @param type type of event e.g. MCRObject
     * @param index index at which the specified element is to be inserted
     */
    public MCREventManager addEventHandler(String type, MCREventHandler handler, int index) {
        getOrCreateEventHandlerListOfType(type).add(index, handler);
        return this;
    }

    /**
     * Removes the specified event handler.
     * 
     * @param type type of event handler
     * @param handler the event handler to remove
     */
    public MCREventManager removeEventHandler(String type, MCREventHandler handler) {
        List<MCREventHandler> handlerList = this.handlers.get(type);
        handlerList.remove(handler);
        if (handlerList.isEmpty()) {
            this.handlers.remove(type);
        }
        return this;
    }

    /**
     * Removes all event handler of the specified type.
     * 
     * @param type type to removed
     */
    public MCREventManager removeEventHandler(String type) {
        this.handlers.remove(type);
        return this;
    }

    /**
     * Clears the <code>MCREventManager</code> so that it contains no <code>MCREventHandler</code>.
     */
    public MCREventManager clear() {
        this.handlers.clear();
        return this;
    }

    public MCREventHandler getEventHandler(String mode, String propertyValue) {
        MCRConfiguration configuration = MCRConfiguration.instance();
        if ("Class".equals(mode)) {
            return configuration.getSingleInstanceOf(propertyValue);
        }
        String className = CONFIG_PREFIX + "Mode." + mode;
        MCREventHandlerInitializer configuredInitializer = configuration.getSingleInstanceOf(className);
        return configuredInitializer.getInstance(propertyValue);
    }

    public interface MCREventHandlerInitializer {
        MCREventHandler getInstance(String propertyValue);
    }
}
