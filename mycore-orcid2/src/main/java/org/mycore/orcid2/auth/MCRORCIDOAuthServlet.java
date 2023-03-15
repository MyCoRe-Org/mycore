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

package org.mycore.orcid2.auth;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.UriBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.streams.MCRMD5InputStream;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.orcid2.MCRORCIDConstants;
import org.mycore.orcid2.client.exception.MCRORCIDRequestException;
import org.mycore.orcid2.user.MCRORCIDUserCredential;
import org.mycore.orcid2.user.MCRORCIDSessionUtils;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

/**
 * Implements ORCID OAuth2 authorization.
 *
 * User should invoke MCROAuthServlet without any parameters. The servlet will
 * redirect the user to orcid.org authorization. The user will login at
 * orcid.org and accept or deny this application as trusted party for the
 * activity scopes defined in MCR.ORCID2.OAuth.Scopes. orcid.org then redirects
 * the user's browser to this servlet again. If the scopes were accepted by
 * user, the response contains a code parameter. This code is exchanged for an
 * access token and stored in the user's attributes here.
 *
 * See <a href="https://members.orcid.org/api/oauth/3legged-oauth">ORCID documentation</a>
 */
public class MCRORCIDOAuthServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CONFIG_PREFIX = MCRORCIDConstants.CONFIG_PREFIX + "OAuth.";

    private static final String USER_SERVLET_PATH = MCRConfiguration2.getStringOrThrow(CONFIG_PREFIX + "User.Servlet");

    private static final String ORCID_BASE_URL
        = MCRConfiguration2.getStringOrThrow(MCRORCIDConstants.CONFIG_PREFIX + "BaseURL");

    private static final String SCOPE = MCRConfiguration2.getString(CONFIG_PREFIX + "Scope").orElse(null);

    private static final boolean IS_PREFILL_REGISTRATION_FORM = MCRConfiguration2
        .getOrThrow(MCRORCIDConstants.CONFIG_PREFIX + "PreFillRegistrationForm", Boolean::parseBoolean);

    /**
     * Servlet url.
     */
    private String redirectURI;

    /**
     * Current user's profile url.
     */
    private String userProfileURL;

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        if (MCRSystemUserInformation.getGuestInstance().getUserID()
            .equals(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID())) {
            job.getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        final HttpServletRequest req = job.getRequest();
        final HttpServletResponse res = job.getResponse();
        redirectURI = MCRFrontendUtil.getBaseURL() + job.getRequest().getServletPath().substring(1);
        userProfileURL = MCRServlet.getServletBaseURL() + USER_SERVLET_PATH;
        final String action = job.getRequest().getParameter("action");
        if (job.getRequest().getParameter("code") != null) {
            handleCode(req, res);
        } else if (action != null) {
            if ("auth".equalsIgnoreCase(action)) {
                handleAuth(req, res);
            } else {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action");
            }
        } else {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "An action must be specified");
        }
    }

    private void handleCode(HttpServletRequest req, HttpServletResponse res) throws Exception {
        final String code = req.getParameter("code");
        final String error = req.getParameter("error");
        if ((error != null) && !error.trim().isEmpty()) {
            res.sendRedirect(userProfileURL + "&XSL.error=" + error); // TODO check
        } else if (code != null && !code.trim().isEmpty()) {
            final String state = req.getParameter("state");
            if (!buildStateParam().equals(state)) {
                String msg = "Invalid state, possibly cross-site request forgery?";
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, msg);
            }
            try {
                final MCRORCIDOAuthAccessTokenResponse accessTokenResponse
                    = MCRORCIDOAuthClient.getInstance().exchangeCode(code, redirectURI);
                final MCRORCIDUserCredential credential = accessTokenResponseToUserCredential(accessTokenResponse);
                MCRORCIDSessionUtils.getCurrentUser().storeCredential(accessTokenResponse.getORCID(), credential);
                res.sendRedirect(userProfileURL);
            } catch (MCRORCIDRequestException e) {
                LOGGER.error("Cannot exchange token.", e);
            }
        }
    }

    private MCRORCIDUserCredential accessTokenResponseToUserCredential(MCRORCIDOAuthAccessTokenResponse response) {
        final MCRORCIDUserCredential credential = new MCRORCIDUserCredential(response.getAccessToken());
        credential.setTokenType(response.getTokenType());
        credential.setRefreshToken(response.getRefreshToken());
        final LocalDate expireDate = LocalDateTime.now(ZoneId.systemDefault())
            .plusSeconds(Integer.parseInt(response.getExpiresIn()))
            .toLocalDate();
        credential.setExpiration(expireDate);
        credential.setScope(response.getScope());
        return credential;
    }

    private void handleAuth(HttpServletRequest req, HttpServletResponse res) throws Exception {
        final String scopeParam = req.getParameter("scope");
        if (scopeParam != null) {
            res.sendRedirect(buildRequestCodeURL(scopeParam).toString());
        } else if (SCOPE != null) {
            LOGGER.info("No scope param, using default scope ({}) as fallback.", SCOPE);
            res.sendRedirect(buildRequestCodeURL(SCOPE).toString());
        } else {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Scope is required");
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
     * @return url to request authorization code
     * @throws URISyntaxException if BaseURL is malformed
     * @throws MalformedURLException if BaseURL is malformed
     */
    private URL buildRequestCodeURL(String scope) throws URISyntaxException, MalformedURLException {
        UriBuilder builder = UriBuilder.fromPath(ORCID_BASE_URL);
        builder.path("oauth/authorize");
        builder.queryParam("redirect_uri", redirectURI);
        builder.queryParam("client_id", MCRORCIDOAuthClient.CLIENT_ID);
        builder.queryParam("response_type", "code");
        builder.queryParam("scope", scope);
        builder.queryParam("prompt", "login");
        builder.queryParam("lang", "en"); // hard coded because german is not support
        builder.queryParam("state", buildStateParam());
        if (IS_PREFILL_REGISTRATION_FORM) {
            preFillRegistrationForm(builder);
        }
        return builder.build().toURL();
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

    /**
     * Builds a state parameter to be used with OAuth to defend against cross-site
     * request forgery. Comparing state ensures the user is still the same that
     * initiated the authorization process.
     *
     * @return current user's hashed user id
     */
    private static String buildStateParam() {
        String userID = MCRUserManager.getCurrentUser().getUserID();
        byte[] bytes = userID.getBytes(StandardCharsets.UTF_8);
        MessageDigest md5Digest = MCRMD5InputStream.buildMD5Digest();
        md5Digest.update(bytes);
        byte[] digest = md5Digest.digest();
        return MCRMD5InputStream.getMD5String(digest);
    }
}
