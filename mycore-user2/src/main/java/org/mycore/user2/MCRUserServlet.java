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

package org.mycore.user2;

import static org.mycore.user2.utils.MCRUserTransformer.JAXB_CONTEXT;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRJAXBContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.services.i18n.MCRTranslation;
import org.mycore.user2.utils.MCRUserTransformer;

/**
 * Provides functionality to search for users, list users, 
 * retrieve, delete or update user data. 
 * 
 * @author Frank L\u00fctzenkirchen
 * @author Thomas Scheffler (yagee)
 */
public class MCRUserServlet extends MCRServlet {
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    private static final long serialVersionUID = 1L;

    /** The logger */
    private static final Logger LOGGER = LogManager.getLogger(MCRUserServlet.class);

    /**
     * Handles requests. The parameter 'action' selects what to do, possible
     * values are show, save, delete, password (with id as second parameter). 
     * The default is to search and list users. 
     */
    public void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest req = job.getRequest();
        HttpServletResponse res = job.getResponse();
        if (forbidIfGuest(res)) {
            return;
        }
        String action = req.getParameter("action");
        String uid = req.getParameter("id");
        MCRUser user;

        if ((uid == null) || (uid.trim().length() == 0)) {
            user = MCRUserManager.getCurrentUser();
            uid = user != null ? String.valueOf(user.getUserID()) : null;
            if (!(user instanceof MCRTransientUser)) {
                //even reload current user, so that owner is correctly initialized
                user = MCRUserManager.getUser(uid);
            }
        } else {
            user = MCRUserManager.getUser(uid);
        }

