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

package org.mycore.restapi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUserInformation;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.jersey.MCRJWTUtil;
import org.mycore.frontend.jersey.resources.MCRJWTResource;
import org.mycore.restapi.v1.MCRRestAPIAuthentication;
import org.mycore.restapi.v1.utils.MCRRestAPIUtil;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class MCRSessionFilter implements ContainerRequestFilter, ContainerResponseFilter, CompletionCallback {

    public static final Logger LOGGER = LogManager.getLogger();

    private static final String PROP_RENEW_JWT = "mcr:renewJWT";

    @Context
    HttpServletRequest httpServletRequest;

    /**
     * If request was authenticated via JSON Web Token add a new token if <code>aud</code> was {@link MCRRestAPIAuthentication#AUDIENCE}.
     *
     * If the response has a status code that represents a client error (4xx), the JSON Web Token is ommited.
     * If the response already has a JSON Web Token no changes are made.
     */
    private static void addJWTToResponse(ContainerRequestContext requestContext,
        ContainerResponseContext responseContext) {
        MCRSession currentSession = MCRSessionMgr.getCurrentSession();
        boolean renewJWT = Optional.ofNullable(requestContext.getProperty(PROP_RENEW_JWT))
            .map(Boolean.class::cast)
            .orElse(Boolean.FALSE);
        Optional.ofNullable(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION))
            .filter(s -> s.startsWith("Bearer "))
            .filter(s -> !responseContext.getStatusInfo().getFamily().equals(Response.Status.Family.CLIENT_ERROR))
            .filter(s -> responseContext.getHeaderString(HttpHeaders.AUTHORIZATION) == null)
            .map(h -> renewJWT ? ("Bearer " + MCRRestAPIAuthentication
                .getToken(currentSession.getUserInformation(), currentSession.getCurrentIP())
                .orElseThrow(() -> new InternalServerErrorException("Could not get JSON Web Token"))) : h)
            .ifPresent(h -> {
                responseContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, h);
                //Authorization header may never be cached in public caches
                Optional.ofNullable(requestContext.getHeaderString(HttpHeaders.CACHE_CONTROL))
                    .map(CacheControl::valueOf)
                    .filter(cc -> !cc.isPrivate())
                    .ifPresent(cc -> {
                        cc.setPrivate(true);
                        responseContext.getHeaders().putSingle(HttpHeaders.CACHE_CONTROL, cc);
                    });
            });
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info("Filter start.");
        boolean isSecure = requestContext.getSecurityContext().isSecure();
        if (MCRSessionMgr.hasCurrentSession()) {
            throw new InternalServerErrorException("Session is already attached.");
        }
        MCRSession currentSession = MCRSessionMgr.getCurrentSession();
        currentSession.setCurrentIP(MCRFrontendUtil.getRemoteAddr(httpServletRequest));
        currentSession.beginTransaction();
        //3 cases for authentication
        Optional<MCRUserInformation> userInformation = Optional.empty();
        String authorization = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        //1. no authentication
        if (authorization == null) {
            LOGGER.debug("No 'Authorization' header");
            return;
        }
        //2. Basic Authentification
        String basic_prefix = "Basic ";
        if (authorization.startsWith(basic_prefix)) {
            LOGGER.debug("Using 'Basic' authentication.");
            byte[] encodedAuth = authorization.substring(basic_prefix.length()).trim()
                .getBytes(StandardCharsets.ISO_8859_1);
            String userPwd = new String(Base64.getDecoder().decode(encodedAuth), StandardCharsets.ISO_8859_1);
            if (userPwd != null && userPwd.contains(":")) {
                String[] upSplit = userPwd.split(":");
                String username = upSplit[0];
                String password = upSplit[1];
                userInformation = Optional.ofNullable(MCRUserManager.checkPassword(username, password));
            }
        }
        //3. JWT
        String bearer_prefix = "Bearer ";
        if (authorization.startsWith(bearer_prefix)) {
            LOGGER.debug("Using 'JSON Web Token' authentication.");
            //get JWT
            String token = authorization.substring(bearer_prefix.length()).trim();
            //validate against secret
            try {
                DecodedJWT jwt = JWT.require(MCRJWTUtil.getJWTAlgorithm())
                    .withClaim(MCRJWTUtil.JWT_CLAIM_IP, MCRFrontendUtil.getRemoteAddr(httpServletRequest))
                    .build()
                    .verify(token);
                //validate in audience
                Optional<String> audience = jwt.getAudience().stream()
                    .filter(s -> MCRJWTResource.AUDIENCE.equals(s) || MCRRestAPIAuthentication.AUDIENCE.equals(s))
                    .findAny();
                if (audience.isPresent()) {
                    switch (audience.get()) {
                        case MCRJWTResource.AUDIENCE:
                            MCRJWTResource.validate(token);
                            break;
                        case MCRRestAPIAuthentication.AUDIENCE:
                            requestContext.setProperty(PROP_RENEW_JWT, true);
                            MCRRestAPIAuthentication.validate(token);
                            break;
                    }
                }
                userInformation = Optional.of(new MCRJWTUserInformation(jwt));
            } catch (JWTVerificationException e) {
                LOGGER.error(e.getMessage());
                LinkedHashMap<String, String> attrs=new LinkedHashMap<>();
                attrs.put("error", "invalid_token");
                attrs.put("error_description", e.getMessage());
                throw new NotAuthorizedException(e.getMessage(), e,
                    MCRRestAPIUtil.getWWWAuthenticateHeader("Bearer", attrs));
            }
        }

        if (!userInformation.isPresent()) {
            LOGGER.warn(() -> "Unsupported " + HttpHeaders.AUTHORIZATION + " header: " + authorization);
        }

        userInformation
            .ifPresent(ui -> {
                currentSession.setUserInformation(ui);
                requestContext.setSecurityContext(new MCRRestSecurityContext(ui, isSecure));
            });
        LOGGER.info("user detected: " + currentSession.getUserInformation().getUserID());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
        throws IOException {
        MCRSession currentSession = MCRSessionMgr.getCurrentSession();
        if (responseContext.getStatus() == Response.Status.FORBIDDEN.getStatusCode() && currentSession
            .getUserInformation().getUserID().equals(MCRSystemUserInformation.getGuestInstance().getUserID())) {
            LOGGER.debug("Guest detected, change response from FORBIDDEN to UNAUTHORIZED.");
            responseContext.setStatus(Response.Status.UNAUTHORIZED.getStatusCode());
            responseContext.getHeaders().putSingle(HttpHeaders.WWW_AUTHENTICATE,
                MCRRestAPIUtil.getWWWAuthenticateHeader("Basic", null));
        }
        addJWTToResponse(requestContext, responseContext);

        try {
            if (currentSession.isTransactionActive()) {
                if (currentSession.transactionRequiresRollback()) {
                    currentSession.rollbackTransaction();
                } else {
                    currentSession.commitTransaction();
                }
            }
        } finally {
            MCRSessionMgr.releaseCurrentSession();
            currentSession.close();
        }
    }

    @Override
    public void onComplete(Throwable throwable) {
        LOGGER.warn("Complete");
        if (MCRSessionMgr.hasCurrentSession()) {
            MCRSession currentSession = MCRSessionMgr.getCurrentSession();
            try {
                if (currentSession.isTransactionActive()) {
                    LOGGER.warn("Active MCRSession and JPA-Transaction found. Clearing up");
                    currentSession.rollbackTransaction();
                } else {
                    LOGGER.warn("Active MCRSession found. Clearing up");
                }
            } finally {
                MCRSessionMgr.releaseCurrentSession();
                currentSession.close();
            }

        }
    }

    private static class MCRJWTUserInformation implements MCRUserInformation {

        private final DecodedJWT jwt;

        public MCRJWTUserInformation(DecodedJWT token) {
            this.jwt = token;
        }

        @Override
        public String getUserID() {
            return jwt.getSubject();
        }

        @Override
        public boolean isUserInRole(String role) {
            return Stream.of(jwt.getClaim("mcr:roles").asArray(String.class))
                .anyMatch(role::equals);
        }

        @Override
        public String getUserAttribute(String attribute) {
            if (MCRUserInformation.ATT_REAL_NAME.equals(attribute)) {
                return jwt.getClaim("name").asString();
            }
            if (MCRUserInformation.ATT_EMAIL.equals(attribute)) {
                return jwt.getClaim("email").asString();
            }
            return null;
        }
    }

    private static class MCRRestSecurityContext implements SecurityContext {
        private final MCRUserInformation ui;

        private final boolean isSecure;

        private final Principal principal;

        public MCRRestSecurityContext(MCRUserInformation ui, boolean isSecure) {
            this.principal = ui::getUserID;
            this.ui = ui;
            this.isSecure = isSecure;
        }

        @Override
        public Principal getUserPrincipal() {
            return principal;
        }

        @Override
        public boolean isUserInRole(String role) {
            return ui.isUserInRole(role);
        }

        @Override
        public boolean isSecure() {
            return isSecure;
        }

        @Override
        public String getAuthenticationScheme() {
            if (ui.getUserID().equals(MCRSystemUserInformation.getGuestInstance().getUserID())) {
                return null;
            }
            if (ui instanceof MCRUser) {
                return SecurityContext.BASIC_AUTH;
            }
            if (ui instanceof MCRJWTUserInformation) {
                return "BEARER";
            }
            return null;
        }
    }
}
