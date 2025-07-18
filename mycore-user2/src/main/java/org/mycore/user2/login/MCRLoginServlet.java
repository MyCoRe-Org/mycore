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

package org.mycore.user2.login;

import java.io.IOException;
import java.io.Serial;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.config.MCRConfiguration2;
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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

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

    @Serial
    private static final long serialVersionUID = 1L;

    protected static final String REALM_URL_PARAMETER = "realm";

    static final String HTTPS_ONLY_PROPERTY = MCRUser2Constants.CONFIG_PREFIX + "LoginHttpsOnly";
    protected static final boolean LOCAL_LOGIN_SECURE_ONLY = MCRConfiguration2
        .getOrThrow(HTTPS_ONLY_PROPERTY, Boolean::parseBoolean);
    static final String ALLOWED_ROLES_PROPERTY = MCRUser2Constants.CONFIG_PREFIX + "LoginAllowedRoles";
    private static final String LOGIN_REDIRECT_URL_PARAMETER = "url";
    private static final String LOGIN_REDIRECT_URL_KEY = "loginRedirectURL";
    private static final List<String> ALLOWED_ROLES = MCRConfiguration2
        .getString(ALLOWED_ROLES_PROPERTY)
        .map(MCRConfiguration2::splitValue)
        .map(s -> s.collect(Collectors.toList()))
        .orElse(Collections.emptyList());

    private static final Logger LOGGER = LogManager.getLogger();

    protected static String getReturnURL(HttpServletRequest req) {
        return Optional.ofNullable(req.getParameter(LOGIN_REDIRECT_URL_PARAMETER))
                .filter(MCRFrontendUtil::isSafeRedirect)
                .or(() -> Optional.ofNullable(req.getHeader("Referer"))
                        .filter(MCRFrontendUtil::isSafeRedirect))
                .orElse(req.getContextPath() + "/");
    }

    protected static void addFormFields(MCRLogin login, String loginToRealm) {
        List<org.mycore.frontend.support.MCRLogin.InputField> fields = new ArrayList<>();
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
        String realmId = userInfo instanceof MCRUser mcrUser ? mcrUser.getRealm().getLabel()
            : userInfo.getUserAttribute(MCRRealm.USER_INFORMATION_ATTR);
        if (realmId == null) {
            realmId = MCRRealmFactory.getLocalRealm().getLabel();
        }
        rootElement.setAttribute(REALM_URL_PARAMETER, realmId);
        rootElement.setAttribute("guest", String.valueOf(currentUserIsGuest()));
    }

    static void addCurrentUserInfo(MCRLogin login) {
        MCRUserInformation userInfo = MCRSessionMgr.getCurrentSession().getUserInformation();
        String realmId = userInfo instanceof MCRUser mcrUser ? mcrUser.getRealm().getLabel()
            : userInfo.getUserAttribute(MCRRealm.USER_INFORMATION_ATTR);
        if (realmId == null) {
            realmId = MCRRealmFactory.getLocalRealm().getLabel();
        }
        login.setRealm(realmId);
    }

    private static boolean currentUserIsGuest() {
        return MCRSessionMgr.getCurrentSession().getUserInformation().getUserID()
            .equals(MCRSystemUserInformation.GUEST.getUserID());
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
        LOGGER.info("Redirecting to url: {}", url);
        res.sendRedirect(res.encodeRedirectURL(url));
    }

    @Override
    public void init() throws ServletException {
        if (!LOCAL_LOGIN_SECURE_ONLY) {
            String logMsg = "Login over unsecure connection is permitted. "
                + "Set '{}=true' to prevent cleartext transmissions of passwords.";
            LOGGER.warn(logMsg, HTTPS_ONLY_PROPERTY);
        }
        super.init();
    }

    /**
     * MCRLoginServlet handles four actions:
     * <p>
     * MCRLoginServlet?url=foo
     * stores foo as redirect url and displays
     * a list of login method options.
     * <p>
     * MCRLoginServlet?url=foo&amp;realm=ID
     * stores foo as redirect url and redirects
     * to the login URL of the given realm.
    
     * MCRLoginServlet?action=login
     * checks input from editor login form and
     * changes the current login user and redirects
     * to the stored url.
     * <p>
     * MCRLoginServlet?action=cancel
     * does not change login user, just
     * redirects to the target url
     */
    @Override
    public void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest req = job.getRequest();
        HttpServletResponse res = job.getResponse();

        String action = req.getParameter("action");
        String realm = req.getParameter(REALM_URL_PARAMETER);
        job.getResponse().setHeader("Cache-Control", "no-cache");
        job.getResponse().setHeader("Pragma", "no-cache");
        job.getResponse().setHeader("Expires", "0");

        if (Objects.equals(action, "login")) {
            presentLoginForm(job);
        } else if (Objects.equals(action, "cancel")) {
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
        if ((getNumLoginOptions() == 1) && currentUserIsGuest()) {
            redirectToUniqueRealm(req, res);
        } else {
            listRealms(req, res);
        }
    }

    private void redirectToUniqueRealm(HttpServletRequest req, HttpServletResponse res) throws Exception {
        String realmID = MCRRealmFactory.listRealms().getFirst().getID();
        loginToRealm(req, res, realmID);
    }

    protected void presentLoginForm(MCRServletJob job)
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
            MCRUser user = MCRUserManager.login(uid, pwd, ALLOWED_ROLES);
            if (user == null) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                loginForm.setLoginFailed(true);
            } else {
                //user logged in
                // MCR-1154
                req.changeSessionId();
                LOGGER.info("user {} logged in successfully.", uid);
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
            if (login != null) {
                login.setAttribute("url", MCRRealmFactory.getRealm(realmID).getLoginURL(redirectURL));
            }
        }
        getLayoutService().doLayout(req, res, new MCRJDOMContent(realmsDoc));
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
    private void storeURL(String url) {
        String storedUrl = url;
        if (url == null || url.isBlank()) {
            storedUrl = MCRFrontendUtil.getBaseURL();
        } else if (url.startsWith(MCRFrontendUtil.getBaseURL()) && !url.equals(MCRFrontendUtil.getBaseURL())) {
            String rest = url.substring(MCRFrontendUtil.getBaseURL().length());
            storedUrl = MCRFrontendUtil.getBaseURL() + encodePath(rest);
        }
        LOGGER.info("Storing redirect URL to session: {}", storedUrl);
        MCRSessionMgr.getCurrentSession().put(LOGIN_REDIRECT_URL_KEY, storedUrl);
    }

    private String encodePath(String path) {
        StringBuilder result = new StringBuilder();
        StringTokenizer st = new StringTokenizer(path.replace('\\', '/'), " /?&=", true);

        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            switch (token) {
                case " " -> result.append("%20");
                case "/", "?", "&", "=" -> result.append(token);
                default -> result.append(URLEncoder.encode(token, StandardCharsets.UTF_8));
            }
        }

        return result.toString();
    }
}
