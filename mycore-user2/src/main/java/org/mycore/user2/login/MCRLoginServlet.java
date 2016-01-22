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
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRJAXBContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.frontend.support.MCRLogin.InputField;
import org.mycore.services.i18n.MCRTranslation;
import org.mycore.user2.MCRRealm;
import org.mycore.user2.MCRRealmFactory;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUser2Constants;
import org.mycore.user2.MCRUserManager;
import org.xml.sax.SAXException;

/**
 * Provides functionality to select login method,
 * change login user and show a welcome page.
 * Login methods and realms are configured in realms.xml.
 * The login form for local users is login.xml. 
 * 
 * @author Frank L\u00fctzenkirchen
 * @author Thomas Scheffler (yagee)
 */
public class MCRLoginServlet extends MCRServlet {
    private static final String REALM_URL_PARAMETER = "realm";

    static final String HTTPS_ONLY_PROPERTY = MCRUser2Constants.CONFIG_PREFIX + "LoginHttpsOnly";

    private static final long serialVersionUID = 1L;

    private static final String LOGIN_REDIRECT_URL_PARAMETER = "url";

    private static final String LOGIN_REDIRECT_URL_KEY = "loginRedirectURL";

    static final boolean LOCAL_LOGIN_SECURE_ONLY = MCRConfiguration.instance().getBoolean(HTTPS_ONLY_PROPERTY);

    private static Logger LOGGER = LogManager.getLogger();

    @Override
    public void init() throws ServletException {
        if (!LOCAL_LOGIN_SECURE_ONLY) {
            LOGGER.warn("Login over unsecure connection is permitted. Set '" + HTTPS_ONLY_PROPERTY
                + "=true' to prevent cleartext transmissions of passwords.");
        }
        super.init();
    }

    /**
     * MCRLoginServlet handles four actions:
     * 
     * MCRLoginServlet?url=foo
     * stores foo as redirect url and displays
     * a list of login method options.
     * 
     * MCRLoginServlet?url=foo&amp;realm=ID
     * stores foo as redirect url and redirects
     * to the login URL of the given realm.
    
     * MCRLoginServlet?action=login
     * checks input from editor login form and
     * changes the current login user and redirects
     * to the stored url.
     * 
     * MCRLoginServlet?action=cancel
     * does not change login user, just
     * redirects to the target url
     */
    public void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest req = job.getRequest();
        HttpServletResponse res = job.getResponse();

        String action = req.getParameter("action");
        String realm = req.getParameter(REALM_URL_PARAMETER);
        job.getResponse().setHeader("Cache-Control", "no-cache");
        job.getResponse().setHeader("Pragma", "no-cache");
        job.getResponse().setHeader("Expires", "0");

