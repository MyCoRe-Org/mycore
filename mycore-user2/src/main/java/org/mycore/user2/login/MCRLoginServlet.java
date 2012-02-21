/**
 * $Revision: 23428 $ 
 * $Date: 2012-02-03 08:53:51 +0100 (Fr, 03 Feb 2012) $
 *
 * This file is part of the MILESS repository software.
 * Copyright (C) 2011 MILESS/MyCoRe developer team
 * See http://duepublico.uni-duisburg-essen.de/ and http://www.mycore.de/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/

package org.mycore.user2.login;

import java.io.IOException;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.user2.MCRRealm;
import org.mycore.user2.MCRRealmFactory;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

/**
 * Provides functionality to select login method,
 * change login user and show a welcome page.
 * Login methods and realms are configured in realms.xml.
 * The login form for local users is login.xml. 
 * 
 * @author Frank L\u00fctzenkirchen
 */
public class MCRLoginServlet extends MCRServlet {

    private static final String LOGIN_REDIRECT_URL_PARAMETER = "url";

    private static final String LOGIN_REDIRECT_URL_KEY = "loginRedirectURL";

    private static Logger LOGGER = Logger.getLogger(MCRLoginServlet.class);

    /**
     * MCRLoginServlet handles four actions:
     * 
     * MCRLoginServlet?url=foo
     * stores foo as redirect url and displays
     * a list of login method options.
     * 
     * MCRLoginServlet?url=foo&realm=ID
     * stores foo as redirect url and redirects
     * to the login URL of the given realm.

     * MCRLoginServlet?action=login
     * checks input from editor login form and
     * changes the current login user and redirects
     * to the stored url.
     * 
     * MCRLoginServlet?action=logout
     * changes to guest user and redirects to 
     * the stored url.
     * 
     * MCRLoginServlet?action=cancel
     * does not change login user, just
     * redirects to the target url
     */
    public void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest req = job.getRequest();
        HttpServletResponse res = job.getResponse();

        String action = req.getParameter("action");
        String realm = req.getParameter("realm");

