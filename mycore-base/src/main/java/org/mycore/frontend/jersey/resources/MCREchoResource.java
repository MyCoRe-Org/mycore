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

package org.mycore.frontend.jersey.resources;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.mycore.frontend.jersey.MCRStaticContent;
import org.mycore.frontend.jersey.access.MCRRequireLogin;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * @author Thomas Scheffler (yagee)
 */
@Path("/echo")
public class MCREchoResource {

    @Context
    HttpServletRequest request;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestrictedAccess(MCRRequireLogin.class)
    public String doEcho() {
        Gson gson = new Gson();
        JsonObject jRequest = new JsonObject();
        jRequest.addProperty("secure", request.isSecure());
        jRequest.addProperty("authType", request.getAuthType());
        jRequest.addProperty("context", request.getContextPath());
        jRequest.addProperty("localAddr", request.getLocalAddr());
        jRequest.addProperty("localName", request.getLocalName());
        jRequest.addProperty("method", request.getMethod());
        jRequest.addProperty("pathInfo", request.getPathInfo());
        jRequest.addProperty("protocol", request.getProtocol());
        jRequest.addProperty("queryString", request.getQueryString());
        jRequest.addProperty("remoteAddr", request.getRemoteAddr());
        jRequest.addProperty("remoteHost", request.getRemoteHost());
        jRequest.addProperty("remoteUser", request.getRemoteUser());
        jRequest.addProperty("remotePort", request.getRemotePort());
        jRequest.addProperty("requestURI", request.getRequestURI());
        jRequest.addProperty("scheme", request.getScheme());
        jRequest.addProperty("serverName", request.getServerName());
        jRequest.addProperty("servletPath", request.getServletPath());
        jRequest.addProperty("serverPort", request.getServerPort());

        HttpSession session = request.getSession(false);
        List<String> attributes = Collections.list(session.getAttributeNames());
        JsonObject sessionJSON = new JsonObject();
        JsonObject attributesJSON = new JsonObject();
        attributes.forEach(attr -> attributesJSON.addProperty(attr, session.getAttribute(attr).toString()));
        sessionJSON.add("attributes", attributesJSON);
        sessionJSON.addProperty("id", session.getId());
        sessionJSON.addProperty("creationTime", session.getCreationTime());
        sessionJSON.addProperty("lastAccessedTime", session.getLastAccessedTime());
        sessionJSON.addProperty("maxInactiveInterval", session.getMaxInactiveInterval());
        sessionJSON.addProperty("isNew", session.isNew());
        jRequest.add("session", sessionJSON);

        jRequest.addProperty("localPort", request.getLocalPort());
        JsonArray jLocales = new JsonArray();
        Enumeration<Locale> locales = request.getLocales();
        while (locales.hasMoreElements()) {
            jLocales.add(gson.toJsonTree(locales.nextElement().toString()));
        }
        jRequest.add("locales", jLocales);
        JsonObject header = new JsonObject();
        jRequest.add("header", header);
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            header.addProperty(headerName, headerValue);
        }
        JsonObject parameter = new JsonObject();
        jRequest.add("parameter", parameter);
        for (Map.Entry<String, String[]> param : request.getParameterMap().entrySet()) {
            if (param.getValue().length == 1) {
                parameter.add(param.getKey(), gson.toJsonTree(param.getValue()[0]));
            } else {
                parameter.add(param.getKey(), gson.toJsonTree(param.getValue()));
            }
        }
        return jRequest.toString();
    }

    @GET
    @Path("ping")
    @MCRStaticContent
    public Response ping() {
        return Response.ok("pong").build();
    }
}
