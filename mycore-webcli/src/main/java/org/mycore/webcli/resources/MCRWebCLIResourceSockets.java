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
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.webcli.container.MCRWebCLIContainer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author Michel Buechner (mcrmibue)
 * 
 */
@ServerEndpoint(value = "/WebCLISocket", configurator = GetMCRSessionIdConfigurator.class)
public class MCRWebCLIResourceSockets {
    
    private static final String SESSION_KEY = "MCRWebCLI";
    
    private MCRWebCLIContainer cliCont = null;
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    @OnOpen
    public void open(Session session) {
        LOGGER.info("Socket Session ID: " + session.getId());
        setSession(session);
        LOGGER.info("MyCore Session ID: " + MCRSessionMgr.getCurrentSessionID());
        cliCont = getCurrentSessionContainer(true, session);
        LOGGER.info("Open ThreadID: " + Thread.currentThread().getName());
        MCRSessionMgr.releaseCurrentSession();
    }
    
    @OnMessage
    public void  message(Session session, String data) {
        setSession(session);
        LOGGER.info("Message ThreadID: " + Thread.currentThread().getName());
        LOGGER.info("MyCore Session ID (message): " + MCRSessionMgr.getCurrentSessionID());
        if (MCRAccessManager.checkPermission("use-webcli")) {
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = jsonParser.parse(data).getAsJsonObject();
            String type = jsonObject.get("type").getAsString();        
            if (type.equals("run")){
                String command = jsonObject.get("command").getAsString();
                cliCont.addCommand(command);
            }
            
            if (type.equals("getKnownCommands")){
                jsonObject.addProperty("return", MCRWebCLIContainer.getKnownCommands().toString());
                try {
                    session.getBasicRemote().sendText(jsonObject.toString());
                } catch (IOException ex) {
                    LOGGER.error("Cannot send message to client.", ex);
                }
            }
            if(type.equals("stopLog")) {
                cliCont.stopLogging();
            }
            if(type.equals("startLog")) {
                cliCont.startLogging();
            }
        }
        else {
            try {
                session.getBasicRemote().sendText("noPermission");
            } catch (IOException ex) {
                LOGGER.error("Cannot send message to client.", ex);
            }
        }
        MCRSessionMgr.releaseCurrentSession();
    }
    
    @OnClose
    public void close(Session session) {
        cliCont.stopLogging();
    }
    
    @OnError
    public void error(Throwable t) {
        if (t instanceof SocketTimeoutException) {
            LOGGER.warn("Socket Session timed out, clossing connection");
        }
        else {
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
            }
            else {
                ((MCRWebCLIContainer) sessionValue).changeWebSocketSession(session);
                ((MCRWebCLIContainer) sessionValue).startLogging();
            }
        }
        return (MCRWebCLIContainer) sessionValue;
    }
    
    private void setSession(Session session) {
        String sessionId = (String) session.getUserProperties().get(MCRServlet.ATTR_MYCORE_SESSION);
        if (MCRSessionMgr.getCurrentSessionID() == null || !MCRSessionMgr.getCurrentSessionID().equals(sessionId)){
            MCRSessionMgr.releaseCurrentSession();
            MCRSessionMgr.setCurrentSession(MCRSessionMgr.getSession(sessionId));
        }
    }
}
