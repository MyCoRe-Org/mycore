package org.mycore.webtools.processing.socket;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.processing.MCRListenableProgressable;
import org.mycore.common.processing.MCRProcessable;
import org.mycore.common.processing.MCRProcessableCollection;
import org.mycore.common.processing.MCRProcessableCollectionListener;
import org.mycore.common.processing.MCRProcessableRegistry;
import org.mycore.common.processing.MCRProcessableRegistryListener;
import org.mycore.common.processing.MCRProcessableStatus;
import org.mycore.common.processing.MCRProcessableStatusListener;
import org.mycore.common.processing.MCRProgressable;
import org.mycore.common.processing.MCRProgressableListener;
import org.mycore.frontend.ws.common.MCRWebsocketDefaultConfigurator;
import org.mycore.frontend.ws.common.MCRWebsocketJSONDecoder;
import org.mycore.frontend.ws.endoint.MCRAbstractEndpoint;

import com.google.gson.JsonObject;
import com.google.inject.Inject;

@ServerEndpoint(value = "/ws/mycore-webtools/processing",
    configurator = MCRWebsocketDefaultConfigurator.class,
    decoders = {
        MCRWebsocketJSONDecoder.class })
public class MCRProcessingEndpoint extends MCRAbstractEndpoint {

    private static final Logger LOGGER = LogManager.getLogger();

    private static Map<String, SessionListener> SESSIONS;

    static {
        SESSIONS = Collections.synchronizedMap(new HashMap<>());
    }

    private MCRProcessableRegistry registry;

    private MCRProcessableWebsocketSender sender;

    @Inject
    public MCRProcessingEndpoint(MCRProcessableRegistry registry, MCRProcessableWebsocketSender sender) {
        this.registry = registry;
        this.sender = sender;
    }

    @OnMessage
    public void onMessage(Session session, JsonObject request) {
        sessionized(session, () -> {
            if (!MCRAccessManager.checkPermission("use-processable")) {
                this.sender.sendError(session, 403);
                return;
            }
            handleMessage(session, request);
        });
    }

    @OnError
    public void onError(Session session, Throwable error) {
        if (error instanceof SocketTimeoutException) {
            this.close(session);
            LOGGER.warn("Websocket error " + session.getId() + ": websocket timeout");
            return;
        }
        LOGGER.error("Websocket error " + session.getId(), error);
    }

    @OnClose
    public void close(Session session) {
        SessionListener sessionListener = SESSIONS.get(session.getId());
        if (session != null) {
            sessionListener.detachListeners(this.registry);
            SESSIONS.remove(session.getId());
        }
    }

    private void handleMessage(Session session, JsonObject request) {
        String type = request.get("type").getAsString();

        if ("connect".equals(type)) {
            connect(session);
            return;
        }

    }

    private void connect(Session session) {
        this.sender.sendRegistry(session, this.registry);
        if (SESSIONS.containsKey(session.getId())) {
            return;
        }
        final SessionListener sessionListener = new SessionListener(session, this.sender);
        registry.addListener(sessionListener);
        this.registry.stream().forEach(collection -> {
            sessionListener.attachCollection(collection);
        });
        SESSIONS.put(session.getId(), sessionListener);
    }

    private static class SessionListener implements MCRProcessableRegistryListener, MCRProcessableCollectionListener,
        MCRProcessableStatusListener, MCRProgressableListener {

        private Session session;

        private MCRProcessableWebsocketSender sender;

        public SessionListener(Session session, MCRProcessableWebsocketSender sender) {
            this.session = session;
            this.sender = sender;
        }

        @Override
        public void onProgressChange(MCRProgressable source, Integer oldProgress, Integer newProgress) {
            if (isClosed()) {
                return;
            }
            if (source instanceof MCRProcessable) {
                this.sender.updateProcessable(session, (MCRProcessable) source);
            }
        }

        @Override
        public void onProgressTextChange(MCRProgressable source, String oldProgressText, String newProgressText) {
            if (isClosed()) {
                return;
            }

            if (source instanceof MCRProcessable) {
                this.sender.updateProcessable(session, (MCRProcessable) source);
            }
        }

        @Override
        public void onStatusChange(MCRProcessable source, MCRProcessableStatus oldStatus,
            MCRProcessableStatus newStatus) {
            if (isClosed()) {
                return;
            }
            this.sender.updateProcessable(session, source);
        }

        @Override
        public void onAdd(MCRProcessableRegistry source, MCRProcessableCollection collection) {
            if (isClosed()) {
                return;
            }
            this.attachCollection(collection);
            this.sender.addCollection(session, source, collection);
        }

        @Override
        public void onRemove(MCRProcessableRegistry source, MCRProcessableCollection collection) {
            if (isClosed()) {
                return;
            }
            this.sender.removeCollection(session, collection);
        }

        @Override
        public void onAdd(MCRProcessableCollection source, MCRProcessable processable) {
            if (isClosed()) {
                return;
            }
            attachProcessable(processable);
            this.sender.addProcessable(session, source, processable);
        }

        @Override
        public void onRemove(MCRProcessableCollection source, MCRProcessable processable) {
            processable.removeStatusListener(this);
            if (isClosed()) {
                return;
            }
            this.sender.removeProcessable(session, processable);
        }

        @Override
        public void onPropertyChange(MCRProcessableCollection source, String name, Object oldValue, Object newValue) {
            if (isClosed()) {
                return;
            }
            this.sender.updateProperty(session, source, name, newValue);
        }

        protected boolean isClosed() {
            if (!this.session.isOpen()) {
                try {
                    this.session.close(new CloseReason(CloseCodes.GOING_AWAY, "client disconnected"));
                } catch (IOException ioExc) {
                    LOGGER.error("Websocket error " + session.getId() + ": Unable to close websocket connection",
                        ioExc);
                }
                return true;
            }
            return false;
        }

        /**
         * Attaches the given collection to this {@link SessionListener} object by
         * adding all relevant listeners.
         * 
         * @param collection the collection to attach to this object
         */
        public void attachCollection(MCRProcessableCollection collection) {
            collection.addListener(this);
            collection.stream().forEach(processable -> {
                attachProcessable(processable);
            });
        }

        /**
         * Attaches the given processable to this {@link SessionListener} object by
         * adding all relevant listeners.
         * 
         * @param processable the processable to attach to this object
         */
        private void attachProcessable(MCRProcessable processable) {
            processable.addStatusListener(this);
            if (processable instanceof MCRListenableProgressable) {
                ((MCRListenableProgressable) processable).addProgressListener(this);
            }
        }

        /**
         * Removes this session data object from all listeners of the given registry.
         * 
         * @param registry the registry to detach from
         */
        public void detachListeners(MCRProcessableRegistry registry) {
            registry.removeListener(this);
            registry.stream().forEach(collection -> {
                collection.removeListener(this);
                collection.stream().forEach(processable -> {
                    processable.removeProgressListener(this);
                    processable.removeStatusListener(this);
                });
            });
        }

    }

}
