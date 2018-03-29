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

package org.mycore.webtools.processing.socket.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.Session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.processing.MCRProcessable;
import org.mycore.common.processing.MCRProcessableCollection;
import org.mycore.common.processing.MCRProcessableRegistry;
import org.mycore.webtools.processing.socket.MCRProcessableWebsocketSender;

import com.google.gson.Gson;

/**
 * Websocket implementation of sending processable objects.
 *
 * @author Matthias Eichner
 */
public class MCRProcessableWebsocketSenderImpl implements MCRProcessableWebsocketSender {

    private static final AtomicInteger ID_GENERATOR;

    private static final Map<Object, Integer> ID_MAP;

    private static final Map<Integer, Integer> PROCESSABLE_COLLECTION_MAP;

    static {
        ID_GENERATOR = new AtomicInteger();
        ID_MAP = Collections.synchronizedMap(new HashMap<>());
        PROCESSABLE_COLLECTION_MAP = Collections.synchronizedMap(new HashMap<>());
    }

    @Override
    public void sendError(Session session, Integer errorCode) {
        MCRWebSocketMessage errorMessage = new MCRErrorMessage(errorCode);
        send(session, errorMessage);
    }

    @Override
    public void sendRegistry(Session session, MCRProcessableRegistry registry) {
        send(session, new MCRRegistryMessage());
        registry.stream().forEach(collection -> addCollection(session, registry, collection));
    }

    @Override
    public void addCollection(Session session, MCRProcessableRegistry registry, MCRProcessableCollection collection) {
        MCRAddCollectionMessage message = new MCRAddCollectionMessage(getId(collection), collection.getName(),
            collection.getProperties());
        send(session, message);
        collection.stream().forEach(processable -> addProcessable(session, collection, processable));
    }

    @Override
    public void removeCollection(Session session, MCRProcessableCollection collection) {
        Integer id = remove(collection);
        if (id == null) {
            return;
        }
        collection.stream().forEach(processable -> removeProcessable(session, processable));
        send(session, new MCRRemoveCollectionMessage(id));
    }

    @Override
    public void addProcessable(Session session, MCRProcessableCollection collection, MCRProcessable processable) {
        Integer processableId = getId(processable);
        Integer collectionId = getId(collection);
        PROCESSABLE_COLLECTION_MAP.put(processableId, collectionId);
        updateProcessable(session, processable, processableId, collectionId);
    }

    @Override
    public void updateProcessable(Session session, MCRProcessable processable) {
        Integer processableId = getId(processable);
        Integer collectionId = PROCESSABLE_COLLECTION_MAP.get(processableId);
        updateProcessable(session, processable, processableId, collectionId);
    }

    protected void updateProcessable(Session session, MCRProcessable processable, Integer processableId,
        Integer collectionId) {
        MCRProcessableMessage message = new MCRProcessableMessage(processable, processableId, collectionId);
        send(session, message);
    }

    @Override
    public void removeProcessable(Session session, MCRProcessable processable) {
        remove(processable);
    }

    @Override
    public void updateProperty(Session session, MCRProcessableCollection collection, String name, Object value) {
        send(session, new MCRUpdateCollectionPropertyMessage(getId(collection), name, value));
    }

    public synchronized Integer getId(Object object) {
        return ID_MAP.computeIfAbsent(object, k -> ID_GENERATOR.incrementAndGet());
    }

    public synchronized Integer remove(Object object) {
        Integer id = ID_MAP.get(object);
        if (id == null) {
            return null;
        }
        ID_MAP.remove(id);
        if (object instanceof MCRProcessable) {
            PROCESSABLE_COLLECTION_MAP.remove(id);
        } else if (object instanceof MCRProcessableCollection) {
            PROCESSABLE_COLLECTION_MAP.values().removeIf(id::equals);
        }
        return id;
    }

    private void send(Session session, MCRWebSocketMessage responseMessage) {
        String msg = new Gson().toJson(responseMessage);
        AsyncSender.send(session, msg);
    }


    /**
     * Tomcat does not support async sending of messages. We have to implement
     * our own sender.
     *
     * <a href="https://bz.apache.org/bugzilla/show_bug.cgi?id=56026">tomcat bug</a>
     */
    private static class AsyncSender {

        private static Logger LOGGER = LogManager.getLogger();

        private static ExecutorService SERVICE;

        static {
            SERVICE = Executors.newSingleThreadExecutor();
            MCRShutdownHandler.getInstance().addCloseable(new MCRShutdownHandler.Closeable() {

                @Override
                public void prepareClose() {
                    SERVICE.shutdown();
                }

                @Override
                public void close() {
                    if (!SERVICE.isTerminated()) {
                        try {
                            SERVICE.awaitTermination(10, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            LOGGER.warn("Error while waiting for shutdown.", e);
                        }
                    }
                }
            });
        }

        /**
         * Sends a text to the session
         *
         * @param session session to send to
         * @param msg the message
         */
        public static void send(Session session, String msg) {
            SERVICE.submit(() -> {
                if (session == null || !session.isOpen()) {
                    return;
                }
                try {
                    session.getBasicRemote().sendText(msg);
                } catch (Exception exc) {
                    LOGGER.error("Websocket error {}: Unable to send message {}", session.getId(), msg);
                }
            });

        }

    }

}
