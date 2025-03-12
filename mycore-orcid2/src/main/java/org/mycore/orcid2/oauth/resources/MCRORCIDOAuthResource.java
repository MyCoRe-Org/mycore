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

package org.mycore.orcid2.oauth.resources;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUserInformation;
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
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.oauth.MCRORCIDOAuthAccessTokenResponse;
import org.mycore.orcid2.oauth.MCRORCIDOAuthClient;
import org.mycore.orcid2.oauth.MCRORCIDOAuthUserInformation;
import org.mycore.orcid2.user.MCRORCIDSessionUtils;
import org.mycore.orcid2.user.MCRORCIDUser;
import org.mycore.orcid2.user.MCRORCIDUserUtils;
import org.mycore.restapi.annotations.MCRRequireTransaction;
import org.mycore.user2.MCRTransientUser;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * This class provides end points for handling the OAuth authentication process with ORCID.
 * It manages the initiation of the OAuth flow, the handling of the authorization code or error
 * returned by ORCID, and the exchange of the authorization code for an access token.
 *
 * @see <a href="https://members.orcid.org/api/oauth2">ORCID OAuth 2.0 Documentation</a> for more
 *      information on the OAuth process and integration.
 */
@Path("orcid/oauth")
public class MCRORCIDOAuthResource {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CONFIG_PREFIX = MCRORCIDConstants.CONFIG_PREFIX + "OAuth.";

    private static final String SCOPE = MCRConfiguration2.getString(CONFIG_PREFIX + "Scope").orElse(null);

    private static final boolean IS_PREFILL_REGISTRATION_FORM = MCRConfiguration2
        .getOrThrow(CONFIG_PREFIX + "PreFillRegistrationForm", Boolean::parseBoolean);

    private static final boolean ALLOW_GUESTS_TO_AUTH = MCRConfiguration2
        .getOrThrow(CONFIG_PREFIX + "EnableForGuests", Boolean::parseBoolean);

    private static final boolean PERSIST_USER = MCRConfiguration2
        .getOrThrow(CONFIG_PREFIX + "PersistUser", Boolean::parseBoolean);

    private static final String PATH_PARAM_ORCID = "orcid";

    @Context
    private HttpServletRequest req;

    @Context
    private UriInfo uriInfo;

    /**
     * Generates a redirection URI for initiating an OAuth authentication request to ORCID, based
     * on the provided scope and user session details.
     *
     * <p>If the current user is a guest and guest authentication is disabled, a {@link ForbiddenException}
     * is thrown. Otherwise, the method attempts to build the OAuth
     * request URI using the provided or default scope.</p>
     *
     * @param scope the OAuth scope defining the permissions. If null, default scope will be used (if available)
     * @return a {@link Response} that redirects the user to the constructed OAuth request URI
     * @throws ForbiddenException if the current user is a guest and guest authentication is not allowed
     * @throws BadRequestException if both the provided scope and the default scope are null
     */
    @GET
    @Path("init")
    public Response getOAuthURI(@QueryParam("scope") String scope) {
        if (checkCurrentUserIsGuest() && !ALLOW_GUESTS_TO_AUTH) {
            throw new ForbiddenException();
        }
        String langCode = MCRSessionMgr.getCurrentSession().getCurrentLanguage();
        if (!MCRORCIDConstants.SUPPORTED_LANGUAGE_CODES.contains(langCode)) {
            // use english as fallback
            langCode = "en";
        }
        final String state = MCRORCIDUtils.hashString(MCRSessionMgr.getCurrentSessionID());
        final boolean prefillRegistration = checkCurrentUserIsGuest() ? false : IS_PREFILL_REGISTRATION_FORM;

        if (scope != null) {
            return Response
                .seeOther(
                    buildRequestCodeUri(scope, state, langCode, MCRUserManager.getCurrentUser(), prefillRegistration))
                .build();
        } else if (SCOPE != null) {
            return Response
                .seeOther(
                    buildRequestCodeUri(SCOPE, state, langCode, MCRUserManager.getCurrentUser(), prefillRegistration))
                .build();
        } else {
            throw new BadRequestException("Scope is required");
        }
    }