        if ("show".equals(action))
            showUser(req, res, user, uid);
        else if ("save".equals(action))
            saveUser(req, res);
        else if ("saveCurrentUser".equals(action))
            saveCurrentUser(req, res);
        else if ("changeMyPassword".equals(action))
            redirectToPasswordChangePage(req, res);
        else if ("password".equals(action))
            changePassword(req, res, user, uid);
        else if ("delete".equals(action))
            deleteUser(req, res, user);
        else
            listUsers(req, res);
    }

    private void redirectToPasswordChangePage(HttpServletRequest req, HttpServletResponse res) throws Exception {
        MCRUser currentUser = MCRUserManager.getCurrentUser();
        if (!checkUserIsNotNull(res, currentUser, null)) {
            return;
        }
        if (checkUserIsLocked(res, currentUser) || checkUserIsDisabled(res, currentUser)) {
            return;
        }
        String url = currentUser.getRealm().getPasswordChangeURL();
        if (url == null) {
            String msg = MCRTranslation.translate("component.user2.UserServlet.missingRealPasswortChangeURL",
                currentUser.getRealmID());
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
        } else {
            res.sendRedirect(url);
        }
    }

    private static boolean checkUserIsNotNull(HttpServletResponse res, MCRUser currentUser, String userID)
        throws IOException {
        if (currentUser == null) {
            String uid = userID == null ? MCRSessionMgr.getCurrentSession().getUserInformation().getUserID() : userID;
            String msg = MCRTranslation.translate("component.user2.UserServlet.currentUserUnknown", uid);
            res.sendError(HttpServletResponse.SC_FORBIDDEN, msg);
            return false;
        }
        return true;
    }

    private static boolean checkUserIsLocked(HttpServletResponse res, MCRUser currentUser) throws IOException {
        if (currentUser.isLocked()) {
            String userName = currentUser.getUserID();
            String msg = MCRTranslation.translate("component.user2.UserServlet.isLocked", userName);
            res.sendError(HttpServletResponse.SC_FORBIDDEN, msg);
            return true;
        }
        return false;
    }

    private static boolean checkUserIsDisabled(HttpServletResponse res, MCRUser currentUser) throws IOException {
        if (currentUser.isDisabled()) {
            String userName = currentUser.getUserID();
            String msg = MCRTranslation.translate("component.user2.UserServlet.isDisabled", userName);
            res.sendError(HttpServletResponse.SC_FORBIDDEN, msg);
            return true;
        }
        return false;
    }

    private static boolean forbidIfGuest(HttpServletResponse res) throws IOException {
        if (MCRSessionMgr.getCurrentSession().getUserInformation()
            .equals(MCRSystemUserInformation.getGuestInstance())) {
            String msg = MCRTranslation.translate("component.user2.UserServlet.noGuestAction");
            res.sendError(HttpServletResponse.SC_FORBIDDEN, msg);
            return true;
        }
        return false;
    }

    /**
     * Handles MCRUserServlet?action=show&id={userID}.
     * Outputs user data for the given id using user.xsl.
     */
    private void showUser(HttpServletRequest req, HttpServletResponse res, MCRUser user, String uid) throws Exception {
        MCRUser currentUser = MCRUserManager.getCurrentUser();
        if (!checkUserIsNotNull(res, currentUser, null) || !checkUserIsNotNull(res, user, uid)) {
            return;
        }
        boolean allowed = MCRAccessManager.checkPermission(MCRUser2Constants.USER_ADMIN_PERMISSION)
            || currentUser.equals(user) || currentUser.equals(user.getOwner());
        if (!allowed) {
            String msg = MCRTranslation.translate("component.user2.UserServlet.noAdminPermission");
            res.sendError(HttpServletResponse.SC_FORBIDDEN, msg);
            return;
        }

        LOGGER.info("show user {} {} {}", user.getUserID(), user.getUserName(), user.getRealmID());
        getLayoutService().doLayout(req, res, getContent(user));
    }

    /**
     * Invoked by editor form user-editor.xed to check for a valid
     * login user name.
     */
    public static boolean checkUserName(String userName) {
        String realmID = MCRRealmFactory.getLocalRealm().getID();

        // Check for required fields is done in the editor form itself, not here
        if ((userName == null) || (realmID == null)) {
            return true;
        }

        // In all other cases, combination of userName and realm must not exist
        return !MCRUserManager.exists(userName, realmID);
    }

    private void saveCurrentUser(HttpServletRequest req, HttpServletResponse res) throws IOException {
        MCRUser currentUser = MCRUserManager.getCurrentUser();
        if (!checkUserIsNotNull(res, currentUser, null)) {
            return;
        }
        if (checkUserIsLocked(res, currentUser) || checkUserIsDisabled(res, currentUser)) {
            return;
        }
        if (!currentUser.hasNoOwner() && currentUser.isLocked()) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Document doc = (Document) (req.getAttribute("MCRXEditorSubmission"));
        Element u = doc.getRootElement();
        updateBasicUserInfo(u, currentUser);
        MCRUserManager.updateUser(currentUser);

        res.sendRedirect(res.encodeRedirectURL("MCRUserServlet?action=show"));
    }

    /**
     * Handles MCRUserServlet?action=save&id={userID}.
     * This is called by user-editor.xml editor form to save the
     * changed user data from editor submission. Redirects to
     * show user data afterwards. 
     */
    private void saveUser(HttpServletRequest req, HttpServletResponse res) throws Exception {
        MCRUser currentUser = MCRUserManager.getCurrentUser();
        if (!checkUserIsNotNull(res, currentUser, null)) {
            return;
        }
        boolean hasAdminPermission = MCRAccessManager.checkPermission(MCRUser2Constants.USER_ADMIN_PERMISSION);
        boolean allowed = hasAdminPermission
            || MCRAccessManager.checkPermission(MCRUser2Constants.USER_CREATE_PERMISSION);
        if (!allowed) {
            String msg = MCRTranslation.translate("component.user2.UserServlet.noCreatePermission");
            res.sendError(HttpServletResponse.SC_FORBIDDEN, msg);
            return;
        }

        Document doc = (Document) (req.getAttribute("MCRXEditorSubmission"));
        Element u = doc.getRootElement();
        String userName = u.getAttributeValue("name");

        String realmID = MCRRealmFactory.getLocalRealm().getID();
        if (hasAdminPermission) {
            realmID = u.getAttributeValue("realm");
        }

        MCRUser user;
        boolean userExists = MCRUserManager.exists(userName, realmID);
        if (!userExists) {
            user = new MCRUser(userName, realmID);
            LOGGER.info("create new user {} {}", userName, realmID);

            // For new local users, set password
            String pwd = u.getChildText("password");
            if ((pwd != null) && (pwd.trim().length() > 0) && user.getRealm().equals(MCRRealmFactory.getLocalRealm())) {
                MCRUserManager.updatePasswordHashToSHA256(user, pwd);
            }
        } else {
            user = MCRUserManager.getUser(userName, realmID);
            if (!(hasAdminPermission || currentUser.equals(user) || currentUser.equals(user.getOwner()))) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

        XPathExpression<Attribute> hintPath = XPathFactory.instance().compile("password/@hint", Filters.attribute());
        Attribute hintAttr = hintPath.evaluateFirst(u);
        String hint = hintAttr == null ? null : hintAttr.getValue();
        if ((hint != null) && (hint.trim().length() == 0)) {
            hint = null;
        }
        user.setHint(hint);

        updateBasicUserInfo(u, user);

        if (hasAdminPermission) {
            boolean locked = "true".equals(u.getAttributeValue("locked"));
            user.setLocked(locked);

            boolean disabled = "true".equals(u.getAttributeValue("disabled"));
            user.setDisabled(disabled);

            Element o = u.getChild("owner");
            if (o != null && !o.getAttributes().isEmpty()) {
                String ownerName = o.getAttributeValue("name");
                String ownerRealm = o.getAttributeValue("realm");
                MCRUser owner = MCRUserManager.getUser(ownerName, ownerRealm);
                if (!checkUserIsNotNull(res, owner, ownerName + "@" + ownerRealm)) {
                    return;
                }
                user.setOwner(owner);
            } else {
                user.setOwner(null);
            }
            String validUntilText = u.getChildTextTrim("validUntil");
            if (validUntilText == null || validUntilText.length() == 0) {
                user.setValidUntil(null);
            } else {

                String dateInUTC = validUntilText;
                if (validUntilText.length() == 10) {
                    dateInUTC = convertToUTC(validUntilText, "yyyy-MM-dd");
                }

                MCRISO8601Date date = new MCRISO8601Date(dateInUTC);
                user.setValidUntil(date.getDate());
            }
        } else { // save read user of creator
            user.setRealm(MCRRealmFactory.getLocalRealm());
            user.setOwner(currentUser);
        }
        Element gs = u.getChild("roles");
        if (gs != null) {
            user.getSystemRoleIDs().clear();
            user.getExternalRoleIDs().clear();
            List<Element> groupList = gs.getChildren("role");
            for (Element group : groupList) {
                String groupName = group.getAttributeValue("name");
                if (hasAdminPermission || currentUser.isUserInRole(groupName)) {
                    user.assignRole(groupName);
                } else {
                    LOGGER.warn("Current user {} has not the permission to add user to group {}",
                        currentUser.getUserID(), groupName);
                }
            }
        }

        if (userExists) {
            MCRUserManager.updateUser(user);
        } else {
            MCRUserManager.createUser(user);
        }

        res.sendRedirect(res.encodeRedirectURL("MCRUserServlet?action=show&id="
            + URLEncoder.encode(user.getUserID(), "UTF-8")));
    }

    private String convertToUTC(String validUntilText, String format) throws ParseException {
        DateFormat inputFormat = new SimpleDateFormat(format, Locale.ROOT);
        inputFormat.setTimeZone(UTC_TIME_ZONE);
        DateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.ROOT);

        Date d = inputFormat.parse(validUntilText);
        outputFormat.setTimeZone(UTC_TIME_ZONE);
        return outputFormat.format(d);
    }

    private void updateBasicUserInfo(Element u, MCRUser user) {
        String name = u.getChildText("realName");
        if ((name != null) && (name.trim().length() == 0)) {
            name = null;
        }
        user.setRealName(name);

        String eMail = u.getChildText("eMail");
        if ((eMail != null) && (eMail.trim().length() == 0)) {
            eMail = null;
        }
        user.setEMail(eMail);

        Element attributes = u.getChild("attributes");
        if (attributes != null) {
            List<Element> attributeList = attributes.getChildren("attribute");
            user.getAttributes().clear();
            for (Element attribute : attributeList) {
                String key = attribute.getAttributeValue("name");
                String value = attribute.getAttributeValue("value");
                user.getAttributes().put(key, value);
            }
        }
    }

    /**
     * Handles MCRUserServlet?action=save&id={userID}.
     * This is called by user-editor.xml editor form to save the
     * changed user data from editor submission. Redirects to
     * show user data afterwards. 
     */
    private void changePassword(HttpServletRequest req, HttpServletResponse res, MCRUser user, String uid)
        throws Exception {
        MCRUser currentUser = MCRUserManager.getCurrentUser();
        if (!checkUserIsNotNull(res, currentUser, null) || !checkUserIsNotNull(res, user, uid)) {
            return;
        }
        boolean allowed = MCRAccessManager.checkPermission(MCRUser2Constants.USER_ADMIN_PERMISSION)
            || currentUser.equals(user.getOwner())
            || (currentUser.equals(user) && currentUser.hasNoOwner() || !currentUser.isLocked());
        if (!allowed) {
            String msg = MCRTranslation.translate("component.user2.UserServlet.noAdminPermission");
            res.sendError(HttpServletResponse.SC_FORBIDDEN, msg);
            return;
        }

        LOGGER.info("change password of user {} {} {}", user.getUserID(), user.getUserName(), user.getRealmID());

        Document doc = (Document) (req.getAttribute("MCRXEditorSubmission"));
        String password = doc.getRootElement().getChildText("password");
        MCRUserManager.setPassword(user, password);

        res.sendRedirect(res.encodeRedirectURL("MCRUserServlet?action=show&XSL.step=changedPassword&id="
            + URLEncoder.encode(user.getUserID(), "UTF-8")));
    }

    /**
     * Handles MCRUserServlet?action=delete&id={userID}.
     * Deletes the user. 
     * Outputs user data of the deleted user using user.xsl afterwards.
     */
    private void deleteUser(HttpServletRequest req, HttpServletResponse res, MCRUser user) throws Exception {
        MCRUser currentUser = MCRUserManager.getCurrentUser();
        boolean allowed = MCRAccessManager.checkPermission(MCRUser2Constants.USER_ADMIN_PERMISSION)
            || currentUser.equals(user.getOwner());
        if (!allowed) {
            String msg = MCRTranslation.translate("component.user2.UserServlet.noAdminPermission");
            res.sendError(HttpServletResponse.SC_FORBIDDEN, msg);
            return;
        }

        LOGGER.info("delete user {} {} {}", user.getUserID(), user.getUserName(), user.getRealmID());
        MCRUserManager.deleteUser(user);
        getLayoutService().doLayout(req, res, getContent(user));
    }

    private MCRJAXBContent<MCRUser> getContent(MCRUser user) {
        return new MCRJAXBContent<>(JAXB_CONTEXT, user.getSafeCopy());
    }

    /**
     * Handles MCRUserServlet?search={pattern}, which is an optional parameter.
     * Searches for users matching the pattern in user name or real name and outputs
     * the list of results using users.xsl. The search pattern may contain * and ?
     * wildcard characters. The property MCR.user2.Users.MaxResults (default 100) specifies
     * the maximum number of users to return. When there are more hits, just the
     * number of results is returned.
     * 
     * When current user is not admin, the search pattern will be ignored and only all 
     * the users the current user is owner of will be listed.
     */
    private void listUsers(HttpServletRequest req, HttpServletResponse res) throws Exception {
        MCRUser currentUser = MCRUserManager.getCurrentUser();
        List<MCRUser> ownUsers = MCRUserManager.listUsers(currentUser);
        boolean hasAdminPermission = MCRAccessManager.checkPermission(MCRUser2Constants.USER_ADMIN_PERMISSION);
        boolean allowed = hasAdminPermission
            || MCRAccessManager.checkPermission(MCRUser2Constants.USER_CREATE_PERMISSION) || !ownUsers.isEmpty();
        if (!allowed) {
            String msg = MCRTranslation.translate("component.user2.UserServlet.noCreatePermission");
            res.sendError(HttpServletResponse.SC_FORBIDDEN, msg);
            return;
        }

        Element users = new Element("users");

        List<MCRUser> results = null;
        if (hasAdminPermission) {
            String search = req.getParameter("search");
            if ((search == null) || search.trim().length() == 0)
                search = null;

            if (search != null) {
                users.setAttribute("search", search);
                search = "*" + search + "*";
            }

            LOGGER.info("search users like {}", search);

            int max = MCRConfiguration.instance().getInt(MCRUser2Constants.CONFIG_PREFIX + "Users.MaxResults", 100);
            int num = MCRUserManager.countUsers(search, null, search);

            if ((num < max) && (num > 0))
                results = MCRUserManager.listUsers(search, null, search);
            users.setAttribute("num", String.valueOf(num));
            users.setAttribute("max", String.valueOf(max));
        } else {
            LOGGER.info("list owned users of {} {}", currentUser.getUserName(), currentUser.getRealmID());
            results = ownUsers;
        }

        if (results != null)
            for (MCRUser user : results) {
                Element u = MCRUserTransformer.buildBasicXML(user).detachRootElement();
                addString(u, "realName", user.getRealName());
                addString(u, "eMail", user.getEMailAddress());
                users.addContent(u);
            }

        getLayoutService().doLayout(req, res, new MCRJDOMContent(users));
    }

    private void addString(Element parent, String name, String value) {
        if ((value != null) && (value.trim().length() > 0))
            parent.addContent(new Element(name).setText(value.trim()));
    }
}
