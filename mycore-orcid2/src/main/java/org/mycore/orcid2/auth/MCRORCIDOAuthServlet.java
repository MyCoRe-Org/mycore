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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.orcid2.MCRORCIDConstants;
import org.mycore.orcid2.MCRORCIDUtils;
import org.mycore.orcid2.client.MCRORCIDCredential;
import org.mycore.orcid2.client.exception.MCRORCIDRequestException;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.user.MCRORCIDSessionUtils;
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

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        final HttpServletRequest req = job.getRequest();
        final HttpServletResponse res = job.getResponse();
        if (MCRSystemUserInformation.getGuestInstance().getUserID()
            .equals(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID())) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        if (req.getParameter("code") != null) {
            final String userID = MCRUserManager.getCurrentUser().getUserID();
            final String state = MCRORCIDUtils.hashString(userID);
            if (!state.equals(req.getParameter("state"))) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "State is invalid");
            }
            final String code = req.getParameter("code").trim();
            if (code.isEmpty()) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Code is empty");
            }
            try {
                final String redirectURI = MCRFrontendUtil.getBaseURL() + req.getServletPath().substring(1);
                handleCode(code, redirectURI);
                final String userProfileURL = MCRServlet.getServletBaseURL() + USER_SERVLET_PATH;
                res.sendRedirect(userProfileURL);
            } catch (MCRORCIDException e) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            }
        } else if (req.getParameter("error") != null) {
            final String error = req.getParameter("error");
            LOGGER.error(error);
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "ORCID was unable to process the request"); // TODO
        } else {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "An action must be specified");
        }
    }

    private void handleCode(String code, String redirectURI) {
        try {
            final MCRORCIDOAuthAccessTokenResponse accessTokenResponse
                = MCRORCIDOAuthClient.getInstance().exchangeCode(code, redirectURI);
            final MCRORCIDCredential credential = accessTokenResponseToUserCredential(accessTokenResponse);
            MCRORCIDSessionUtils.getCurrentUser().storeCredential(accessTokenResponse.getORCID(), credential);
        } catch (MCRORCIDRequestException e) {
            throw new MCRORCIDException("Cannot exchange token");
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
}
