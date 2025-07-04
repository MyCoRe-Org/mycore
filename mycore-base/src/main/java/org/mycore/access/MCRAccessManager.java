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
package org.mycore.access;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.access.strategies.MCRAccessCheckStrategy;
import org.mycore.common.MCRException;
import org.mycore.common.MCRScopedSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * @author Thomas Scheffler
 */
public class MCRAccessManager {

    private static final MCRAccessCacheManager ACCESS_CACHE = new MCRAccessCacheManager();

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String PERMISSION_READ = "read";

    public static final String PERMISSION_WRITE = "writedb";

    public static final String PERMISSION_DELETE = "deletedb";

    public static final String PERMISSION_PREVIEW = "preview";

    public static final String PERMISSION_VIEW = "view";

    public static final String PERMISSION_HISTORY_VIEW = "view-history";

    public static final String PERMISSION_HISTORY_READ = "read-history";

    public static final String PERMISSION_HISTORY_DELETE = "delete-history";

    @SuppressWarnings("unchecked")
    public static <T extends MCRAccessInterface> T getAccessImpl() {
        return (T) MCRConfiguration2.getInstanceOfOrThrow(MCRAccessInterface.class, "MCR.Access.Class");
    }

    private static MCRAccessCheckStrategy getAccessStrategy() {
        // if acccessStrategy equals accessImpl we reuse the accessImpl, 
        // to make sure, that only one singleton gets created
        // (used to instantiate fact-based access system) 
        Optional<String> optStrategy = MCRConfiguration2.getString("MCR.Access.Strategy.Class");
        Optional<String> optAccessImpl = MCRConfiguration2.getString("MCR.Access.Class");
        if (optStrategy.isPresent() && optAccessImpl.isPresent() && optStrategy.get().equals(optAccessImpl.get())) {
            return MCRConfiguration2.getInstanceOfOrThrow(MCRAccessCheckStrategy.class, "MCR.Access.Class");
        } else {
            return MCRConfiguration2.getInstanceOfOrThrow(MCRAccessCheckStrategy.class, "MCR.Access.Strategy.Class");
        }
    }

    /**
     * adds an access rule for an MCRObjectID to an access system.
     *
     * @param id
     *            the MCRObjectID of the object
     * @param permission
     *            the access permission for the rule
     * @param rule
     *            the access rule
     * @param description
     *            description for the given access rule, e.g. "allows public access"
     * @throws MCRException
     *             if an error was occurred
     * @see MCRRuleAccessInterface#addRule(String, String, Element, String)
     */
    public static void addRule(MCRObjectID id, String permission, Element rule, String description)
        throws MCRException {
        requireRulesInterface().addRule(id.toString(), permission, rule, description);
    }

    /**
     * adds an access rule for an ID to an access system.
     *
     * @param id
     *            the ID of the object as String
     * @param permission
     *            the access permission for the rule
     * @param rule
     *            the access rule
     * @param description
     *            description for the given access rule, e.g. "allows public access"
     * @throws MCRException
     *             if an error was occurred
     * @see MCRRuleAccessInterface#addRule(String, String, Element, String)
     */
    public static void addRule(String id, String permission, Element rule, String description)
        throws MCRException {
        requireRulesInterface().addRule(id, permission, rule, description);
    }

    /**
     * removes the <code>permission</code> rule for the MCRObjectID.
     *
     * @param id
     *            the MCRObjectID of an object
     * @param permission
     *            the access permission for the rule
     * @throws MCRException
     *             if an error was occurred
     * @see MCRRuleAccessInterface#removeRule(String, String)
     */
    public static void removeRule(MCRObjectID id, String permission) throws MCRException {
        requireRulesInterface().removeRule(id.toString(), permission);
    }

    /**
     * removes the <code>permission</code> rule for the ID.
     *
     * @param id
     *            the ID of an object as String
     * @param permission
     *            the access permission for the rule
     * @throws MCRException
     *             if an error was occurred
     * @see MCRRuleAccessInterface#removeRule(String, String)
     */
    public static void removeRule(String id, String permission) throws MCRException {
        requireRulesInterface().removeRule(id, permission);
    }

