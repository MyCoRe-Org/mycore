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

package org.mycore.orcid.oauth;

import java.io.IOException;
import java.io.Serial;
import java.net.URISyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.orcid.user.MCRORCIDSession;
import org.mycore.orcid.user.MCRORCIDUser;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Implements ORCID OAuth2 authorization.
 * <p>
 * User should invoke MCROAuthServlet without any parameters.
 * The servlet will redirect the user to orcid.org authorization.
 * The user will login at orcid.org and accept or deny this application as trusted party
 * for the activity scopes defined in MCR.ORCID.OAuth.Scopes.
 * orcid.org then redirects the user's browser to this servlet again.
 * If the scopes were accepted by user, the response contains a code parameter.
 * This code is exchanged for an access token and stored in the user's attributes here.
 * <p>
 * See https://members.orcid.org/api/oauth/3legged-oauth
 *
 * @author Frank Lützenkirchen
 */
public class MCROAuthServlet extends MCRServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger();

    private String scopes = MCRConfiguration2.getStringOrThrow("MCR.ORCID.OAuth.Scopes");

    private String userServlet = MCRConfiguration2.getStringOrThrow("MCR.ORCID.OAuth.User.Servlet");

    private String redirectURL;

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        String baseURL = MCRFrontendUtil.getBaseURL();

        this.redirectURL = baseURL + job.getRequest().getServletPath().substring(1);
        String userProfileURL = getServletBaseURL() + userServlet;

        String code = job.getRequest().getParameter("code");
        String error = job.getRequest().getParameter("error");

        if (error != null && !error.isBlank()) {
            job.getResponse().sendRedirect(userProfileURL + "&XSL.error=" + error);
        } else if (code == null || code.isBlank()) {
            redirectToGetAuthorization(job);
        } else {
            String state = job.getRequest().getParameter("state");
            if (!MCROAuthClient.buildStateParam().equals(state)) {
                String msg = "Invalid state, possibly cross-site request forgery?";
                job.getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED, msg);
            }

            MCRTokenResponse token = exchangeCodeForAccessToken(code);

            MCRORCIDUser orcidUser = MCRORCIDSession.getCurrentUser();
            orcidUser.store(token);
            orcidUser.getProfile().getWorksSection();

            job.getResponse().sendRedirect(userProfileURL);
        }
    }

    private void redirectToGetAuthorization(MCRServletJob job)
        throws URISyntaxException, IOException {
        String url = MCROAuthClient.obtainInstance().getCodeRequestURL(redirectURL, scopes);
        job.getResponse().sendRedirect(url);
    }

    private MCRTokenResponse exchangeCodeForAccessToken(String code)
        throws JsonProcessingException, IOException {
        MCRTokenRequest request = MCROAuthClient.obtainInstance().getTokenRequest();
        request.set("grant_type", "authorization_code");
        request.set("code", code);
        request.set("redirect_uri", redirectURL);

        MCRTokenResponse token = request.post();
        LOGGER.info(() -> "access granted for " + token.getORCID() + " " + token.getAccessToken());
        return token;
    }
}
