/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;

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
public final class MCREventManager {

    public static final String CONFIG_PREFIX = "MCR.EventHandler.";

    /** Call event handlers in forward direction (create, update) */
    public static final boolean FORWARD = true;

    /** Call event handlers in backward direction (delete) */
    public static final boolean BACKWARD = false;

    private static final Logger LOGGER = LogManager.getLogger();

    /** Table of all configured event handlers * */
    private final Map<String, List<MCREventHandler>> handlers;

    private MCREventManager() {
        handlers = new ConcurrentHashMap<>();

        // collect all properties like 'MCR.EventHandler.{type}.{number}.Class,
        // but without the leading 'MCR.EventHandler.'
        Map<String, String> propertyKeySuffixes = MCRConfiguration2.getSubPropertiesMap(CONFIG_PREFIX)
                .entrySet()
                .stream()
                .filter(property -> !property.getValue().isBlank())
                .filter(property -> {
                    String key = property.getKey();
                    int indexAfterTypeAndNumber = key.indexOf('.', key.indexOf('.') + 1);
                    String configPropertySuffix = key.substring(indexAfterTypeAndNumber);
                    return ".Class".equals(configPropertySuffix);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<String> propertyKeySuffixList = new ArrayList<>(propertyKeySuffixes.size());
        propertyKeySuffixList.addAll(propertyKeySuffixes.keySet());
        Collections.sort(propertyKeySuffixList);

        for (String propertyKeySuffix : propertyKeySuffixList) {

            String fullPropertyKey = CONFIG_PREFIX + propertyKeySuffix;
            String type = propertyKeySuffix.substring(0, propertyKeySuffix.indexOf('.'));

            LOGGER.debug("EventManager instantiating handler {} for type {}",
                () -> propertyKeySuffixes.get(propertyKeySuffix), () -> type);

            addEventHandler(type, getEventHandler(fullPropertyKey));

        }
    }

    /**
     * The singleton manager instance
     *
     * @return the single event manager
     */
    public static MCREventManager getInstance() {
        return LazyInstanceHolder.SINGLETON_INSTANCE;
    }

    /**
     * Returns a new instance of the event manager for testing purposes.
     */
    static MCREventManager obtainTestInstance() {
        return new MCREventManager();
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
        LOGGER.info("Handling event {}", evt);

        final String objectType = evt.getObjectType() == MCREvent.ObjectType.CUSTOM ? evt.getCustomObjectType()
            : evt.getObjectType().getClassName();
        List<MCREventHandler> list = handlers.get(objectType);
        if (list == null) {
            return;
        }
        int first = direction ? 0 : list.size() - 1;
        int undoPos = first;
        Exception handleEventExceptionCaught = processEventHandlersAndUndo(evt, direction, undoPos);
        if (handleEventExceptionCaught != null) {
            String msg = "Exception caught in EventHandler, rollback by calling undo of successfull handlers done.";
            throw new MCRException(msg, handleEventExceptionCaught);
        }
    }

    private Exception processEventHandlersAndUndo(MCREvent evt, boolean direction, int undoPos) {
        final String objectType = evt.getObjectType() == MCREvent.ObjectType.CUSTOM ? evt.getCustomObjectType()
            : evt.getObjectType().getClassName();
        List<MCREventHandler> list = handlers.get(objectType);
        int first = direction ? 0 : list.size() - 1;
        int last = direction ? list.size() - 1 : 0;
        int step = direction ? 1 : -1;
        int pos = undoPos;
        final String eventType = evt.getEventType() == MCREvent.EventType.CUSTOM ? evt.getCustomEventType()
            : evt.getEventType().name();
        Exception handleEventExceptionCaught = null;
        for (int i = first; i != last + step; i += step) {
            MCREventHandler eh = list.get(i);
            LOGGER.debug("EventManager {} {} calling handler {}", () -> objectType, () -> eventType,
                () -> eh.getClass().getName());
            handleEventExceptionCaught = handleEventAndUndoOnException(eh, evt);
            if (handleEventExceptionCaught != null) {
                pos = i;
                break;
            }
        }
        for (int i = pos - step; i != first - step; i -= step) {
            rollbackByUndoingHandlers(list.get(i), evt);
        }
        return handleEventExceptionCaught;
    }

    private Exception handleEventAndUndoOnException(MCREventHandler eh, MCREvent evt) {
        Exception handleEventExceptionCaught = null;
        try {
            eh.doHandleEvent(evt);
        } catch (Exception ex) {
            handleEventExceptionCaught = ex;
            LOGGER.error("Exception caught while calling event handler", ex);
            LOGGER.error("Trying rollback by calling undo method of event handlers");
        }
        return handleEventExceptionCaught;

    }

    private void rollbackByUndoingHandlers(MCREventHandler eh, MCREvent evt) {
        final String objectType = evt.getObjectType() == MCREvent.ObjectType.CUSTOM ? evt.getCustomObjectType()
            : evt.getObjectType().getClassName();
        final String eventType = evt.getEventType() == MCREvent.EventType.CUSTOM ? evt.getCustomEventType()
            : evt.getEventType().name();
        LOGGER.debug("EventManager {} {} calling undo of handler {}", () -> objectType, () -> eventType,
            () -> eh.getClass().getName());
        try {
            eh.undoHandleEvent(evt);
        } catch (Exception ex) {
            LOGGER.error("Exception caught while calling undo of event handler", ex);
        }

    }

    /** Same as handleEvent( evt, MCREventManager.FORWARD ) */
    public void handleEvent(MCREvent evt) throws MCRException {
        handleEvent(evt, FORWARD);
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

    /**
     * @deprecated Support for modes other than "Class" has been dropped.
     * Use {@link #getEventHandler(String)} instead.
     */
    @Deprecated(forRemoval = true)
    public MCREventHandler getEventHandler(String mode, String propertyKey) {
        return getEventHandler(propertyKey);
    }

    public MCREventHandler getEventHandler(String propertyKey) {
        return MCRConfiguration2.getSingleInstanceOfOrThrow(MCREventHandler.class, propertyKey);
    }

    private static final class LazyInstanceHolder {
        public static final MCREventManager SINGLETON_INSTANCE = new MCREventManager();
    }

}
