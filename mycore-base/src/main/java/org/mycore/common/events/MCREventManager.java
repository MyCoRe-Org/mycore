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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
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

    public static final String CONFIG_PREFIX = "MCR.EventHandler.";

    /** Call event handlers in forward direction (create, update) */
    public static final boolean FORWARD = true;

    /** Call event handlers in backward direction (delete) */
    public static final boolean BACKWARD = false;

    private static Logger logger = LogManager.getLogger(MCREventManager.class);

    private static MCREventManager instance;

    /** Table of all configured event handlers * */
    private ConcurrentHashMap<String, List<MCREventHandler>> handlers;

    private MCREventManager() {
        handlers = new ConcurrentHashMap<>();

        Map<String, String> props = MCRConfiguration2.getPropertiesMap()
            .entrySet()
            .stream()
            .filter(p -> p.getKey().startsWith(CONFIG_PREFIX))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

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

            logger.debug("EventManager instantiating handler {} for type {}", props.get(propertyKey), type);

            if (propKeyIsSet(propertyKey)) {
                addEventHandler(type, getEventHandler(mode, propertyKey));
            }
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

    private boolean propKeyIsSet(String propertyKey) {
        return MCRConfiguration2.getString(propertyKey).isPresent();
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
        final String objectType = evt.getObjectType() == MCREvent.ObjectType.CUSTOM ? evt.getCustomObjectType()
            : evt.getObjectType().getClassName();
        List<MCREventHandler> list = handlers.get(objectType);
        if (list == null) {
            return;
        }

        int first = direction ? 0 : list.size() - 1;
        int last = direction ? list.size() - 1 : 0;
        int step = direction ? 1 : -1;
        int undoPos = first;

        Exception handleEventExceptionCaught = null;
        final String eventType = evt.getEventType() == MCREvent.EventType.CUSTOM ? evt.getCustomEventType()
            : evt.getEventType().name();
        for (int i = first; i != last + step; i += step) {
            MCREventHandler eh = list.get(i);
            logger.debug("EventManager {} {} calling handler {}", objectType, eventType,
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
            logger.debug("EventManager {} {} calling undo of handler {}", objectType, eventType,
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
    public MCREventManager addEventHandler(MCREvent.ObjectType type, MCREventHandler handler) {
        checkNonCustomObjectType(type);
        return addEventHandler(type.getClassName(), handler);
    }

    private static void checkNonCustomObjectType(MCREvent.ObjectType type) {
        if (type == MCREvent.ObjectType.CUSTOM) {
            throw new IllegalArgumentException("'CUSTOM' object type is unsupported here.");
        }
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
    public MCREventManager addEventHandler(MCREvent.ObjectType type, MCREventHandler handler, int index) {
        checkNonCustomObjectType(type);
        return addEventHandler(type.getClassName(), handler, index);
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
    public MCREventManager removeEventHandler(MCREvent.ObjectType type, MCREventHandler handler) {
        checkNonCustomObjectType(type);
        return removeEventHandler(type.getClassName(), handler);
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
    public MCREventManager removeEventHandler(MCREvent.ObjectType type) {
        checkNonCustomObjectType(type);
        return removeEventHandler(type.getClassName());
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
        if (Objects.equals(mode, "Class")) {
            return MCRConfiguration2.<MCREventHandler>getSingleInstanceOf(propertyValue)
                .orElseThrow(() -> MCRConfiguration2.createConfigurationException(propertyValue));
        }
        String className = CONFIG_PREFIX + "Mode." + mode;
        MCREventHandlerInitializer configuredInitializer = MCRConfiguration2
            .<MCREventHandlerInitializer>getSingleInstanceOf(className)
            .orElseThrow(() -> MCRConfiguration2.createConfigurationException(className));
        return configuredInitializer.getInstance(propertyValue);
    }

    public interface MCREventHandlerInitializer {
        MCREventHandler getInstance(String propertyValue);
    }

    /**
     * Parse the property key of event handlers, extract type and mode.
     *
     * @see MCREventHandler
     *
     * @author Huu Chi Vu
     *
     */
    private static class EventHandlerProperty {

        private String type;

        private String mode;

        EventHandlerProperty(String propertyKey) {
            String[] splitedKey = propertyKey.split("\\.");
            if (splitedKey.length != 5) {
                throw new MCRConfigurationException("Property key " + propertyKey + " for event handler not valid.");
            }

            this.setType(splitedKey[2]);
            this.setMode(splitedKey[4]);
        }

        public String getType() {
            return type;
        }

        private void setType(String type) {
            this.type = type;
        }

        public String getMode() {
            return mode;
        }

        private void setMode(String mode) {
            this.mode = mode;
        }

    }
}
