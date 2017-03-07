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
import org.mycore.webtools.processing.MCRProcessableJSONUtil;
import org.mycore.webtools.processing.socket.MCRProcessableWebsocketSender;

import com.google.gson.JsonObject;

/**
 * Websocket implementation of sending processable objects.
 * 
 * @author Matthias Eichner
 */
public class MCRProcessableWebsocketSenderImpl implements MCRProcessableWebsocketSender {

    private final static AtomicInteger ID_GENERATOR;

    private final static Map<Object, Integer> ID_MAP;

    static {
        ID_GENERATOR = new AtomicInteger();
        ID_MAP = Collections.synchronizedMap(new HashMap<>());
    }

    @Override
    public void sendError(Session session, Integer errorCode) {
        JsonObject errorMessage = new JsonObject();
        errorMessage.addProperty("error", errorCode);
        send(session, errorMessage, Type.error);
    }

    @Override
    public void sendRegistry(Session session, MCRProcessableRegistry registry) {
        JsonObject registryMessage = new JsonObject();
        send(session, registryMessage, Type.registry);
        registry.stream().forEach(collection -> {
            addCollection(session, registry, collection);
        });
    }

    @Override
    public void addCollection(Session session, MCRProcessableRegistry registry, MCRProcessableCollection collection) {
        JsonObject addCollectionMessage = new JsonObject();
        addCollectionMessage.addProperty("id", getId(collection));
        addCollectionMessage.addProperty("name", collection.getName());
        addCollectionMessage.add("properties", MCRProcessableJSONUtil.toJSON(collection.getProperties()));
        send(session, addCollectionMessage, Type.addCollection);

        collection.stream().forEach(processable -> {
            addProcessable(session, collection, processable);
        });
    }

    @Override
    public void removeCollection(Session session, MCRProcessableCollection collection) {
        Integer id = remove(collection);
        if (id == null) {
            return;
        }
        collection.stream().forEach(processable -> {
            removeProcessable(session, processable);
        });
        JsonObject removeCollectionMessage = new JsonObject();
        removeCollectionMessage.addProperty("id", id);
        send(session, removeCollectionMessage, Type.removeCollection);
    }

    @Override
    public void addProcessable(Session session, MCRProcessableCollection collection, MCRProcessable processable) {
        JsonObject addProcessableMessage = MCRProcessableJSONUtil.toJSON(processable);
        addProcessableMessage.addProperty("id", getId(processable));
        addProcessableMessage.addProperty("collectionId", getId(collection));
        send(session, addProcessableMessage, Type.addProcessable);
    }

    @Override
    public void removeProcessable(Session session, MCRProcessable processable) {
        Integer id = remove(processable);
        if (id == null) {
            return;
        }
        JsonObject removeProcessableMessage = new JsonObject();
        removeProcessableMessage.addProperty("id", id);
        send(session, removeProcessableMessage, Type.removeProcessable);
    }

    @Override
    public void updateProcessable(Session session, MCRProcessable processable) {
        JsonObject addProcessableMessage = MCRProcessableJSONUtil.toJSON(processable);
        addProcessableMessage.addProperty("id", getId(processable));
        send(session, addProcessableMessage, Type.updateProcessable);
    }

    @Override
    public void updateProperty(Session session, MCRProcessableCollection collection, String name, Object value) {
        JsonObject updatePropertyMessage = new JsonObject();
        updatePropertyMessage.addProperty("id", getId(collection));
        updatePropertyMessage.addProperty("propertyName", name);
        updatePropertyMessage.add("propertyValue", MCRProcessableJSONUtil.toJSON(value));
        send(session, updatePropertyMessage, Type.updateCollectionProperty);
    }

    public synchronized Integer getId(Object object) {
        Integer id = ID_MAP.get(object);
        if (id == null) {
            id = ID_GENERATOR.incrementAndGet();
            ID_MAP.put(object, id);
        }
        return id;
    }

    public synchronized Integer remove(Object object) {
        Integer id = ID_MAP.get(object);
        if (id == null) {
            return null;
        }
        ID_MAP.remove(id);
        return id;
    }

    private void send(Session session, JsonObject responseMessage, Type type) {
        responseMessage.addProperty("type", type.name());
        String msg = responseMessage.toString();
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
            SERVICE.submit(new Runnable() {

                @Override
                public void run() {
                    if (session != null && !session.isOpen()) {
                        return;
                    }
                    try {
                        session.getBasicRemote().sendText(msg);
                    } catch (Exception exc) {
                        LOGGER.error("Websocket error " + session.getId() + ": Unable to send message " + msg);
                    }
                }

            });

        }

    }

}