    /**
     * removes all rules for the MCRObjectID.
     *
     * @param id
     *            the MCRObjectID of an object
     * @throws MCRException
     *             if an error was occurred
     * @see MCRRuleAccessInterface#removeRule(String)
     */
    public static void removeAllRules(MCRObjectID id) throws MCRException {
        requireRulesInterface().removeAllRules(id.toString());
    }

    /**
     * updates an access rule for an MCRObjectID.
     *
     * @param id
     *            the MCRObjectID of the object
     * @param permission
     *            the access permission for the rule
     * @param rule
     *            the access rule
     * @param description
     *            description for the given access rule, e.g. "allows public access"
     * @throws MCRException
     *             if an error was occurred
     * @see MCRRuleAccessInterface#updateRule(String, String, Element, String)
     */
    public static void updateRule(MCRObjectID id, String permission, Element rule, String description)
        throws MCRException {
        requireRulesInterface().updateRule(id.toString(), permission, rule, description);
    }

    /**
     * updates an access rule for an ID.
     *
     * @param id
     *            the ID of the object
     * @param permission
     *            the access permission for the rule
     * @param rule
     *            the access rule
     * @param description
     *            description for the given access rule, e.g. "allows public access"
     * @throws MCRException
     *             if an error was occurred
     * @see MCRRuleAccessInterface#updateRule(String, String, Element, String)
     */
    public static void updateRule(String id, String permission, Element rule, String description)
        throws MCRException {
        requireRulesInterface().updateRule(id, permission, rule, description);
    }

    /**
     * determines whether the current user has the permission to perform a certain action.
     *
     * @param id
     *            the MCRObjectID of the object
     * @param permission
     *            the access permission for the rule
     * @return true if the access is allowed otherwise it return
     * @see MCRRuleAccessInterface#checkPermission(String, String)
     */
    public static boolean checkPermission(MCRObjectID id, String permission) {
        return checkPermission(id.toString(), permission);
    }

    /**
     * checks if the current user has the permission to perform an action on the derivate metadata.
     * @param derId the MCRObjectID of the derivate
     * @param permission the access permission for the rule
     * @return true, if the access is allowed
     */
    public static boolean checkDerivateMetadataPermission(MCRObjectID derId, String permission) {
        MCRObjectID objectId = MCRMetadataManager.getObjectId(derId, 10, TimeUnit.MINUTES);
        if (objectId != null) {
            return checkPermission(objectId, permission);
        }
        return checkPermission(derId.toString(), permission);
    }

    /**
     * checks if the current user has the permission to perform an action on the derivate content.
     * @param derId the MCRObjectID of the derivate
     * @param permission the access permission for the rule
     * @return true, if the access is allowed
     */
    public static boolean checkDerivateContentPermission(MCRObjectID derId, String permission) {
        return checkPermission(derId.toString(), permission);
    }

    /**
     * checks if the current user has the permission to view the derivate content.
     * @param derId the MCRObjectID of the derivate
     * @return true, if the access is allowed
     */
    public static boolean checkDerivateDisplayPermission(String derId) {
        return checkPermission(derId, PERMISSION_READ) || checkPermission(derId, PERMISSION_VIEW);
    }

    /**
     * determines whether the current user has the permission to perform a certain action.
     *
     * @param id
     *            the MCRObjectID of the object
     * @param permission
     *            the access permission for the rule
     * @return true if the permission for the id is given
     */
    public static boolean checkPermission(String id, String permission) {
        Boolean value = ACCESS_CACHE.isPermitted(id, permission);
        if (value == null) {
            value = getAccessStrategy().checkPermission(id, permission);
            ACCESS_CACHE.cachePermission(id, permission, value);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("checkPermission id:{} permission:{} --> {}", id, permission, value);
        }
        return value;
    }