    private URI buildRequestCodeUri(String scope, String state, String langCode, MCRUser user,
        boolean prefillRegistration) {
        final UriBuilder builder = UriBuilder.fromPath(MCRORCIDConstants.ORCID_BASE_URL);
        builder.path("oauth/authorize");
        builder.queryParam("redirect_uri", getRedirectUri());
        builder.queryParam("client_id", MCRORCIDOAuthClient.CLIENT_ID);
        builder.queryParam("response_type", "code");
        builder.queryParam("scope", scope);
        builder.queryParam("prompt", "login");
        builder.queryParam("lang", langCode);
        builder.queryParam("state", state);
        if (prefillRegistration) {
            preFillRegistrationForm(builder, user);
        }
        return builder.build();
    }

    /**
     * Adds the current user's email address, first name, and last name as query parameters
     * to the given {@link URIBuilder}. The email, first name, and last name are extracted
     * from the provided {@link MCRUser}.
     *
     * @param builder the builder
     * @param user the user
     *
     * @see <a href="https://members.orcid.org/api/resources/customize">ORCID documentation</a>
     */
    private static void preFillRegistrationForm(UriBuilder builder, MCRUser user) {
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

    /**
     * Handles the response from an OAuth authentication request. This method processes the query parameters
     * returned from ORCID after a user has either successfully authenticated or encountered an error during
     * the OAuth process.
     *
     * <p>If a valid authorization code is provided, the method verifies the state parameter to prevent CSRF attacks,
     * exchanges the code for an access token, and then either logs in the user or associates the token with the
     * current session.</p>
     *
     * <p>If an error is encountered, the method logs the error and either redirects the guest user or returns
     * a detailed error message for logged-in users.</p>
     *
     * @param code the authorization code returned from ORCID, or null if an error occurred
     * @param state the state parameter used for CSRF protection, which must match the session state
     * @param error the error code returned by ORCID if the authentication request failed, or null if no error occurred
     * @param errorDescription a human-readable description of the error, if available
     *
     * @return a {@link Response} object representing the outcome of the OAuth request
     *         This can either be a redirection to the home page or an HTML response
     *
     * @throws ForbiddenException if the current user is a guest and guest authentication is disabled
     * @throws BadRequestException if the code or state is invalid, or if neither the code nor error is provided
     * @throws InternalServerErrorException if an error occurs during the transformation of the response
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response handleCodeRequest(@QueryParam("code") String code, @QueryParam("state") String state,
        @QueryParam("error") String error, @QueryParam("error_description") String errorDescription) {
        if (checkCurrentUserIsGuest() && !ALLOW_GUESTS_TO_AUTH) {
            throw new ForbiddenException();
        }
        if (code != null) {
            final String codeTrimmed = code.trim();
            if (state == null || codeTrimmed.isEmpty()
                || !Objects.equals(MCRORCIDUtils.hashString(MCRSessionMgr.getCurrentSessionID()), state)) {
                throw new BadRequestException();
            }
            final MCRORCIDOAuthAccessTokenResponse accessTokenResponse = MCRORCIDOAuthClient.getInstance()
                .exchangeCode(codeTrimmed, getRedirectUri());
            final MCRORCIDCredential credential = accessTokenResponseToUserCredential(accessTokenResponse);
            if (checkCurrentUserIsGuest()) {
                handleLogin(accessTokenResponse.getORCID(), accessTokenResponse.getName(), credential, PERSIST_USER);
                return Response.seeOther(URI.create(MCRFrontendUtil.getBaseURL())).build();
            } else {
                addCredentialToCurrentUser(accessTokenResponse.getORCID(), credential);
                final MCRContent result = marshalOAuthAccessTokenResponse(accessTokenResponse);
                try {
                    return Response.ok(MCRJerseyUtil.transform(result.asXML(), req).getInputStream()).build();
                } catch (Exception e) {
                    throw new InternalServerErrorException(e);
                }
            }
        } else if (error != null) {
            if (checkCurrentUserIsGuest()) {
                LogManager.getLogger().error(error);
                return Response.seeOther(URI.create(MCRFrontendUtil.getBaseURL())).build();
            } else {
                final MCRContent result = handleError(error, errorDescription);
                try {
                    return Response.ok(MCRJerseyUtil.transform(result.asXML(), req).getInputStream()).build();
                } catch (Exception e) {
                    throw new InternalServerErrorException(e);
                }
            }
        } else {
            throw new BadRequestException();
        }
    }

    /**
     * Revokes the ORCID credentials associated with a specific ORCID iD for the current user.
     *
     * <p>This method revokes the credentials linked to the given ORCID iD for the currently logged-in
     * user. If the ORCID iD is null, a {@link BadRequestException} is thrown. If an error occurs
     * while attempting to revoke the credentials, a {@link BadRequestException} is thrown with the
     * underlying exception as the cause.
     *
     * @param orcid the ORCID iD to revoke credentials for
     * @return a {@link Response} indicating a successful operation (HTTP 200 OK)
     *
     * @throws BadRequestException if an error occurs during revocation
     */
    @DELETE
    @Path("{" + PATH_PARAM_ORCID + "}")
    @MCRRequireTransaction
    @MCRRestrictedAccess(MCRRequireLogin.class)
    public Response revoke(@PathParam(PATH_PARAM_ORCID) String orcid) {
        try {
            MCRORCIDUserUtils.revokeCredentialByORCID(MCRORCIDSessionUtils.getCurrentUser(), orcid);
        } catch (MCRORCIDException e) {
            throw new BadRequestException(e);
        }
        return Response.ok().build();
    }

    private String getRedirectUri() {
        return uriInfo.getBaseUri().toString() + getClass().getAnnotation(Path.class).value();
    }

    private boolean checkCurrentUserIsGuest() {
        return Objects.equals(MCRSystemUserInformation.getGuestInstance().getUserID(),
            MCRSessionMgr.getCurrentSession().getUserInformation().getUserID());
    }

    private void addCredentialToCurrentUser(String orcid, MCRORCIDCredential credential) {
        final MCRORCIDUser orcidUser = MCRORCIDSessionUtils.getCurrentUser();
        orcidUser.addCredential(orcid, credential);
        MCRUserManager.updateUser(orcidUser.getUser());
    }

    private void handleLogin(String orcid, String name, MCRORCIDCredential credential, boolean persistUser) {
        MCRUserInformation userInformation;
        final MCRUser user = MCRUserManager.getUser(orcid, MCRORCIDOAuthUserInformation.REALM_ID);
        if (user != null) {
            user.setLastLogin();
            final MCRORCIDUser orcidUser = new MCRORCIDUser(user);
            orcidUser.addCredential(orcid, credential);
            MCRUserManager.updateUser(user);
            userInformation = user;
        } else {
            userInformation = new MCRORCIDOAuthUserInformation(orcid, name, credential);
            if (persistUser) {
                final MCRORCIDUser orcidUser = new MCRORCIDUser(new MCRTransientUser(userInformation));
                orcidUser.addCredential(orcid, credential);
                MCRUserManager.createUser(orcidUser.getUser());
            }
        }
        MCRSessionMgr.getCurrentSession().setUserInformation(userInformation);
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
            return new MCRJAXBContent<>(JAXBContext.newInstance(MCRORCIDOAuthErrorResponse.class), errorResponse);
        } catch (JAXBException e) {
            throw new IllegalArgumentException("Invalid auth response", e);
        }
    }

    private static MCRContent marshalOAuthAccessTokenResponse(MCRORCIDOAuthAccessTokenResponse tokenResponse) {
        try {
            return new MCRJAXBContent<>(JAXBContext.newInstance(MCRORCIDOAuthAccessTokenResponse.class), tokenResponse);
        } catch (JAXBException e) {
            throw new IllegalArgumentException("Invalid token response", e);
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
