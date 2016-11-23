package org.mycore.frontend.ws.common;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import org.mycore.common.inject.MCRInjectorConfig;
import org.mycore.frontend.servlets.MCRServlet;

/**
 * Default mycore configuration for websocket endpoints.
 * 
 * @author Michel Buechner (mcrmibue)
 * @author Matthias Eichner
 */
public class MCRWebsocketDefaultConfigurator extends ServerEndpointConfig.Configurator {

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        return MCRInjectorConfig.injector().getInstance(endpointClass);
    }

    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
        HttpSession httpSession = (HttpSession) request.getHttpSession();
        config.getUserProperties().put(MCRServlet.ATTR_MYCORE_SESSION,
            httpSession.getAttribute(MCRServlet.ATTR_MYCORE_SESSION));
    }

}