        if ("login".equals(action)) {
            presentLoginForm(job);
        } else if ("cancel".equals(action)) {
            redirect(res);
        } else if (realm != null) {
            loginToRealm(req, res, req.getParameter(REALM_URL_PARAMETER));
        } else {
            chooseLoginMethod(req, res);
        }
    }

    /**
     * Stores the target url and outputs a list of realms to login to. The list is
     * rendered using realms.xsl.
     */
    private void chooseLoginMethod(HttpServletRequest req, HttpServletResponse res) throws Exception {
        storeURL(getReturnURL(req));
        // redirect directly to login url if there is only one realm available and the user is not logged in
        if ((getNumLoginOptions() == 1) && currentUserIsGuest())
            redirectToUniqueRealm(req, res);
        else
            listRealms(req, res);
    }

    static String getReturnURL(HttpServletRequest req) {
        String returnURL = req.getParameter(LOGIN_REDIRECT_URL_PARAMETER);
        if (returnURL == null) {
            String referer = req.getHeader("Referer");
            returnURL = (referer != null) ? referer : req.getContextPath() + "/";
        }
        return returnURL;
    }

    private void redirectToUniqueRealm(HttpServletRequest req, HttpServletResponse res) throws Exception {
        String realmID = MCRRealmFactory.listRealms().iterator().next().getID();
        loginToRealm(req, res, realmID);
    }

    private void presentLoginForm(MCRServletJob job)
        throws IOException, TransformerException, SAXException, JAXBException {
        HttpServletRequest req = job.getRequest();
        HttpServletResponse res = job.getResponse();
        if (LOCAL_LOGIN_SECURE_ONLY && !req.isSecure()) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, getErrorI18N("component.user2.login", "httpsOnly"));
            return;
        }

        String returnURL = getReturnURL(req);
        String formAction = req.getRequestURI();
        MCRLogin loginForm = new MCRLogin(MCRSessionMgr.getCurrentSession().getUserInformation(), returnURL,
            formAction);
        String uid = getProperty(req, "uid");
        String pwd = getProperty(req, "pwd");
        if (uid != null) {
            MCRUser user = MCRUserManager.login(uid, pwd);
            if (user == null) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                loginForm.setLoginFailed(true);
            } else {
                //user logged in
                // MCR-1154
                req.changeSessionId();
                LOGGER.info("user " + uid + " logged in successfully.");
                res.sendRedirect(res.encodeRedirectURL(getReturnURL(req)));
                return;
            }
        }
        addFormFields(loginForm, job.getRequest().getParameter(REALM_URL_PARAMETER));
        getLayoutService().doLayout(req, res, new MCRJAXBContent<>(JAXBContext.newInstance(MCRLogin.class), loginForm));
    }

    private void listRealms(HttpServletRequest req, HttpServletResponse res)
        throws IOException, TransformerException, SAXException {
        String redirectURL = getReturnURL(req);
        Document realmsDoc = MCRRealmFactory.getRealmsDocument();
        Element realms = realmsDoc.getRootElement();
        addCurrentUserInfo(realms);
        List<Element> realmList = realms.getChildren(REALM_URL_PARAMETER);
        for (Element realm : realmList) {
            String realmID = realm.getAttributeValue("id");
            Element login = realm.getChild("login");
            if (login != null)
                login.setAttribute("url", MCRRealmFactory.getRealm(realmID).getLoginURL(redirectURL));
        }
        getLayoutService().doLayout(req, res, new MCRJDOMContent(realmsDoc));
    }

    protected static void addFormFields(MCRLogin login, String loginToRealm) {
        ArrayList<org.mycore.frontend.support.MCRLogin.InputField> fields = new ArrayList<>();
        if (loginToRealm != null) {
            //realmParameter
            MCRRealm realm = MCRRealmFactory.getRealm(loginToRealm);
            InputField realmParameter = new InputField(realm.getRealmParameter(), loginToRealm, null, null, false,
                true);
            fields.add(realmParameter);
        }
        fields.add(new InputField("action", "login", null, null, false, true));
        fields.add(new InputField("url", login.getReturnURL(), null, null, false, true));
        String userNameText = MCRTranslation.translate("component.user2.login.form.userName");
        fields.add(new InputField("uid", null, userNameText, userNameText, false, false));
        String pwdText = MCRTranslation.translate("component.user2.login.form.password");
        fields.add(new InputField("pwd", null, pwdText, pwdText, true, false));
        login.getForm().getInput().addAll(fields);
    }

    static void addCurrentUserInfo(Element rootElement) {
        MCRUserInformation userInfo = MCRSessionMgr.getCurrentSession().getUserInformation();
        rootElement.setAttribute("user", userInfo.getUserID());
        String realmId = (userInfo instanceof MCRUser) ? ((MCRUser) userInfo).getRealm().getLabel()
            : userInfo.getUserAttribute(MCRRealm.USER_INFORMATION_ATTR);
        if (realmId == null) {
            realmId = MCRRealmFactory.getLocalRealm().getLabel();
        }
        rootElement.setAttribute(REALM_URL_PARAMETER, realmId);
        rootElement.setAttribute("guest", String.valueOf(currentUserIsGuest()));
    }

    static void addCurrentUserInfo(MCRLogin login) {
        MCRUserInformation userInfo = MCRSessionMgr.getCurrentSession().getUserInformation();
        String realmId = (userInfo instanceof MCRUser) ? ((MCRUser) userInfo).getRealm().getLabel()
            : userInfo.getUserAttribute(MCRRealm.USER_INFORMATION_ATTR);
        if (realmId == null) {
            realmId = MCRRealmFactory.getLocalRealm().getLabel();
        }
        login.setRealm(realmId);
    }

    private static boolean currentUserIsGuest() {
        return MCRSessionMgr.getCurrentSession().getUserInformation()
            .equals(MCRSystemUserInformation.getGuestInstance());
    }

    private int getNumLoginOptions() {
        int numOptions = 0;
        for (MCRRealm realm : MCRRealmFactory.listRealms()) {
            numOptions++;
            if (realm.getCreateURL() != null) {
                numOptions++;
            }
        }
        return numOptions;
    }

    private void loginToRealm(HttpServletRequest req, HttpServletResponse res, String realmID) throws Exception {
        String redirectURL = getReturnURL(req);
        storeURL(redirectURL);
        MCRRealm realm = MCRRealmFactory.getRealm(realmID);
        String loginURL = realm.getLoginURL(redirectURL);
        res.sendRedirect(res.encodeRedirectURL(loginURL));
    }

    /**
     * Stores the given url in MCRSession. When login is canceled, or after
     * successful login, the browser is redirected to that url. 
     */
    private void storeURL(String url) throws Exception {
        if ((url == null) || (url.trim().length() == 0))
            url = MCRFrontendUtil.getBaseURL();
        else if (url.startsWith(MCRFrontendUtil.getBaseURL()) && !url.equals(MCRFrontendUtil.getBaseURL())) {
            String rest = url.substring(MCRFrontendUtil.getBaseURL().length());
            url = MCRFrontendUtil.getBaseURL() + encodePath(rest);
        }
        LOGGER.info("Storing redirect URL to session: " + url);
        MCRSessionMgr.getCurrentSession().put(LOGIN_REDIRECT_URL_KEY, url);
    }

    private String encodePath(String path) throws Exception {
        path = path.replace('\\', '/');

        StringBuilder result = new StringBuilder();
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
            url = MCRFrontendUtil.getBaseURL();
        }
        LOGGER.info("Redirecting to url: " + url);
        res.sendRedirect(res.encodeRedirectURL(url));
    }
}
