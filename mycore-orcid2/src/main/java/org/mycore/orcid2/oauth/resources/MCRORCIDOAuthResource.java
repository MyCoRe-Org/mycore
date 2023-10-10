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

package org.mycore.orcid2.oauth.resources;

import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJAXBContent;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.jersey.MCRJerseyUtil;
import org.mycore.frontend.jersey.access.MCRRequireLogin;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;
import org.mycore.orcid2.MCRORCIDConstants;
import org.mycore.orcid2.MCRORCIDUtils;
import org.mycore.orcid2.client.MCRORCIDCredential;
import org.mycore.orcid2.client.exception.MCRORCIDRequestException;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.oauth.MCRORCIDOAuthAccessTokenResponse;
import org.mycore.orcid2.oauth.MCRORCIDOAuthClient;
import org.mycore.orcid2.user.MCRORCIDSessionUtils;
import org.mycore.orcid2.user.MCRORCIDUser;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Resource for ORCID OAuth methods.
 */
@Path("orcid/oauth")
public class MCRORCIDOAuthResource {

    private static final Logger LOGGER = LogManager.getLogger(MCRORCIDOAuthResource.class);

    private static final String CONFIG_PREFIX = MCRORCIDConstants.CONFIG_PREFIX + "OAuth.";

    private static final String SCOPE = MCRConfiguration2.getString(CONFIG_PREFIX + "Scope").orElse(null);

    private static final boolean IS_PREFILL_REGISTRATION_FORM = MCRConfiguration2
        .getOrThrow(MCRORCIDConstants.CONFIG_PREFIX + "PreFillRegistrationForm", Boolean::parseBoolean);

    private final String redirectURI = "rsc/orcid/oauth";

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
    @MCRRestrictedAccess(MCRRequireLogin.class)
    public InputStream handleCodeRequest(@QueryParam("code") String code, @QueryParam("state") String state,
        @QueryParam("error") String error, @QueryParam("error_description") String errorDescription) {
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

    /**
     * Returns authorization URI
     *
     * @param scope the scope
     * @return auth URI
     * @throws WebApplicationException if scope is null
     */
    @GET
    @Path("init")
    @MCRRestrictedAccess(MCRRequireLogin.class)
    public Response getOAuthURI(@QueryParam("scope") String scope) {
        String langCode = MCRSessionMgr.getCurrentSession().getCurrentLanguage();
        if (!MCRORCIDConstants.SUPPORTED_LANGUAGE_CODES.contains(langCode)) {
            // use english as fallback
            langCode = "en";
        }
        final String userID = MCRUserManager.getCurrentUser().getUserID();
        final String state = MCRORCIDUtils.hashString(userID);
        if (scope != null) {
            return Response.seeOther(buildRequestCodeURI(scope, state, langCode)).build();
        } else if (SCOPE != null) {
            return Response.seeOther(buildRequestCodeURI(SCOPE, state, langCode)).build();
        } else {
            throw new WebApplicationException("Scope is required", Status.BAD_REQUEST);
        }
    }

    /**
     * Builds the URL where to redirect the user's browser to initiate a three-way
     * authorization and request permission to access the given scopes. If
     *
     * MCR.ORCID2.PreFillRegistrationForm=true
     *
     * submits the current user's email address, first and last name to the ORCID
     * registration form to simplify registration. May be disabled for more data
     * privacy.
     *
     * @param code the code
     * @return url to request authorization code
     */

    private MCRContent handleCode(String code) {
        try {
            final MCRORCIDOAuthAccessTokenResponse accessTokenResponse = MCRORCIDOAuthClient.getInstance()
                .exchangeCode(code, MCRFrontendUtil.getBaseURL() + redirectURI);
            final MCRORCIDCredential credential = accessTokenResponseToUserCredential(accessTokenResponse);
            final MCRORCIDUser orcidUser = MCRORCIDSessionUtils.getCurrentUser();
            orcidUser.addCredential(accessTokenResponse.getORCID(), credential);
            MCRUserManager.updateUser(orcidUser.getUser());
            return marshalOAuthAccessTokenResponse(accessTokenResponse);
        } catch (IllegalArgumentException e) {
            throw new MCRORCIDException("Cannot create response", e);
        } catch (MCRORCIDRequestException e) {
            throw new MCRORCIDException(
                "Cannot exchange token. Response was: " + e.getResponse().readEntity(String.class), e);
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

    private URI buildRequestCodeURI(String scope, String state, String langCode) {
        final UriBuilder builder = UriBuilder.fromPath(MCRORCIDConstants.ORCID_BASE_URL);
        builder.path("oauth/authorize");
        builder.queryParam("redirect_uri", MCRFrontendUtil.getBaseURL() + redirectURI);
        builder.queryParam("client_id", MCRORCIDOAuthClient.CLIENT_ID);
        builder.queryParam("response_type", "code");
        builder.queryParam("scope", scope);
        builder.queryParam("prompt", "login");
        builder.queryParam("lang", langCode);
        builder.queryParam("state", state);
        if (IS_PREFILL_REGISTRATION_FORM) {
            preFillRegistrationForm(builder);
        }
        return builder.build();
    }

    /**
     *
     * Adds current user's email address, first and last name as params to URIBuilder.
     *
     * @param builder the builder
     * See <a href="https://members.orcid.org/api/resources/customize">ORCID documentation</a>
     */
    private static void preFillRegistrationForm(UriBuilder builder) {
        MCRUser user = MCRUserManager.getCurrentUser();
        String email = user.getEMailAddress();
        if (email != null) {
            builder.queryParam("email", email);
        }
        String name = user.getRealName();
        String firstName = null;
        String lastName = name;
        if (name.contains(",")) {
            String[] nameParts = name.split(",");
            if (nameParts.length == 2) {
                firstName = nameParts[1].trim();
                lastName = nameParts[0].trim();
            }
        } else if (name.contains(" ")) {
            String[] nameParts = name.split(" ");
            if (nameParts.length == 2) {
                firstName = nameParts[0].trim();
                lastName = nameParts[1].trim();
            }
        }
        if (firstName != null) {
            builder.queryParam("given_names", firstName);
        }
        if (lastName != null) {
            builder.queryParam("family_names", lastName);
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
