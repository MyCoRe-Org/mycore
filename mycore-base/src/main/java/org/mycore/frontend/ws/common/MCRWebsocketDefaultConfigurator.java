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

package org.mycore.frontend.ws.common;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.servlets.MCRServlet;

import jakarta.servlet.http.HttpSession;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * Default mycore configuration for websocket endpoints.
 * 
 * @author Michel Buechner (mcrmibue)
 * @author Matthias Eichner
 */
public class MCRWebsocketDefaultConfigurator extends ServerEndpointConfig.Configurator {

    public static final String HTTP_SESSION = "http.session";

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        return MCRConfiguration2.instantiateClass(endpointClass.getName());
    }

    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
        HttpSession httpSession = (HttpSession) request.getHttpSession();
        config.getUserProperties().put(MCRServlet.ATTR_MYCORE_SESSION,
            httpSession.getAttribute(MCRServlet.ATTR_MYCORE_SESSION));
        config.getUserProperties().put(HTTP_SESSION, httpSession);
    }

}
