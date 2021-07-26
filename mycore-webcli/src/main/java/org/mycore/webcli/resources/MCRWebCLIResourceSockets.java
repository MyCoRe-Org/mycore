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

package org.mycore.webcli.resources;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.ws.common.MCRWebsocketDefaultConfigurator;
import org.mycore.frontend.ws.common.MCRWebsocketJSONDecoder;
import org.mycore.frontend.ws.endoint.MCRAbstractEndpoint;
import org.mycore.webcli.container.MCRWebCLIContainer;

import com.google.gson.JsonObject;

import jakarta.websocket.CloseReason;
import jakarta.websocket.Extension;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import jakarta.websocket.server.ServerEndpoint;

/**
 * @author Michel Buechner (mcrmibue)
 * 
 */
@ServerEndpoint(value = "/ws/mycore-webcli/socket",
    configurator = MCRWebsocketDefaultConfigurator.class,
    decoders = {
        MCRWebsocketJSONDecoder.class })
public class MCRWebCLIResourceSockets extends MCRAbstractEndpoint {

    private static final String SESSION_KEY = "MCRWebCLI";

    private MCRWebCLIContainer cliCont = null;

    private static final Logger LOGGER = LogManager.getLogger();

    @OnOpen
    public void open(Session session) {
        sessionized(session, () -> {
            LOGGER.info("Socket Session ID: {}", session.getId());
            LOGGER.info("MyCore Session ID: {}", MCRSessionMgr.getCurrentSessionID());
            cliCont = getCurrentSessionContainer(true, session);
            LOGGER.info("Open ThreadID: {}", Thread.currentThread().getName());
        });
    }

    @OnMessage
    public void message(Session session, JsonObject request) {
        sessionized(session, () -> {
            LOGGER.info("Message ThreadID: {}", Thread.currentThread().getName());
            LOGGER.info("MyCore Session ID (message): {}", MCRSessionMgr.getCurrentSessionID());
            if (!MCRAccessManager.checkPermission("use-webcli")) {
                try {
                    session.getBasicRemote().sendText("noPermission");
                } catch (IOException ex) {
                    LOGGER.error("Cannot send message to client.", ex);
                }
                return;
            }
            handleMessage(session, request);
        });
    }

    private void handleMessage(Session session, JsonObject request) {
        LOGGER.info("WebSocket Request: {}", request::toString);
        String type = request.get("type").getAsString();
        if ("run".equals(type)) {
            String command = request.get("command").getAsString();
            cliCont.addCommand(command);
        } else if ("getKnownCommands".equals(type)) {
            request.add("return", MCRWebCLIContainer.getKnownCommands());
            try {
                cliCont.getWebsocketLock().lock();
                session.getBasicRemote().sendText(request.toString());
            } catch (IOException ex) {
                LOGGER.error("Cannot send message to client.", ex);
            } finally {
                cliCont.getWebsocketLock().unlock();
            }
        } else if ("stopLog".equals(type)) {
            cliCont.stopLogging();
        } else if ("startLog".equals(type)) {
            cliCont.startLogging();
        } else if ("continueIfOneFails".equals(type)) {
            boolean value = request.get("value").getAsBoolean();
            cliCont.setContinueIfOneFails(value);
        } else if ("clearCommandList".equals(type)) {
            cliCont.clearCommandList();
        }
    }

    @OnClose
    public void close(Session session) {
        LOGGER.info("Closing socket {}", session.getId());
        if (cliCont != null) {
            cliCont.webSocketClosed();
        }
    }

    @OnError
    public void error(Throwable t) {
        if (t instanceof SocketTimeoutException) {
            LOGGER.warn("Socket Session timed out, clossing connection");
        } else {
            LOGGER.error("Error in WebSocket Session", t);
        }
    }

