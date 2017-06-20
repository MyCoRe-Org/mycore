package org.mycore.webcli.resources;

import java.io.IOException;
import java.net.SocketTimeoutException;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.ws.common.MCRWebsocketDefaultConfigurator;
import org.mycore.frontend.ws.common.MCRWebsocketJSONDecoder;
import org.mycore.frontend.ws.endoint.MCRAbstractEndpoint;
import org.mycore.webcli.container.MCRWebCLIContainer;

import com.google.gson.JsonObject;

/**
 * @author Michel Buechner (mcrmibue)
 * 
 */
@ServerEndpoint(value = "/ws/mycore-webcli/socket", configurator = MCRWebsocketDefaultConfigurator.class, decoders = {
    MCRWebsocketJSONDecoder.class })
public class MCRWebCLIResourceSockets extends MCRAbstractEndpoint {

    private static final String SESSION_KEY = "MCRWebCLI";

    private MCRWebCLIContainer cliCont = null;

    private static final Logger LOGGER = LogManager.getLogger();

    @OnOpen
    public void open(Session session) {
        sessionized(session, () -> {
            LOGGER.info("Socket Session ID: " + session.getId());
            LOGGER.info("MyCore Session ID: " + MCRSessionMgr.getCurrentSessionID());
            cliCont = getCurrentSessionContainer(true, session);
            LOGGER.info("Open ThreadID: " + Thread.currentThread().getName());
        });
    }

    @OnMessage
    public void message(Session session, JsonObject request) {
        sessionized(session, () -> {
            LOGGER.info("Message ThreadID: " + Thread.currentThread().getName());
            LOGGER.info("MyCore Session ID (message): " + MCRSessionMgr.getCurrentSessionID());
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
        String type = request.get("type").getAsString();
        if ("run".equals(type)) {
            String command = request.get("command").getAsString();
            cliCont.addCommand(command);
        }

        else if ("getKnownCommands".equals(type)) {
            request.addProperty("return", MCRWebCLIContainer.getKnownCommands().toString());
            try {
                session.getBasicRemote().sendText(request.toString());
            } catch (IOException ex) {
                LOGGER.error("Cannot send message to client.", ex);
            }
        }

        else if ("stopLog".equals(type)) {
            cliCont.stopLogging();
        }

        else if ("startLog".equals(type)) {
            cliCont.startLogging();
        }

        else if ("continueIfOneFails".equals(type)) {
            boolean value = request.get("value").getAsBoolean();
            cliCont.setContinueIfOneFails(value);
        }

        else if ("clearCommandList".equals(type)) {
            cliCont.clearCommandList();
        }
    }

    @OnClose
    public void close(Session session) {
        cliCont.stopLogging();
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
        Object sessionValue;
        synchronized (MCRSessionMgr.getCurrentSession()) {
            sessionValue = MCRSessionMgr.getCurrentSession().get(SESSION_KEY);
            if (sessionValue == null) {
                if (!create)
                    return null;
                // create object
                sessionValue = new MCRWebCLIContainer(session);
                MCRSessionMgr.getCurrentSession().put(SESSION_KEY, sessionValue);
            } else {
                ((MCRWebCLIContainer) sessionValue).changeWebSocketSession(session);
                ((MCRWebCLIContainer) sessionValue).startLogging();
            }
        }
        return (MCRWebCLIContainer) sessionValue;
    }

}