        if ("login".equals(action) || (req.getAttribute("MCREditorSubmission") != null)) {
            login(req, res);
        } else if ("cancel".equals(action)) {
            redirect(res);
        } else if (realm != null) {
            loginToRealm(req, res, req.getParameter("realm"));
        } else {
            chooseLoginMethod(req, res);
        }
    }

    /**
     * Handles input from local login editor form, which is in the
     * page editor.xml. Current user is changed. When the user is
     * personalized (has LegalEntity associated), a welcome page
     * is shown, which is rendered using welcome.xsl. Otherwise,
     * the user is immediately redirected to the target url.
     */
    private void login(HttpServletRequest req, HttpServletResponse res) throws IOException, Exception {
        MCREditorSubmission sub = (MCREditorSubmission) (req.getAttribute("MCREditorSubmission"));
        if (sub == null) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Element root = sub.getXML().getRootElement();
        String userName = root.getChildTextTrim("userName");
        String password = root.getChildText("password");

        if (!MCRUserManager.exists(userName)) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        MCRUser user = MCRUserManager.login(userName, password);
        if (user == null) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        redirect(res);
    }

    private Element realms = MCRURIResolver.instance().resolve("resource:realms.xml");

    /**
     * Stores the target url and outputs a list of realms to login to. The list is
     * rendered using realms.xsl.
     */
    private void chooseLoginMethod(HttpServletRequest req, HttpServletResponse res) throws Exception {
        storeURL(req.getParameter(LOGIN_REDIRECT_URL_PARAMETER));
        // redirect directly to login url if there is only one realm available and the user is not logged in
        if ((getNumLoginOptions() == 1) && currentUserIsGuest())
            redirectToUniqueRealm(req, res);
        else
            listRealms(req, res);
    }

    private void redirectToUniqueRealm(HttpServletRequest req, HttpServletResponse res) throws Exception {
        String realmID = realms.getChild("realm").getAttributeValue("id");
        loginToRealm(req, res, realmID);
    }

    private void listRealms(HttpServletRequest req, HttpServletResponse res) throws IOException {
        MCRUserInformation userInfo = MCRSessionMgr.getCurrentSession().getUserInformation();
        realms = (Element) (realms.clone());
        realms.setAttribute("user", userInfo.getUserID());
        realms.setAttribute("realm", (userInfo instanceof MCRUser) ? ((MCRUser) userInfo).getRealm().getLabel() : MCRRealmFactory.getLocalRealm().getLabel());
        realms.setAttribute("guest", String.valueOf(currentUserIsGuest()));
        getLayoutService().doLayout(req, res, new Document(realms));
    }

    private static boolean currentUserIsGuest() {
        return MCRSessionMgr.getCurrentSession().getUserInformation().equals(MCRSystemUserInformation.getGuestInstance());
    }

    private int getNumLoginOptions() {
        int numOptions = realms.getChildren("realm").size();
        if (!"false".equals(realms.getAttributeValue("createAccount")))
            numOptions++;
        return numOptions;
    }

    private void loginToRealm(HttpServletRequest req, HttpServletResponse res, String realmID) throws Exception {
        storeURL(req.getParameter(LOGIN_REDIRECT_URL_PARAMETER));
        MCRRealm realm = MCRRealmFactory.getRealm(realmID);
        String loginURL = realm.getLoginURL();
        res.sendRedirect(loginURL);
    }

    /**
     * Stores the given url in MCRSession. When login is canceled, or after
     * successful login, the browser is redirected to that url. 
     */
    private void storeURL(String url) throws Exception {
        if ((url == null) || (url.trim().length() == 0))
            url = MCRServlet.getBaseURL();
        else if (url.startsWith(getBaseURL()) && !url.equals(getBaseURL())) {
            String rest = url.substring(getBaseURL().length());
            url = getBaseURL() + encodePath(rest);
        }
        LOGGER.info("Storing redirect URL to session: " + url);
        MCRSessionMgr.getCurrentSession().put(LOGIN_REDIRECT_URL_KEY, url);
    }

    private String encodePath(String path) throws Exception {
        path = path.replace('\\', '/');

        StringBuffer result = new StringBuffer();
        StringTokenizer st = new StringTokenizer(path, " /?&=", true);

        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.equals(" "))
                result.append("%20");
            else if (token.equals("/") || token.equals("?") || token.equals("&") || token.equals("="))
                result.append(token);
            else
                result.append(java.net.URLEncoder.encode(token, "UTF-8"));
        }

        return result.toString();
    }

    /**
     * Redirects the browser to the target url.
     */
    static void redirect(HttpServletResponse res) throws Exception {
        String url = (String) (MCRSessionMgr.getCurrentSession().get(LOGIN_REDIRECT_URL_KEY));
        if (url == null) {
            LOGGER.warn("Could not get redirect URL from session.");
            url = getBaseURL();
        }
        LOGGER.info("Redirecting to url: " + url);
        res.sendRedirect(res.encodeRedirectURL(url));
    }

    /**
     * Invoked by local editor login form login.xml to check for a valid
     * username and password entered.
     */
    public static boolean checkPassword(String userName, String password) {
        // Check for required fields is done in the login form itself, not here
        if ((userName == null) || (password == null))
            return true;

        if (userName.equalsIgnoreCase(MCRSystemUserInformation.getGuestInstance().getUserID()))
            return false; // Do not allow guest user login
        if (!MCRUserManager.exists(userName))
            return false;

        MCRUser user = MCRUserManager.getUser(userName);
        if (!user.getRealm().equals(MCRRealmFactory.getLocalRealm()))
            return false; // Do not login remote users, only local realm
        return MCRUserManager.checkPassword(userName, password) != null;
    }
}
