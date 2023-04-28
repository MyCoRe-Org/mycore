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

package org.mycore.orcid2.resources;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJAXBContent;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.jersey.MCRJerseyUtil;
import org.mycore.orcid2.MCRORCIDUtils;
import org.mycore.orcid2.client.MCRORCIDCredential;
import org.mycore.orcid2.client.exception.MCRORCIDRequestException;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.auth.MCRORCIDOAuthAccessTokenResponse;
import org.mycore.orcid2.auth.MCRORCIDOAuthClient;
import org.mycore.orcid2.user.MCRORCIDSessionUtils;
import org.mycore.user2.MCRUserManager;

/**
 * Resource for orcid oauth methods.
 */
@Path("orcid/oauth")
public class MCRORCIDOAuthResource {

    private static final Logger LOGGER = LogManager.getLogger();

    @Context
    HttpServletRequest req;

    /**
     * Handles ORCID code request.
     * 
     * @param code the code
     * @param state the state
     * @param error the error
     * @param errorDescription the errorDescription
     * @return Response
     * @throws WebApplicationException is request is invalid or error
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public InputStream handleCodeRequest(@QueryParam("code") String code, @QueryParam("state") String state,
        @QueryParam("error") String error, @QueryParam("error_description") String errorDescription) {
        if (MCRORCIDUtils.isCurrentUserGuest()) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        try {
            MCRContent result = null;
            if (code != null) {
                final String userID = MCRUserManager.getCurrentUser().getUserID();
                if (state == null || !Objects.equals(MCRORCIDUtils.hashString(userID), state)) {
                    throw new WebApplicationException(Status.BAD_REQUEST);
                }
                final String codeTrimmed = code.trim();
                if (codeTrimmed.isEmpty()) {
                    throw new WebApplicationException(Status.BAD_REQUEST);
                }
                result = handleCode(codeTrimmed);
            } else if (error != null) {
                result = handleError(error, errorDescription);
            } else {
                throw new WebApplicationException(Status.BAD_REQUEST);
            }
            return MCRJerseyUtil.transform(result.asXML(), req).getInputStream();
        } catch (Exception e) {
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
    }

    private MCRContent handleCode(String code) {
        try {
            final String redirectURI = MCRFrontendUtil.getBaseURL(); // no matter
            final MCRORCIDOAuthAccessTokenResponse accessTokenResponse
                = MCRORCIDOAuthClient.getInstance().exchangeCode(code, redirectURI);
            final MCRORCIDCredential credential = accessTokenResponseToUserCredential(accessTokenResponse);
            MCRORCIDSessionUtils.getCurrentUser().storeCredential(accessTokenResponse.getORCID(), credential);
            return marshalOAuthAccessTokenResponse(accessTokenResponse);
        } catch (IllegalArgumentException e) {
            throw new MCRORCIDException("Cannot create response", e);
        } catch (MCRORCIDRequestException e) {
            throw new MCRORCIDException("Cannot exchange token", e);
        }
    }

    private MCRContent handleError(String error, String errorDescription) {
        LOGGER.error(error);
        try {
            return marshalOAuthErrorResponse(new MCRORCIDOAuthErrorResponse(error, errorDescription));
        } catch (IllegalArgumentException e) {
            throw new MCRORCIDException("Cannot create response", e);
        }
    }

    private MCRORCIDCredential accessTokenResponseToUserCredential(MCRORCIDOAuthAccessTokenResponse response) {
        final MCRORCIDCredential credential = new MCRORCIDCredential(response.getAccessToken());
        credential.setTokenType(response.getTokenType());
        credential.setRefreshToken(response.getRefreshToken());
        final LocalDate expireDate = LocalDateTime.now(ZoneId.systemDefault())
            .plusSeconds(Integer.parseInt(response.getExpiresIn()))
            .toLocalDate();
        credential.setExpiration(expireDate);
        credential.setScope(response.getScope());
        return credential;
    }

    private static MCRContent marshalOAuthErrorResponse(MCRORCIDOAuthErrorResponse errorResponse) {
        try {
            return new MCRJAXBContent(JAXBContext.newInstance(MCRORCIDOAuthErrorResponse.class), errorResponse);
        } catch (JAXBException e) {
            throw new IllegalArgumentException("Invalid auth response");
        }
    }

    private static MCRContent marshalOAuthAccessTokenResponse(MCRORCIDOAuthAccessTokenResponse tokenResponse) {
        try {
            return new MCRJAXBContent(JAXBContext.newInstance(MCRORCIDOAuthAccessTokenResponse.class), tokenResponse);
        } catch (JAXBException e) {
            throw new IllegalArgumentException("Invalid token response");
        }
    }

    @XmlRootElement(name = "ORCIDOAuthErrorResponse")
    static class MCRORCIDOAuthErrorResponse {

        @XmlElement(name = "error")
        private final String error;

        @XmlElement(name = "errorDescription")
        private final String errorDescription;

        MCRORCIDOAuthErrorResponse() {
            this(null, null);
        }

        MCRORCIDOAuthErrorResponse(String error, String errorDescription) {
            this.error = error;
            this.errorDescription = errorDescription;
        }
    }
}
