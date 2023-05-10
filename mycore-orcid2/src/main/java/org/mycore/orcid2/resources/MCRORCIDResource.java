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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;

import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.orcid2.MCRORCIDUtils;
import org.mycore.orcid2.MCRORCIDConstants;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.oauth.MCRORCIDOAuthClient;
import org.mycore.orcid2.user.MCRORCIDSessionUtils;
import org.mycore.orcid2.user.MCRORCIDUser;
import org.mycore.orcid2.user.MCRORCIDUserUtils;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

/**
 * Base resource for orcid methods.
 */
@Path("v1")
public class MCRORCIDResource {

    private static final String CONFIG_PREFIX = MCRORCIDConstants.CONFIG_PREFIX + "OAuth.";

    private static final String SCOPE = MCRConfiguration2.getString(CONFIG_PREFIX + "Scope").orElse(null);

    private static final boolean IS_PREFILL_REGISTRATION_FORM = MCRConfiguration2
        .getOrThrow(MCRORCIDConstants.CONFIG_PREFIX + "PreFillRegistrationForm", Boolean::parseBoolean);

    @Context
    HttpServletRequest req;

    /**
     * Returns the ORCID status of the current user as JSON, e.g.
     *
     * {
     *   orcids: ['0000-0001-5484-889X'],
     *   trustedOrcids: ['0000-0001-5484-889X'],
     * }
     * @return the user status
     * @throws WebApplicationException if user is guest
     */
    @GET
    @Path("user-status")
    @Produces(MediaType.APPLICATION_JSON)
    public MCRORCIDUserStatus getUserStatus() {
        if (MCRORCIDUtils.isCurrentUserGuest()) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        final MCRORCIDUser user = MCRORCIDSessionUtils.getCurrentUser();
        final String[] orcids = user.getORCIDs().toArray(String[]::new);
        final String[] credentials = user.getCredentials().entrySet().stream().map(Map.Entry::getKey)
            .toArray(String[]::new);
        return new MCRORCIDUserStatus(orcids, credentials);
    }

    /**
     * Revokes ORCID iD for current user.
     * 
     * @param orcid the ORCID iD
     * @param redirectString optional redirect URI as String
     * @return Response
     * @throws WebApplicationException if orcid is null, user is guest or revoke fails
     */
    @POST
    @Path("revoke/{orcid}")
    public Response revoke(@PathParam("orcid") String orcid, @QueryParam("redirect_uri") String redirectString) {
        if (orcid == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        if (MCRORCIDUtils.isCurrentUserGuest()) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        URI redirectURI = null;
        if (redirectString != null) {
            try {
                redirectURI = new URI(redirectString);
            } catch (URISyntaxException e) {
                throw new WebApplicationException(Status.BAD_REQUEST);
            }
        }
        final MCRORCIDUser orcidUser = MCRORCIDSessionUtils.getCurrentUser();
        try {
            MCRORCIDUserUtils.revokeCredentialByORCID(orcidUser, orcid);
        } catch (MCRORCIDException e) {
            throw new WebApplicationException(e, Status.BAD_REQUEST);
        }
        if (redirectURI != null) {
            return Response.seeOther(redirectURI).build();
        }
        return Response.ok().build();
    }

    /**
     * Returns auth URI
     * 
     * @param scope the scope
     * @return auth URI
     * @throws WebApplicationException if scope is null
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("oauth-uri")
    public String getOAuthURI(@QueryParam("scope") String scope) {
        if (MCRORCIDUtils.isCurrentUserGuest()) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        String langCode = MCRSessionMgr.getCurrentSession().getCurrentLanguage();
        if (!MCRORCIDConstants.SUPPORTED_LANGUAGE_CODES.contains(langCode)) {
            // use english as fallback
            langCode = "en";
        }
        final String userID = MCRUserManager.getCurrentUser().getUserID();
        final String state = MCRORCIDUtils.hashString(userID);
        if (scope != null) {
            return buildRequestCodeURI(scope, state, langCode).toString();
        } else if (SCOPE != null) {
            return buildRequestCodeURI(SCOPE, state, langCode).toString();
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
     * @param scope not encoded scope string
     * @param langCode language code
     * @return url to request authorization code
     */
    private URI buildRequestCodeURI(String scope, String state, String langCode) {
        final UriBuilder builder = UriBuilder.fromPath(MCRORCIDConstants.ORCID_BASE_URL);
        final String redirectURI = MCRFrontendUtil.getBaseURL(req) + "rsc/orcid/oauth";
        builder.path("oauth/authorize");
        builder.queryParam("redirect_uri", redirectURI);
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

    static class MCRORCIDUserStatus {

        private String[] orcids;

        private String[] trustedOrcids;

        MCRORCIDUserStatus(String[] orcids, String[] trustedOrcids) {
            this.orcids = orcids;
            this.trustedOrcids = trustedOrcids;
        }

        public String[] getOrcids() {
            return orcids;
        }

        public String[] getTrustedOrcids() {
            return trustedOrcids;
        }
    }
}
