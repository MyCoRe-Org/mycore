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

package org.mycore.user2;

import static org.mycore.user2.utils.MCRUserTransformer.JAXB_CONTEXT;

import java.io.IOException;
import java.io.Serial;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRJAXBContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.services.i18n.MCRTranslation;
import org.mycore.user2.utils.MCRUserTransformer;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Provides functionality to search for users, list users,
 * retrieve, delete or update user data.
 *
 * @author Frank L\u00fctzenkirchen
 * @author Thomas Scheffler (yagee)
 */
public class MCRUserServlet extends MCRServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String ATTRIBUTE_NAME = "name";

    private static final String ATTRIBUTE_REALM = "realm";

    private static final String ATTRIBUTE_VALUE = "value";

    /**
     * Handles requests. The parameter 'action' selects what to do, possible
     * values are show, save, delete, password (with id as second parameter).
     * The default is to search and list users.
     */
    @Override
    public void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest req = job.getRequest();
        HttpServletResponse res = job.getResponse();
        if (forbidIfGuest(res)) {
            return;
        }
        String action = Optional.ofNullable(req.getParameter("action")).orElse("listUsers");
        String uid = req.getParameter("id");
        MCRUser user;

        if (uid == null || uid.isBlank()) {
            user = MCRUserManager.getCurrentUser();
            uid = user != null ? String.valueOf(user.getUserID()) : null;
            if (!(user instanceof MCRTransientUser)) {
                //even reload current user, so that owner is correctly initialized
                user = MCRUserManager.getUser(uid);
            }
        } else {
            user = MCRUserManager.getUser(uid);
        }

        switch (action) {
            case "show" -> showUser(req, res, user, uid);
            case "save" -> saveUser(req, res);
            case "saveCurrentUser" -> saveCurrentUser(req, res);
            case "changeMyPassword" -> redirectToPasswordChangePage(res);
            case "password" -> changePassword(req, res, user, uid);
            case "delete" -> deleteUser(req, res, user);
            case "listUsers" -> listUsers(req, res);
            default -> throw new ServletException("unknown action: " + action);
        }
    }

    private void redirectToPasswordChangePage(HttpServletResponse res) throws Exception {
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
        if (MCRSessionMgr.getCurrentSession().getUserInformation().getUserID()
            .equals(MCRSystemUserInformation.GUEST.getUserID())) {
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

        LOGGER.info("show user {} {} {}", user::getUserID, user::getUserName, user::getRealmID);
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

        Document doc = (Document) req.getAttribute("MCRXEditorSubmission");
        Element u = doc.getRootElement();
        String userName = u.getAttributeValue(ATTRIBUTE_NAME);

        String realmID = MCRRealmFactory.getLocalRealm().getID();
        if (hasAdminPermission) {
            realmID = u.getAttributeValue(ATTRIBUTE_REALM);
        }

        MCRUser user = getUserIfAllowed(userName, realmID,
            u.getChildText("password"),
            testUser -> hasAdminPermission || currentUser.equals(testUser) || currentUser.equals(testUser.getOwner()),
            res);
        if (user == null) {
            return;
        }

        user.setHint(getPasswordHint(u));

        updateBasicUserInfo(u, user);

        if (hasAdminPermission) {
            if (!applyAdminUpdates(user, u, res)) {
                return;
            }
        } else { // save read user of creator
            user.setRealm(MCRRealmFactory.getLocalRealm());
            user.setOwner(currentUser);
        }

        assignRolesToUser(getRoles(u), user,
            role -> hasAdminPermission || currentUser.isUserInRole(role),
            role -> LOGGER.warn("Current user {} has not the permission to add user to group {}",
                currentUser.getUserID(), role));

        MCRUserManager.updateUser(user); //also creates user

        res.sendRedirect(res.encodeRedirectURL("MCRUserServlet?action=show&id="
            + URLEncoder.encode(user.getUserID(), StandardCharsets.UTF_8)));
    }

    private static MCRUser getUserIfAllowed(String userName, String realmID, String pwd, Predicate<MCRUser> allowUpdate,
        HttpServletResponse res) throws IOException {
        MCRUser user;
        boolean userExists = MCRUserManager.exists(userName, realmID);
        if (!userExists) {
            user = createUser(userName, realmID, pwd);
        } else {
            user = MCRUserManager.getUser(userName, realmID);
            if (!allowUpdate.test(user)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return null;
            }
        }
        return user;
    }

    private static String getPasswordHint(Element u) {
        XPathExpression<Attribute> hintPath = XPathFactory.instance().compile("password/@hint", Filters.attribute());
        Attribute hintAttr = hintPath.evaluateFirst(u);
        String hint = hintAttr == null ? null : hintAttr.getValue();
        if ((hint != null) && (hint.isBlank())) {
            hint = null;
        }
        return hint;
    }

    private boolean applyAdminUpdates(MCRUser user, Element u, HttpServletResponse res)
        throws IOException, ParseException {
        boolean locked = "true".equals(u.getAttributeValue("locked"));
        user.setLocked(locked);

        boolean disabled = "true".equals(u.getAttributeValue("disabled"));
        user.setDisabled(disabled);

        Element o = u.getChild("owner");
        if (o != null && !o.getAttributes().isEmpty()) {
            String ownerName = o.getAttributeValue(ATTRIBUTE_NAME);
            String ownerRealm = o.getAttributeValue(ATTRIBUTE_REALM);
            MCRUser owner = MCRUserManager.getUser(ownerName, ownerRealm);
            if (!checkUserIsNotNull(res, owner, ownerName + "@" + ownerRealm)) {
                return false;
            }
            user.setOwner(owner);
        } else {
            user.setOwner(null);
        }
        String validUntilText = u.getChildTextTrim("validUntil");
        if (validUntilText == null || validUntilText.isEmpty()) {
            user.setValidUntil(null);
        } else {
            String dateInUTC = validUntilText;
            if (validUntilText.length() == 10) {
                dateInUTC = convertToUTC(validUntilText, "yyyy-MM-dd");
            }

            MCRISO8601Date date = new MCRISO8601Date(dateInUTC);
            user.setValidUntil(date.getDate());
        }
        return true;
    }

    private static void assignRolesToUser(List<String> roles, MCRUser user, Predicate<String> permission,
        Consumer<String> failedAssignment) {
        if (roles == null) {
            return;
        }
        user.getSystemRoleIDs().clear();
        user.getExternalRoleIDs().clear();
        for (String role : roles) {
            if (permission.test(role)) {
                user.assignRole(role);
            } else {
                failedAssignment.accept(role);
            }
        }
    }

    private List<String> getRoles(Element user) {
        Element gs = user.getChild("roles");
        if (gs == null) {
            return null;
        }
        return gs.getChildren("role").stream()
            .map(group -> group.getAttributeValue(ATTRIBUTE_NAME))
            .filter(Objects::nonNull)
            .toList();
    }

    private static MCRUser createUser(String userName, String realmID, String pwd) {
        MCRUser user;
        user = new MCRUser(userName, realmID);
        LOGGER.info("create new user {} {}", userName, realmID);

        // For new local users, set password
        if ((pwd != null) && !pwd.isBlank() && user.getRealm().equals(MCRRealmFactory.getLocalRealm())) {
            MCRUserManager.setUserPassword(user, pwd);
        }
        return user;
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
        if (name != null && name.isBlank()) {
            name = null;
        }
        user.setRealName(name);

        String eMail = u.getChildText("eMail");
        if (eMail != null && eMail.isBlank()) {
            eMail = null;
        }
        user.setEMail(eMail);

        List<Element> attributeList = Optional.ofNullable(u.getChild("attributes"))
            .map(attributes -> attributes.getChildren("attribute"))
            .orElse(Collections.emptyList());
        Set<MCRUserAttribute> newAttrs = attributeList.stream()
            .map(a -> new MCRUserAttribute(
                a.getAttributeValue(ATTRIBUTE_NAME),
                a.getAttributeValue(ATTRIBUTE_VALUE)))
            .collect(Collectors.toSet());
        user.getAttributes().retainAll(newAttrs);
        newAttrs.removeAll(user.getAttributes());
        user.getAttributes().addAll(newAttrs);
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
            || currentUser.equals(user) && !currentUser.isLocked();
        if (!allowed) {
            String msg = MCRTranslation.translate("component.user2.UserServlet.noAdminPermission");
            res.sendError(HttpServletResponse.SC_FORBIDDEN, msg);
            return;
        }

        LOGGER.info("change password of user {} {} {}", user::getUserID, user::getUserName, user::getRealmID);

        Document doc = (Document) (req.getAttribute("MCRXEditorSubmission"));
        String password = doc.getRootElement().getChildText("password");
        MCRUserManager.setPassword(user, password);

        res.sendRedirect(res.encodeRedirectURL("MCRUserServlet?action=show&XSL.step=changedPassword&id="
            + URLEncoder.encode(user.getUserID(), StandardCharsets.UTF_8)));
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

        LOGGER.info("delete user {} {} {}", user::getUserID, user::getUserName, user::getRealmID);
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
     * <p>
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
            String pattern;

            if (search == null || search.isBlank()) {
                pattern = null;
            } else {
                users.setAttribute("search", search);
                pattern = "*" + search + "*";
            }

            LOGGER.info("search users like {}", search);

            int max = MCRConfiguration2.getInt(MCRUser2Constants.CONFIG_PREFIX + "Users.MaxResults").orElse(100);
            int num = MCRUserManager.countUsers(pattern, null, pattern, pattern, null, pattern);

            if ((num < max) && (num > 0)) {
                results = MCRUserManager.listUsers(pattern, null, pattern, pattern, null,
                    pattern, 0, Integer.MAX_VALUE);
            }
            users.setAttribute("num", String.valueOf(num));
            users.setAttribute("max", String.valueOf(max));
        } else {
            LOGGER.info("list owned users of {} {}", currentUser::getUserName, currentUser::getRealmID);
            results = ownUsers;
        }

        if (results != null) {
            for (MCRUser user : results) {
                Element u = MCRUserTransformer.buildBasicXML(user).detachRootElement();
                addString(u, "realName", user.getRealName());
                addString(u, "eMail", user.getEMail());
                users.addContent(u);
            }
        }

        getLayoutService().doLayout(req, res, new MCRJDOMContent(users));
    }

    private void addString(Element parent, String name, String value) {
        if (value != null && !value.isBlank()) {
            parent.addContent(new Element(name).setText(value.trim()));
        }
    }
}