    private MCRWebCLIContainer getCurrentSessionContainer(boolean create, Session session) {
        MCRWebCLIContainer sessionValue;
        synchronized (MCRSessionMgr.getCurrentSession()) {
            sessionValue = (MCRWebCLIContainer) MCRSessionMgr.getCurrentSession().get(SESSION_KEY);
            if (sessionValue == null && !create) {
                return null;
            }
            Session mySession = new DelegatedSession(session) {
                @Override
                public void close() throws IOException {
                    LOGGER.debug("Close session.", new RuntimeException("debug"));
                    super.close();
                }

                @Override
                public void close(CloseReason closeReason) throws IOException {
                    LOGGER.debug(() -> "Close session: " + closeReason, new RuntimeException("debug"));
                    super.close(closeReason);
                }
            };
            if (sessionValue == null) {
                // create object
                sessionValue = new MCRWebCLIContainer(mySession);
                MCRSessionMgr.getCurrentSession().put(SESSION_KEY, sessionValue);
            } else {
                sessionValue.changeWebSocketSession(mySession);
                sessionValue.startLogging();
            }
        }
        return sessionValue;
    }

    private static class DelegatedSession implements Session {
        private Session delegate;

        DelegatedSession(Session delegate) {
            this.delegate = delegate;
        }

        @Override
        public WebSocketContainer getContainer() {
            return delegate.getContainer();
        }

        @Override
        public void addMessageHandler(MessageHandler handler) throws IllegalStateException {
            delegate.addMessageHandler(handler);
        }

        @Override
        public <T> void addMessageHandler(Class<T> clazz, MessageHandler.Whole<T> handler) {
            delegate.addMessageHandler(clazz, handler);
        }

        @Override
        public <T> void addMessageHandler(Class<T> clazz, MessageHandler.Partial<T> handler) {
            delegate.addMessageHandler(clazz, handler);
        }

        @Override
        public Set<MessageHandler> getMessageHandlers() {
            return delegate.getMessageHandlers();
        }

        @Override
        public void removeMessageHandler(MessageHandler handler) {
            delegate.removeMessageHandler(handler);
        }

        @Override
        public String getProtocolVersion() {
            return delegate.getProtocolVersion();
        }

        @Override
        public String getNegotiatedSubprotocol() {
            return delegate.getNegotiatedSubprotocol();
        }

        @Override
        public List<Extension> getNegotiatedExtensions() {
            return delegate.getNegotiatedExtensions();
        }

        @Override
        public boolean isSecure() {
            return delegate.isSecure();
        }

        @Override
        public boolean isOpen() {
            return delegate.isOpen();
        }

        @Override
        public long getMaxIdleTimeout() {
            return delegate.getMaxIdleTimeout();
        }

        @Override
        public void setMaxIdleTimeout(long milliseconds) {
            delegate.setMaxIdleTimeout(milliseconds);
        }

        @Override
        public void setMaxBinaryMessageBufferSize(int length) {
            delegate.setMaxBinaryMessageBufferSize(length);
        }

        @Override
        public int getMaxBinaryMessageBufferSize() {
            return delegate.getMaxBinaryMessageBufferSize();
        }

        @Override
        public void setMaxTextMessageBufferSize(int length) {
            delegate.setMaxTextMessageBufferSize(length);
        }

        @Override
        public int getMaxTextMessageBufferSize() {
            return delegate.getMaxTextMessageBufferSize();
        }

        @Override
        public RemoteEndpoint.Async getAsyncRemote() {
            return delegate.getAsyncRemote();
        }

        @Override
        public RemoteEndpoint.Basic getBasicRemote() {
            return delegate.getBasicRemote();
        }

        @Override
        public String getId() {
            return delegate.getId();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        @Override
        public void close(CloseReason closeReason) throws IOException {
            delegate.close(closeReason);
        }

        @Override
        public URI getRequestURI() {
            return delegate.getRequestURI();
        }

        @Override
        public Map<String, List<String>> getRequestParameterMap() {
            return delegate.getRequestParameterMap();
        }

        @Override
        public String getQueryString() {
            return delegate.getQueryString();
        }

        @Override
        public Map<String, String> getPathParameters() {
            return delegate.getPathParameters();
        }

        @Override
        public Map<String, Object> getUserProperties() {
            return delegate.getUserProperties();
        }

        @Override
        public Principal getUserPrincipal() {
            return delegate.getUserPrincipal();
        }

        @Override
        public Set<Session> getOpenSessions() {
            return delegate.getOpenSessions();
        }
    }

}
