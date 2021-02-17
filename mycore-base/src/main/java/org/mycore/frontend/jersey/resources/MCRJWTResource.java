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
import java.io.UnsupportedEncodingException;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.frontend.jersey.MCRCacheControl;
import org.mycore.frontend.jersey.MCRJWTUtil;
import org.mycore.frontend.jersey.MCRStaticContent;
import org.mycore.frontend.servlets.MCRServlet;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

/**
 * @author Thomas Scheffler (yagee)
 */
@Path("/jwt")
public class MCRJWTResource {

    public static final String AUDIENCE = "mcr:session";

    @Context
    HttpServletRequest request;

    @GET
    @Produces(MediaType.APPLICATION_JSON + "; charset=UTF-8")
    @MCRStaticContent
    @MCRCacheControl(noTransform = true,
        noStore = true,
        private_ = @MCRCacheControl.FieldArgument(active = true),
        noCache = @MCRCacheControl.FieldArgument(active = true))
    public Response getTokenFromSession() throws IOException {
        if (!Optional.ofNullable(request.getSession(false))
            .map(s -> s.getAttribute(MCRServlet.ATTR_MYCORE_SESSION))
            .isPresent()) {
            return MCRJWTUtil.getJWTLoginErrorResponse("No active MyCoRe session found.");
        }
        MCRSession mcrSession = MCRServlet.getSession(request);
        String jwt = getToken(mcrSession);
        return MCRJWTUtil.getJWTLoginSuccessResponse(jwt);
    }

    private String getToken(MCRSession mcrSession) throws UnsupportedEncodingException {
        MCRUserInformation userInformation = mcrSession.getUserInformation();
        String issuer = request.getRequestURL().toString();
        return MCRJWTUtil.getJWTBuilder(userInformation)
            .withJWTId(mcrSession.getID())
            .withIssuer(issuer)
            .withAudience(AUDIENCE)
            .withClaim(MCRJWTUtil.JWT_CLAIM_IP, mcrSession.getCurrentIP())
            .sign(MCRJWTUtil.getJWTAlgorithm());
    }

    public static void validate(String token) throws JWTVerificationException {
        if (!Optional.of(JWT.require(MCRJWTUtil.getJWTAlgorithm())
            .withAudience(AUDIENCE)
            .build().verify(token))
            .map(DecodedJWT::getId)
            .map(MCRSessionMgr::getSession)
            .isPresent()) {
            throw new JWTVerificationException("MCRSession is invalid.");
        }
    }

}
