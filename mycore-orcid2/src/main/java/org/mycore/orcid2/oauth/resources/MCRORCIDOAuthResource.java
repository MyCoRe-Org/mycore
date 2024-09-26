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
import org.mycore.orcid2.MCRORCIDConstants;
import org.mycore.orcid2.MCRORCIDUtils;
import org.mycore.orcid2.client.MCRORCIDCredential;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.oauth.MCRORCIDOAuthAccessTokenResponse;
import org.mycore.orcid2.oauth.MCRORCIDOAuthClient;
import org.mycore.orcid2.oauth.MCRORCIDOAuthUserInformation;
import org.mycore.orcid2.user.MCRORCIDSessionUtils;
import org.mycore.orcid2.user.MCRORCIDUser;
import org.mycore.user2.MCRTransientUser;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
        .getOrThrow(CONFIG_PREFIX + "PreFillRegistrationForm", Boolean::parseBoolean);

    private static final boolean ALLOW_GUESTS_TO_AUTH = MCRConfiguration2
        .getOrThrow(CONFIG_PREFIX + "EnableForGuests", Boolean::parseBoolean);

    private static final boolean PERSIST_USER = MCRConfiguration2
        .getOrThrow(CONFIG_PREFIX + "PersistUser", Boolean::parseBoolean);

    private final String redirectURI = "rsc/orcid/oauth";

    @Context
    HttpServletRequest req;

    /**
     * Returns authorization URI
     *
     * @param scope the scope
     * @return auth URI
     * @throws ForbiddenException if user is not allowed to initiate authentication
     * @throws BadRequestException if scope is null
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
                    buildRequestCodeURI(scope, state, langCode, MCRUserManager.getCurrentUser(), prefillRegistration))
                .build();
        } else if (SCOPE != null) {
            return Response
                .seeOther(
                    buildRequestCodeURI(SCOPE, state, langCode, MCRUserManager.getCurrentUser(), prefillRegistration))
                .build();
        } else {
            throw new BadRequestException("Scope is required");
        }
    }

    private URI buildRequestCodeURI(String scope, String state, String langCode, MCRUser user,
        boolean prefillRegistration) {
        final UriBuilder builder = UriBuilder.fromPath(MCRORCIDConstants.ORCID_BASE_URL);
        builder.path("oauth/authorize");
        builder.queryParam("redirect_uri", MCRFrontendUtil.getBaseURL() + redirectURI);
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
     *
     * Adds current user's email address, first and last name as params to URIBuilder.
     *
     * @param builder the builder
     * @param user the user
     * See <a href="https://members.orcid.org/api/resources/customize">ORCID documentation</a>
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
     * Handles ORCID code request.
     *
     * @param code the code
     * @param state the state
     * @param error the error
     * @param errorDescription the error description
     * @return Response
     * @throws BadRequestException if the request is invalid
     * @throws InternalServerErrorException if an error occurs during processing the request
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
                .exchangeCode(codeTrimmed, MCRFrontendUtil.getBaseURL() + redirectURI);
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
            throw new IllegalArgumentException("Invalid auth response");
        }
    }

    private static MCRContent marshalOAuthAccessTokenResponse(MCRORCIDOAuthAccessTokenResponse tokenResponse) {
        try {
            return new MCRJAXBContent<>(JAXBContext.newInstance(MCRORCIDOAuthAccessTokenResponse.class), tokenResponse);
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
