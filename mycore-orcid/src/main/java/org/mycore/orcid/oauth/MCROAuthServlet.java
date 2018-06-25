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

package org.mycore.orcid.oauth;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.orcid.user.MCRORCIDSession;
import org.mycore.orcid.user.MCRORCIDUser;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Implements ORCID OAuth2 authorization.
 *
 * User should invoke MCROAuthServlet without any parameters.
 * The servlet will redirect the user to orcid.org authorization.
 * The user will login at orcid.org and accept or deny this application as trusted party
 * for the activity scopes defined in MCR.ORCID.OAuth.Scopes.
 * orcid.org then redirects the user's browser to this servlet again.
 * If the scopes were accepted by user, the response contains a code parameter.
 * This code is exchanged for an access token and stored in the user's attributes here.
 *
 * See https://members.orcid.org/api/oauth/3legged-oauth
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCROAuthServlet extends MCRServlet {

    private final static Logger LOGGER = LogManager.getLogger(MCROAuthServlet.class);

    private String scopes = MCRConfiguration.instance().getString("MCR.ORCID.OAuth.Scopes");

    private final static String USER_PROFILE_URL = MCRFrontendUtil.getBaseURL() + "servlets/MCRUserServlet?action=show";

    private String redirectURL;

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        this.redirectURL = MCRFrontendUtil.getBaseURL() + job.getRequest().getServletPath().substring(1);

        String code = job.getRequest().getParameter("code");
        String error = job.getRequest().getParameter("error");

        if ((error != null) && !error.trim().isEmpty()) {
            job.getResponse().sendRedirect(USER_PROFILE_URL + "&XSL.error=" + error);
        } else if ((code == null) || code.trim().isEmpty()) {
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

            job.getResponse().sendRedirect(USER_PROFILE_URL);
        }
    }

    private void redirectToGetAuthorization(MCRServletJob job)
        throws URISyntaxException, MalformedURLException, IOException {
        String url = MCROAuthClient.instance().getCodeRequestURL(redirectURL, scopes);
        job.getResponse().sendRedirect(url);
    }

    private MCRTokenResponse exchangeCodeForAccessToken(String code)
        throws JsonProcessingException, IOException, JDOMException, SAXException {
        MCRTokenRequest request = MCROAuthClient.instance().getTokenRequest();
        request.set("grant_type", "authorization_code");
        request.set("code", code);
        request.set("redirect_uri", redirectURL);

        MCRTokenResponse token = request.post();
        LOGGER.info("access granted for " + token.getORCID() + " " + token.getAccessToken());
        return token;
    }
}
