/*
 *
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */
package org.mycore.access;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.access.strategies.MCRAccessCheckStrategy;
import org.mycore.access.strategies.MCRDerivateIDStrategy;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.util.concurrent.MCRFixedUserCallable;

/**
 * @author Thomas Scheffler
 * @version $Revision$ $Date$
 */
public class MCRAccessManager {

    private static final MCRAccessCacheManager ACCESS_CACHE = new MCRAccessCacheManager();

    public static final Logger LOGGER = LogManager.getLogger(MCRAccessManager.class);

    public static final String PERMISSION_READ = "read";

    public static final String PERMISSION_WRITE = "writedb";

    public static final String PERMISSION_DELETE = "deletedb";

    private static final ExecutorService EXECUTOR_SERVICE;

    static {
        EXECUTOR_SERVICE = Executors.newWorkStealingPool();
        MCRShutdownHandler.getInstance().addCloseable(EXECUTOR_SERVICE::shutdownNow);
    }

    public static MCRAccessInterface getAccessImpl() {
        return MCRConfiguration.instance().<MCRAccessInterface> getSingleInstanceOf("MCR.Access.Class",
            MCRAccessBaseImpl.class.getName());
    }

    private static MCRAccessCheckStrategy getAccessStrategy() {
        return MCRConfiguration.instance().getInstanceOf("MCR.Access.Strategy.Class",
            MCRDerivateIDStrategy.class.getName());
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
     * @see MCRAccessInterface#addRule(String, String, org.jdom2.Element, String)
     */
    public static void addRule(MCRObjectID id, String permission, org.jdom2.Element rule, String description)
        throws MCRException {
        getAccessImpl().addRule(id.toString(), permission, rule, description);
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
     * @see MCRAccessInterface#addRule(String, String, org.jdom2.Element, String)
     */
    public static void addRule(String id, String permission, org.jdom2.Element rule, String description)
        throws MCRException {
        getAccessImpl().addRule(id, permission, rule, description);
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
     * @see MCRAccessInterface#removeRule(String, String)
     */
    public static void removeRule(MCRObjectID id, String permission) throws MCRException {
        getAccessImpl().removeRule(id.toString(), permission);
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
     * @see MCRAccessInterface#removeRule(String, String)
     */
    public static void removeRule(String id, String permission) throws MCRException {
        getAccessImpl().removeRule(id, permission);
    }

    /**
     * removes all rules for the MCRObjectID.
     *
     * @param id
     *            the MCRObjectID of an object
     * @throws MCRException
     *             if an error was occurred
     * @see MCRAccessInterface#removeRule(String)
     */
    public static void removeAllRules(MCRObjectID id) throws MCRException {
        getAccessImpl().removeAllRules(id.toString());
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
     * @see MCRAccessInterface#updateRule(String, String, Element, String)
     */
    public static void updateRule(MCRObjectID id, String permission, org.jdom2.Element rule, String description)
        throws MCRException {
        getAccessImpl().updateRule(id.toString(), permission, rule, description);
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
     * @see MCRAccessInterface#updateRule(String, String, Element, String)
     */
    public static void updateRule(String id, String permission, org.jdom2.Element rule, String description)
        throws MCRException {
        getAccessImpl().updateRule(id, permission, rule, description);
    }

    /**
     * determines whether the current user has the permission to perform a certain action.
     *
     * @param id
     *            the MCRObjectID of the object
     * @param permission
     *            the access permission for the rule
     * @return true if the access is allowed otherwise it return
     * @see MCRAccessInterface#checkPermission(String, String)
     */
    public static boolean checkPermission(MCRObjectID id, String permission) {
        return checkPermission(id.toString(), permission);
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
            LOGGER.debug("checkPermission id:" + id + " permission:" + permission + " --> " + value);
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
            LOGGER.debug("checkPermission permission:" + permission + " --> " + value);
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
     * Invalidates the permission for current user on cache.
     *
     * @param permission the access permission
     */
    public static void invalidPermissionCache(String permission) {
        invalidPermissionCache(null, permission);
    }

    /**
     * checks whether the current user has the permission to read/see a derivate check is also against the mcrobject,
     * the derivate belongs to both checks must return true <br>
     * it is needed in MCRFileNodeServlet and MCRZipServlet
     *
     * @param derID
     *            String ID of a MyCoRe-Derivate
     * @return true if the access is allowed otherwise it return false
     */
    public static boolean checkPermissionForReadingDerivate(String derID) {
        // derID must be a derivate ID
        boolean accessAllowed = false;
        MCRObjectID objectId = MCRMetadataManager.getObjectId(MCRObjectID.getInstance(derID), 10, TimeUnit.MINUTES);
        if (objectId != null) {
            accessAllowed = checkPermission(objectId, PERMISSION_READ) && checkPermission(derID, PERMISSION_READ);
        } else {
            accessAllowed = checkPermission(derID, PERMISSION_READ);
            LogManager.getLogger("MCRAccessManager.class").warn("no mcrobject could be found for derivate: " + derID);
        }
        return accessAllowed;
    }

    /**
     * lists all permissions defined for the <code>id</code>.
     *
     * @param id
     *            the ID of the object as String
     * @return a <code>List</code> of all for <code>id</code> defined permissions
     */
    public static Collection<String> getPermissionsForID(String id) {
        return getAccessImpl().getPermissionsForID(id);
    }

    /**
     * lists all permissions defined for the <code>id</code>.
     *
     * @param id
     *            the MCRObjectID of the object
     * @return a <code>List</code> of all for <code>id</code> defined permissions
     */
    public static Collection<String> getPermissionsForID(MCRObjectID id) {
        return getAccessImpl().getPermissionsForID(id.toString());
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
        return getAccessImpl().hasRule(id, permission);
    }

    public static CompletableFuture<Boolean> checkPermission(MCRUserInformation user, Supplier<Boolean> checkSuplier) {
        return checkPermission(user, checkSuplier, EXECUTOR_SERVICE);
    }

    public static CompletableFuture<Boolean> checkPermission(MCRUserInformation user, Supplier<Boolean> checkSuplier,
        ExecutorService es) {
        return CompletableFuture.supplyAsync(getWrappedFixedUserCallable(user, checkSuplier), es);
    }

    private static Supplier<Boolean> getWrappedFixedUserCallable(MCRUserInformation user,
        Supplier<Boolean> checkSuplier) {
        Supplier<Boolean> check = () -> {
            try {
                return checkSuplier.get();
            } finally {
                MCREntityManagerProvider.getCurrentEntityManager().clear();
            }
        };
        MCRFixedUserCallable<Boolean> mcrFixedUserCallable = new MCRFixedUserCallable<>(check::get, user);
        return () -> {
            try {
                return mcrFixedUserCallable.call();
            } catch (Exception e) {
                LOGGER.error("Exception while running ACL check for user: " + user.getUserID(), e);
                return false;
            }
        };
    }

}
