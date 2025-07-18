/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.output.ProxyOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRTransactionManager;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.jersey.MCRJWTUtil;
import org.mycore.frontend.jersey.resources.MCRJWTResource;
import org.mycore.restapi.v1.MCRRestAPIAuthentication;
import org.mycore.restapi.v1.utils.MCRRestAPIUtil;
import org.mycore.user2.MCRRealm;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.RuntimeDelegate;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class MCRSessionFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String BASIC_AUTH_PREFIX = "Basic ";

    private static final String PROP_RENEW_JWT = "mcr:renewJWT";

    private static final String BEARER_AUTH_PREFIX = "Bearer ";

    private static final List<String> ALLOWED_JWT_SESSION_ATTRIBUTES = MCRConfiguration2
        .getString("MCR.RestAPI.JWT.AllowedSessionAttributePrefixes").stream()
        .flatMap(MCRConfiguration2::splitValue)
        .toList();

    @Context
    HttpServletRequest httpServletRequest;

    @Context
    Application app;

    /**
     * If request was authenticated via JSON Web Token add a new token if <code>aud</code> was
     * {@link MCRRestAPIAuthentication#AUDIENCE}.
     * <p>
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
            .filter(s -> s.startsWith(BEARER_AUTH_PREFIX))
            .filter(s -> !responseContext.getStatusInfo().getFamily().equals(Response.Status.Family.CLIENT_ERROR))
            .filter(s -> responseContext.getHeaderString(HttpHeaders.AUTHORIZATION) == null)
            .map(h -> renewJWT ? (BEARER_AUTH_PREFIX + MCRRestAPIAuthentication
                .getToken(currentSession, currentSession.getCurrentIP())
                .orElseThrow(() -> new InternalServerErrorException("Could not get JSON Web Token"))) : h)
            .ifPresent(h -> {
                responseContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, h);
                //Authorization header may never be cached in public caches
                Optional.ofNullable(requestContext.getHeaderString(HttpHeaders.CACHE_CONTROL))
                    .map(RuntimeDelegate.getInstance()
                        .createHeaderDelegate(CacheControl.class)::fromString)
                    .filter(cc -> !cc.isPrivate())
                    .ifPresent(cc -> {
                        cc.setPrivate(true);
                        responseContext.getHeaders().putSingle(HttpHeaders.CACHE_CONTROL, cc);
                    });
            });
    }

    private static void checkIPClaim(Claim ipClaim, String remoteAddr) {
        try {
            if (ipClaim.isNull() || !MCRFrontendUtil.isIPAddrAllowed(ipClaim.asString(), remoteAddr)) {
                throw new JWTVerificationException(
                    "The Claim '" + MCRJWTUtil.JWT_CLAIM_IP + "' value doesn't match the required one.");
            }
        } catch (UnknownHostException e) {
            throw new JWTVerificationException(
                "The Claim '" + MCRJWTUtil.JWT_CLAIM_IP + "' value doesn't match the required one.", e);
        }
    }

    private static boolean isUnAuthorized(ContainerRequestContext requestContext) {
        return requestContext.getHeaderString(HttpHeaders.AUTHORIZATION) == null;
    }

    //returns true for Ajax-Requests or requests for embedded images
    private static boolean doNotWWWAuthenticate(ContainerRequestContext requestContext) {
        return !"ServiceWorker".equals(requestContext.getHeaderString("X-Requested-With")) &&
            ("XMLHttpRequest".equals(requestContext.getHeaderString("X-Requested-With")) ||
                requestContext.getAcceptableMediaTypes()
                    .stream()
                    .findFirst()
                    .filter(m -> "image".equals(m.getType()))
                    .isPresent());
    }

    private static void closeSessionIfNeeded() {
        if (MCRSessionMgr.hasCurrentSession()) {
            MCRSession currentSession = MCRSessionMgr.getCurrentSession();
            try {
                if (MCRTransactionManager.hasActiveTransactions()) {
                    LOGGER.debug("Active MCRSession and JPA-Transaction found. Clearing up");
                    if (MCRTransactionManager.hasRollbackOnlyTransactions()) {
                        MCRTransactionManager.rollbackTransactions();
                    } else {
                        MCRTransactionManager.commitTransactions();
                    }
                } else {
                    LOGGER.debug("Active MCRSession found. Clearing up");
                }
            } finally {
                MCRSessionMgr.releaseCurrentSession();
                currentSession.close();
                LOGGER.debug("Session closed.");
            }
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        LOGGER.debug("Filter start.");
        boolean isSecure = requestContext.getSecurityContext().isSecure();
        if (MCRSessionMgr.hasCurrentSession()) {
            throw new InternalServerErrorException("Session is already attached.");
        }
        MCRSessionMgr.unlock();
        MCRSession currentSession = MCRSessionMgr.getCurrentSession(); //bind to this request
        currentSession.setCurrentIP(MCRFrontendUtil.getRemoteAddr(httpServletRequest));
        MCRTransactionManager.beginTransactions();
        //3 cases for authentication
        String authorization = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        //1. no authentication
        if (authorization == null) {
            LOGGER.debug("No 'Authorization' header");
            return;
        }
        //2. Basic Authentification

        MCRUserInformation userInformation;

        if (authorization.startsWith(BASIC_AUTH_PREFIX)) {
            userInformation = extractUserFromBasicAuth(authorization);
        } else if (authorization.startsWith(BEARER_AUTH_PREFIX)) {
            userInformation = extractUserFromBearerAuth(requestContext, authorization, currentSession);
        } else {
            LOGGER.warn(() -> "Unsupported " + HttpHeaders.AUTHORIZATION + " header: " + authorization);
            return;
        }

        currentSession.setUserInformation(userInformation);
        requestContext.setSecurityContext(new MCRRestSecurityContext(userInformation, isSecure));
        LOGGER.info(() -> "user detected: " + currentSession.getUserInformation().getUserID());
    }

    private MCRUserInformation extractUserFromBasicAuth(String authorization) {
        LOGGER.debug("Using 'Basic' authentication.");
        byte[] encodedAuth = authorization.substring(BASIC_AUTH_PREFIX.length()).trim()
            .getBytes(StandardCharsets.ISO_8859_1);
        String userPwd = new String(Base64.getDecoder().decode(encodedAuth), StandardCharsets.ISO_8859_1);
        if (userPwd.contains(":") && userPwd.length() > 1) {
            String[] upSplit = userPwd.split(":");
            String username = upSplit[0];
            String password = upSplit[1];
            return Optional.ofNullable(MCRUserManager.checkPassword(username, password))
                .map(MCRUserInformation.class::cast)
                .orElseThrow(() -> {
                    Map<String, String> attrs = new LinkedHashMap<>();
                    attrs.put("error", "invalid_login");
                    attrs.put("error_description", "Wrong login or password.");
                    return new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED)
                        .header(HttpHeaders.WWW_AUTHENTICATE,
                            MCRRestAPIUtil.getWWWAuthenticateHeader(null, attrs, app))
                        .build());
                });
        }
        return null;
    }

    private MCRUserInformation extractUserFromBearerAuth(ContainerRequestContext requestContext,
        String authorization, MCRSession currentSession) {
        MCRUserInformation userInformation;
        LOGGER.debug("Using 'JSON Web Token' authentication.");
        String token = authorization.substring(BEARER_AUTH_PREFIX.length()).trim();
        try {
            DecodedJWT jwt = JWT.require(MCRJWTUtil.getJWTAlgorithm())
                .build()
                .verify(token);
            //validate ip
            checkIPClaim(jwt.getClaim(MCRJWTUtil.JWT_CLAIM_IP), MCRFrontendUtil.getRemoteAddr(httpServletRequest));
            //validate in audience
            Optional<String> audience = jwt.getAudience().stream()
                .filter(s -> MCRJWTResource.AUDIENCE.equals(s) || MCRRestAPIAuthentication.AUDIENCE.equals(s))
                .findAny();
            if (audience.isPresent()) {
                switch (audience.get()) {
                    case MCRJWTResource.AUDIENCE -> MCRJWTResource.validate(token);
                    case MCRRestAPIAuthentication.AUDIENCE -> {
                        requestContext.setProperty(PROP_RENEW_JWT, true);
                        MCRRestAPIAuthentication.validate(token);
                    }
                    default -> LOGGER.warn("Cannot validate JWT for '{}' audience.", audience.get());
                }
            }
            userInformation = new MCRJWTUserInformation(jwt);
            if (!ALLOWED_JWT_SESSION_ATTRIBUTES.isEmpty()) {
                for (Map.Entry<String, Claim> entry : jwt.getClaims().entrySet()) {
                    if (entry.getKey().startsWith(MCRJWTUtil.JWT_SESSION_ATTRIBUTE_PREFIX)) {
                        final String key = entry.getKey()
                            .substring(MCRJWTUtil.JWT_SESSION_ATTRIBUTE_PREFIX.length());
                        for (String prefix : ALLOWED_JWT_SESSION_ATTRIBUTES) {
                            if (key.startsWith(prefix)) {
                                currentSession.put(key, entry.getValue().asString());
                                break;
                            }
                        }
                    }
                }
            }
        } catch (JWTVerificationException e) {
            LOGGER.error(e::getMessage);
            Map<String, String> attrs = new LinkedHashMap<>();
            attrs.put("error", "invalid_token");
            attrs.put("error_description", e.getMessage());
            throw new NotAuthorizedException(e.getMessage(), e,
                MCRRestAPIUtil.getWWWAuthenticateHeader("Bearer", attrs, app));
        }
        return userInformation;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        LOGGER.debug("ResponseFilter start");
        try {
            MCRSessionMgr.unlock();
            MCRSessionMgr.getCurrentSession();
            if (responseContext.getStatus() == Response.Status.FORBIDDEN.getStatusCode()
                && isUnAuthorized(requestContext)) {
                LOGGER.debug("Guest detected, change response from FORBIDDEN to UNAUTHORIZED.");
                responseContext.setStatus(Response.Status.UNAUTHORIZED.getStatusCode());
                responseContext.getHeaders().putSingle(HttpHeaders.WWW_AUTHENTICATE,
                    MCRRestAPIUtil.getWWWAuthenticateHeader("Basic", null, app));
            }
            if (responseContext.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()
                && doNotWWWAuthenticate(requestContext)) {
                LOGGER.debug("Remove {} header.", HttpHeaders.WWW_AUTHENTICATE);
                responseContext.getHeaders().remove(HttpHeaders.WWW_AUTHENTICATE);
            }
            addJWTToResponse(requestContext, responseContext);
            if (responseContext.hasEntity()) {
                responseContext.setEntityStream(new ProxyOutputStream(responseContext.getEntityStream()) {
                    @Override
                    public void close() throws IOException {
                        LOGGER.debug("Closing EntityStream");
                        try {
                            super.close();
                        } finally {
                            closeSessionIfNeeded();
                            LOGGER.debug("Closing EntityStream done");
                        }
                    }
                });
            } else {
                LOGGER.debug("No Entity in response, closing MCRSession");
                closeSessionIfNeeded();
            }
        } finally {
            LOGGER.debug("ResponseFilter stop");
        }
    }

    private static class MCRJWTUserInformation implements MCRUserInformation {

        private final DecodedJWT jwt;

        MCRJWTUserInformation(DecodedJWT token) {
            this.jwt = token;
        }

        @Override
        public String getUserID() {
            return jwt.getSubject();
        }

        @Override
        public boolean isUserInRole(String role) {
            return Arrays.asList(jwt.getClaim("mcr:roles").asArray(String.class)).contains(role);
        }

        @Override
        public String getUserAttribute(String attribute) {
            return switch (attribute) {
                case ATT_REAL_NAME -> jwt.getClaim("name").asString();
                case ATT_EMAIL -> jwt.getClaim("email").asString();
                case MCRRealm.USER_INFORMATION_ATTR -> {
                    if (getUserID().contains("@")) {
                        yield getUserID().substring(getUserID().lastIndexOf('@') + 1);
                    }
                    yield null;
                }
                default -> jwt.getClaim(MCRJWTUtil.JWT_USER_ATTRIBUTE_PREFIX + attribute).asString();
            };
        }
    }

    private static class MCRRestSecurityContext implements SecurityContext {
        private final MCRUserInformation ui;

        private final boolean isSecure;

        private final Principal principal;

        MCRRestSecurityContext(MCRUserInformation ui, boolean isSecure) {
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
            if (ui.getUserID().equals(MCRSystemUserInformation.GUEST.getUserID())) {
                return null;
            }
            if (ui instanceof MCRUser) {
                return BASIC_AUTH;
            }
            if (ui instanceof MCRJWTUserInformation) {
                return "BEARER";
            }
            return null;
        }
    }
}
