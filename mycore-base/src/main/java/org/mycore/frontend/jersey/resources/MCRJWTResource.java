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

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.jersey.MCRStaticContent;
import org.mycore.frontend.servlets.MCRServlet;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @author Thomas Scheffler (yagee)
 */
@Path("/jwt")
public class MCRJWTResource {

    private static final String ROLES_PROPERTY = "MCR.Rest.JWT.Roles";

    private static final String SECRET_PROPERTY = "MCR.Rest.JWT.SharedSecret";

    public static final String AUDIENCE = "mcr:session";

    @Context
    HttpServletRequest request;

    @GET
    @Produces(MediaType.APPLICATION_JSON + "; charset=UTF-8")
    @MCRStaticContent
    public Response getTokenFromSession() throws IOException {
        JsonFactory jf = new JsonFactory();
        if (!Optional.ofNullable(request.getSession(false))
            .map(s -> s.getAttribute(MCRServlet.ATTR_MYCORE_SESSION))
            .isPresent()) {
            try (StringWriter sw = new StringWriter()) {
                JsonGenerator jsonGenerator = jf.createGenerator(sw);
                jsonGenerator.writeStartObject();
                jsonGenerator.writeBooleanField("login_success", false);
                jsonGenerator.writeStringField("error", "login_failed");
                jsonGenerator.writeStringField("error_description", "No active MyCoRe session found.");
                jsonGenerator.writeEndObject();
                jsonGenerator.flush();
                jsonGenerator.close();
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(sw.toString())
                    .cacheControl(getCacheControl())
                    .build();
            }
        }
        MCRSession mcrSession = MCRServlet.getSession(request);
        String jwt = getToken(mcrSession);
        try (StringWriter sw = new StringWriter()) {
            JsonGenerator jsonGenerator = jf.createGenerator(sw);
            jsonGenerator.writeStartObject();
            jsonGenerator.writeBooleanField("login_success", true);
            jsonGenerator.writeStringField("access_token", jwt);
            jsonGenerator.writeStringField("token_type", "Bearer");
            jsonGenerator.writeEndObject();
            jsonGenerator.flush();
            jsonGenerator.close();
            return Response.status(Response.Status.OK)
                .header("Authorization", "Bearer " + jwt)
                .entity(sw.toString())
                .cacheControl(getCacheControl())
                .build();
        }
    }

    private CacheControl getCacheControl() {
        CacheControl cc = new CacheControl();
        cc.setPrivate(true);
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setNoTransform(true);
        return cc;
    }

    private String getToken(MCRSession mcrSession) throws UnsupportedEncodingException {
        MCRUserInformation userInformation = mcrSession.getUserInformation();
        String[] roles = MCRConfiguration2.getOrThrow(ROLES_PROPERTY, MCRConfiguration2::splitValue)
            .filter(userInformation::isUserInRole)
            .toArray(String[]::new);
        int bufferSize = MCRConfiguration2.getOrThrow("",Integer::parseInt)
        String subject = userInformation.getUserID();
        String email = userInformation.getUserAttribute(MCRUserInformation.ATT_EMAIL);
        String name = userInformation.getUserAttribute(MCRUserInformation.ATT_REAL_NAME);
        String issuer = request.getRequestURL().toString();
        return JWT.create().withIssuer(issuer)
            .withIssuedAt(new Date())
            .withSubject(subject)
            .withAudience(AUDIENCE)
            .withArrayClaim("mcr:roles", roles)
            .withJWTId(mcrSession.getID())
            .withClaim("email", email)
            .withClaim("name", name)
            .sign(getAlgorithm());
    }

    private static Algorithm getAlgorithm() {
        String secret = MCRConfiguration2.getStringOrThrow(SECRET_PROPERTY);
        try {
            return Algorithm.HMAC512(secret);
        } catch (UnsupportedEncodingException e) {
            //may not happen, as UTF-8 is a standard codec
            //wait for fix https://github.com/auth0/java-jwt/issues/236
            throw new IllegalStateException(e);
        }
    }

    public static boolean isValid(String token) {
        return Optional.of(JWT.require(getAlgorithm())
            .withAudience(AUDIENCE)
            .build().verify(token))
            .map(DecodedJWT::getId)
            .map(MCRSessionMgr::getSession)
            .isPresent();
    }

}
