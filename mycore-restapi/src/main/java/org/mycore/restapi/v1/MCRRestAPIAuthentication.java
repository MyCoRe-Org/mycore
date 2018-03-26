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

package org.mycore.restapi.v1;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.jersey.MCRCacheControl;
import org.mycore.frontend.jersey.MCRJWTUtil;
import org.mycore.frontend.jersey.MCRJerseyUtil;
import org.mycore.frontend.jersey.access.MCRRequireLogin;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;
import org.mycore.restapi.v1.utils.MCRRestAPIUtil;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;

/**
 * Rest Controller that handles authentication.
 *
 * @author Thomas Scheffler
 * @author Robert Stephan
 *
 */
@Path("/v1/auth")
public class MCRRestAPIAuthentication {

    private static final int EXPIRATION_TIME_MINUTES = 10;

    public static final String AUDIENCE = "mcr:rest-auth";

    /**
     * Unauthenticated requests should return a response whose header contains a HTTP 401 Unauthorized status and a
     * WWW-Authenticate field.
     * 
     * 200 OK Content-Type: application/json;charset=UTF-8
     * 
     * { "access_token": "NgCXRK...MzYjw", "token_type": "Bearer", "expires_at": 1372700873, "refresh_token":
     * "NgAagA...Um_SHo" }
     * 
     * Returning the JWT (Java Web Token to the client is not properly specified). We use the "Authorization" Header in
     * the response, which is unusual but not strictly forbidden.
     * 
     * @param authorization - content HTTP Header Authorization
     * @return response message as JSON
     */
    @GET
    @Produces({ MCRJerseyUtil.APPLICATION_JSON_UTF8 })
    @Path("/login")
    @MCRCacheControl(noTransform = true,
        noStore = true,
        private_ = @MCRCacheControl.FieldArgument(active = true),
        noCache = @MCRCacheControl.FieldArgument(active = true))
    public Response authorize(@DefaultValue("") @HeaderParam("Authorization") String authorization,
        @Context HttpServletRequest req) throws IOException {
        if (authorization.startsWith("Basic ")) {
            //login handled by MCRSessionFilter
            Optional<String> jwt = getToken(MCRSessionMgr.getCurrentSession().getUserInformation(),
                MCRFrontendUtil.getRemoteAddr(req));
            if (jwt.isPresent()) {
                return MCRJWTUtil.getJWTLoginSuccessResponse(jwt.get());
            }
        }
        throw new NotAuthorizedException(
            "Login failed. Please provide proper user name and password via HTTP Basic Authentication.",
            MCRRestAPIUtil.getWWWAuthenticateHeader("Basic", null));
    }

    public static Optional<String> getToken(MCRUserInformation userInformation, String remoteIp) {
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneOffset.UTC);
        return Optional.ofNullable(userInformation)
            .map(MCRJWTUtil::getJWTBuilder)
            .map(b -> {
                return b.withAudience(AUDIENCE)
                    .withClaim(MCRJWTUtil.JWT_CLAIM_IP, remoteIp)
                    .withExpiresAt(Date.from(currentTime.plusMinutes(EXPIRATION_TIME_MINUTES).toInstant()))
                    .withNotBefore(Date.from(currentTime.minusMinutes(EXPIRATION_TIME_MINUTES).toInstant()))
                    .sign(MCRJWTUtil.getJWTAlgorithm());
            });
    }

    @GET
    @Path("/renew")
    @MCRRestrictedAccess(MCRRequireLogin.class)
    @MCRCacheControl(noTransform = true,
        noStore = true,
        private_ = @MCRCacheControl.FieldArgument(active = true),
        noCache = @MCRCacheControl.FieldArgument(active = true))
    public Response renew(@DefaultValue("") @HeaderParam("Authorization") String authorization,
        @Context HttpServletRequest req) throws IOException {
        if (authorization.startsWith("Bearer ")) {
            //login handled by MCRSessionFilter
            Optional<String> jwt = getToken(MCRSessionMgr.getCurrentSession().getUserInformation(),
                MCRFrontendUtil.getRemoteAddr(req));
            if (jwt.isPresent()) {
                return MCRJWTUtil.getJWTRenewSuccessResponse(jwt.get());
            }
        }
        throw new NotAuthorizedException(
            "Login failed. Please provide a valid JSON Web Token for authentication.",
            MCRRestAPIUtil.getWWWAuthenticateHeader("Basic", null));
    }

    public static void validate(String token) throws JWTVerificationException {
        JWT.require(MCRJWTUtil.getJWTAlgorithm())
            .withAudience(AUDIENCE)
            .acceptLeeway(0)
            .build().verify(token);
    }
}
