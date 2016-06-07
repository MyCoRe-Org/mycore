package org.mycore.webcli.resources;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import org.mycore.frontend.servlets.MCRServlet;

/**
 * @author Michel Buechner (mcrmibue)
 * 
 */
public class GetMCRSessionIdConfigurator extends ServerEndpointConfig.Configurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
        HttpSession httpSession = (HttpSession) request.getHttpSession();
        config.getUserProperties().put(MCRServlet.ATTR_MYCORE_SESSION, httpSession.getAttribute(MCRServlet.ATTR_MYCORE_SESSION));
    }

}