    /**
     * determines whether the current user has the permission to perform a certain action.
     *
     * @param permission
     *            the access permission for the rule
     * @return true if the permission exist
     */
    public static boolean checkPermission(String permission) {
        Boolean value = ACCESS_CACHE.isPermitted(null, permission);
        if (value == null) {
            value = getAccessImpl().checkPermission(permission);
            ACCESS_CACHE.cachePermission(null, permission, value);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("checkPermission permission:{} --> {}", permission, value);
        }
        return value;
    }

    /**
     * Invalidates the permission for current user on cache.
     *
     * @param id the {@link MCRObjectID}
     * @param permission the access permission
     */
    public static void invalidPermissionCache(String id, String permission) {
        ACCESS_CACHE.removePermission(id, permission);
    }

    /**
     * Invalidates all permissions for a specific id for current user on cache
     * @param ids id of the cache handle
     */
    public static void invalidPermissionCacheByID(String... ids) {
        ACCESS_CACHE.removePermission(ids);
    }

    /**
     * Invalidates all permissions for a specific id for all access caches in every session
     * @param ids id of the cache handle
     */
    public static void invalidAllPermissionCachesById(String... ids) {
        ACCESS_CACHE.removePermissionFromAllCachesById(ids);
    }

    /**
     * Invalidates the permission for current user on cache.
     *
     * @param permission the access permission
     */
    public static void invalidPermissionCache(String permission) {
        invalidPermissionCache(null, permission);
    }

    /**
     * lists all permissions defined for the <code>id</code>.
     *
     * @param id
     *            the ID of the object as String
     * @return a <code>List</code> of all for <code>id</code> defined permissions
     */
    public static Collection<String> getPermissionsForID(String id) {
        return requireRulesInterface().getPermissionsForID(id);
    }

    /**
     * lists all permissions defined for the <code>id</code>.
     *
     * @param id
     *            the MCRObjectID of the object
     * @return a <code>List</code> of all for <code>id</code> defined permissions
     */
    public static Collection<String> getPermissionsForID(MCRObjectID id) {
        return requireRulesInterface().getPermissionsForID(id.toString());
    }

    /**
     * return a rule, that allows something for everybody
     *
     * @return a rule, that allows something for everybody
     */
    public static Element getTrueRule() {
        Element condition = new Element("condition");
        condition.setAttribute("format", "xml");
        Element booleanOp = new Element("boolean");
        booleanOp.setAttribute("operator", "true");
        condition.addContent(booleanOp);
        return condition;
    }

    /**
     * return a rule, that forbids something for all, but superuser
     *
     * @return a rule, that forbids something for all, but superuser
     */
    public static Element getFalseRule() {
        Element condition = new Element("condition");
        condition.setAttribute("format", "xml");
        Element booleanOp = new Element("boolean");
        booleanOp.setAttribute("operator", "false");
        condition.addContent(booleanOp);
        return condition;
    }

    /**
     * return true if a rule for the id exist
     *
     * @param id
     *            the MCRObjectID of the object
     * @param permission
     *            the access permission for the rule
     */
    public static boolean hasRule(String id, String permission) {
        // if impl doesnt have a hasRule method, we assume there is a rule for id, permission
        if (getAccessImpl() instanceof MCRRuleAccessInterface) {
            return requireRulesInterface().hasRule(id, permission);
        } else {
            return true;
        }
    }

    public static boolean checkPermission(MCRUserInformation user, Supplier<Boolean> checkSupplier) {
        if (!MCRSessionMgr.hasCurrentSession()
            || !(MCRSessionMgr.getCurrentSession() instanceof MCRScopedSession session)) {
            throw new IllegalStateException("require an instance of MCRScopedSession");
        }
        return session.doAs(new MCRScopedSession.ScopedValues(user), checkSupplier);
    }

    public static MCRRuleAccessInterface requireRulesInterface() {
        if (!implementsRulesInterface()) {
            throw new MCRException(MCRAccessInterface.class + " is no " + MCRRuleAccessInterface.class);
        }
        return getAccessImpl();
    }

    public static boolean implementsRulesInterface() {
        return getAccessImpl() instanceof MCRRuleAccessInterface;
    }
}
