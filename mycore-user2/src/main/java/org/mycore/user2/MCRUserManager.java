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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRISO8601Format;
import org.mycore.user2.hash.MCRPasswordCheckData;
import org.mycore.user2.hash.MCRPasswordCheckManager;
import org.mycore.user2.hash.MCRPasswordCheckResult;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * Manages all users using a database table.
 *
 * @author Frank L\u00fctzenkirchen
 * @author Thomas Scheffler (yagee)
 */
public class MCRUserManager {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Returns the user with the given userName, in the default realm
     *
     * @param userName the unique userName within the default realm
     * @return the user with the given login name, or null
     */
    public static MCRUser getUser(String userName) {
        if (!userName.contains("@")) {
            return getUser(userName, MCRRealmFactory.getLocalRealm());
        } else {
            String[] parts = userName.split("@");
            if (parts.length == 2) {
                return getUser(parts[0], parts[1]);
            } else {
                return null;
            }
        }
    }

    /**
     * Returns the user with the given userName, in the given realm
     *
     * @param userName the unique userName within the given realm
     * @param realm the realm the user belongs to
     * @return the user with the given login name, or null
     */
    public static MCRUser getUser(String userName, MCRRealm realm) {
        return getUser(userName, realm.getID());
    }

    /**
     * Returns the user with the given userName, in the given realm
     *
     * @param userName the unique userName within the given realm
     * @param realmId the ID of the realm the user belongs to
     * @return the user with the given login name, or null
     */
    public static MCRUser getUser(String userName, String realmId) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        return getByNaturalID(em, userName, realmId)
            .map(MCRUserManager::setRoles)
            .orElseGet(() -> {
                LOGGER.warn("Could not find requested user: {}@{}", userName, realmId);
                return null;
            });
    }

    /**
     * Returns a Stream of users where the user has a given attribute.
     * @param attrName name of the user attribute
     * @param attrValue value of the user attribute
     */
    public static Stream<MCRUser> getUsers(String attrName, String attrValue) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<MCRUser> propertyQuery = em.createNamedQuery("MCRUser.byPropertyValue", MCRUser.class);
        propertyQuery.setParameter("name", attrName);
        propertyQuery.setParameter("value", attrValue);
        MCRUserAttribute attr = new MCRUserAttribute(attrName, attrValue);
        return propertyQuery.getResultList()
            .stream()
            .filter(u -> u.getAttributes().contains(attr))
            .peek(em::refresh); //fixes MCR-1885
    }

    private static MCRUser setRoles(MCRUser mcrUser) {
        Collection<MCRCategoryID> roleIDs = MCRRoleManager.getRoleIDs(mcrUser);
        mcrUser.getSystemRoleIDs().clear();
        mcrUser.getExternalRoleIDs().clear();
        for (MCRCategoryID roleID : roleIDs) {
            if (roleID.getRootID().equals(MCRUser2Constants.ROLE_CLASSID.getRootID())) {
                mcrUser.getSystemRoleIDs().add(roleID.getId());
            } else {
                mcrUser.getExternalRoleIDs().add(roleID.toString());
            }
        }
        return mcrUser;
    }

    /**
     * Checks if a user with the given login name exists in the default realm.
     *
     * @param userName the login user name.
     * @return true, if a user with the given login name exists.
     */
    public static boolean exists(String userName) {
        return exists(userName, MCRRealmFactory.getLocalRealm());
    }

    /**
     * Checks if a user with the given login name exists in the given realm.
     *
     * @param userName the login user name.
     * @param realm the realm the user belongs to
     * @return true, if a user with the given login name exists.
     */
    public static boolean exists(String userName, MCRRealm realm) {
        return exists(userName, realm.getID());
    }

    /**
     * Checks if a user with the given login name exists in the given realm.
     *
     * @param userName the login user name.
     * @param realm the ID of the realm the user belongs to
     * @return true, if a user with the given login name exists.
     */
    public static boolean exists(String userName, String realm) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Number> query = cb.createQuery(Number.class);
        Root<MCRUser> users = query.from(MCRUser.class);
        return em.createQuery(
            query
                .select(cb.count(users))
                .where(getUserRealmCriterion(cb, users, userName, realm)))
            .getSingleResult().intValue() > 0;
    }

    /**
     * Creates and stores a new login user in the database.
     * This will also store role membership information.
     *
     * @param user the user to create in the database.
     */
    public static void createUser(MCRUser user) {
        if (isInvalidUser(user)) {
            throw new MCRException("User is invalid: " + user.getUserID());
        }

        MCRUser persistableUser = user.toPersistableUser();

        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.persist(persistableUser);
        LOGGER.info(() -> "user saved: " + persistableUser.getUserID());
        MCRRoleManager.storeRoleAssignments(persistableUser);
        MCREvent evt = new MCREvent(MCREvent.ObjectType.USER, MCREvent.EventType.CREATE);
        evt.put(MCREvent.USER_KEY, persistableUser);
        MCREventManager.getInstance().handleEvent(evt);
    }

    /**
     * Checks whether the user is invalid.
     * <p>
     * MCRUser is not allowed to overwrite information returned by {@link MCRSystemUserInformation#GUEST}
     * or {@link MCRSystemUserInformation#SYSTEM_USER}.
     * @return true if {@link #createUser(MCRUser)} or {@link #updateUser(MCRUser)} would reject the given user
     */
    public static boolean isInvalidUser(MCRUser user) {
        return MCRSystemUserInformation.GUEST.getUserID().equals(user.getUserID())
            || MCRSystemUserInformation.SYSTEM_USER.getUserID().equals(user.getUserID());
    }

    /**
     * Updates an existing login user in the database.
     * This will also update role membership information.
     *
     * @param user the user to update in the database.
     */
    public static void updateUser(MCRUser user) {
        if (isInvalidUser(user)) {
            throw new MCRException("User is invalid: " + user.getUserID());
        }
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        Optional<MCRUser> inDb = getByNaturalID(em, user.getUserName(), user.getRealmID());
        if (!inDb.isPresent()) {
            createUser(user);
            return;
        }
        inDb.ifPresent(db -> {
            user.internalID = db.internalID;
            em.merge(user);
            MCRRoleManager.unassignRoles(user);
            MCRRoleManager.storeRoleAssignments(user);
            MCREvent evt = new MCREvent(MCREvent.ObjectType.USER, MCREvent.EventType.UPDATE);
            evt.put(MCREvent.USER_KEY, user);
            MCREventManager.getInstance().handleEvent(evt);
        });
    }

    /**
     * Deletes a user from the given database
     *
     * @param userName the login name of the user to delete, in the default realm.
     */
    public static void deleteUser(String userName) {
        if (!userName.contains("@")) {
            deleteUser(userName, MCRRealmFactory.getLocalRealm());
        } else {
            String[] parts = userName.split("@");
            deleteUser(parts[0], parts[1]);
        }
    }

    /**
     * Deletes a user from the given database
     *
     * @param userName the login name of the user to delete, in the given realm.
     * @param realm the realm the user belongs to
     */
    public static void deleteUser(String userName, MCRRealm realm) {
        deleteUser(userName, realm.getID());
    }

    /**
     * Deletes a user from the given database
     *
     * @param userName the login name of the user to delete, in the given realm.
     * @param realmId the ID of the realm the user belongs to
     */
    public static void deleteUser(String userName, String realmId) {
        MCRUser user = getUser(userName, realmId);
        MCREvent evt = new MCREvent(MCREvent.ObjectType.USER, MCREvent.EventType.DELETE);
        evt.put(MCREvent.USER_KEY, user);
        MCREventManager.getInstance().handleEvent(evt);
        MCRRoleManager.unassignRoles(user);
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.remove(user);
    }

    /**
     * Deletes a user from the given database
     *
     * @param user the user to delete
     */
    public static void deleteUser(MCRUser user) {
        deleteUser(user.getUserName(), user.getRealmID());
    }

    /**
     * Returns a list of all users the given user is owner of.
     *
     * @param owner the user that owns other users
     */
    public static List<MCRUser> listUsers(MCRUser owner) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRUser> query = cb.createQuery(MCRUser.class);
        Root<MCRUser> users = query.from(MCRUser.class);
        users.fetch(MCRUser_.owner);
        return em.createQuery(
            query
                .distinct(true)
                .where(
                    owner == null ? cb.isNull(users.get(MCRUser_.owner)) : cb.equal(users.get(MCRUser_.owner), owner)))
            .getResultList();
    }

    private static String buildSearchPattern(String searchPattern) {
        return searchPattern.replace('*', '%').replace('?', '_')
            .toLowerCase(MCRSessionMgr.getCurrentSession().getLocale());
    }

    private static boolean isValidSearchPattern(String searchPattern) {
        return StringUtils.isNotEmpty(searchPattern);
    }

    private static Predicate[] buildCondition(CriteriaBuilder cb, Root<MCRUser> root, String userPattern, String realm,
        String namePattern, String mailPattern, String attributeNamePattern, String attributeValuePattern) {

        List<Predicate> predicates = new ArrayList<>(2);
        addEqualsPredicate(cb, root, MCRUser_.realmID, realm, predicates);

        List<Predicate> searchPredicates = new ArrayList<>(3);
        addSearchPredicate(cb, root, MCRUser_.userName, userPattern, searchPredicates);
        addSearchPredicate(cb, root, MCRUser_.realName, namePattern, searchPredicates);
        addSearchPredicate(cb, root, MCRUser_.EMail, mailPattern, searchPredicates);

        if (isValidSearchPattern(attributeNamePattern) || isValidSearchPattern(attributeValuePattern)) {
            Join<MCRUser, MCRUserAttribute> userAttributeJoin = root.join(MCRUser_.attributes, JoinType.LEFT);
            if (isValidSearchPattern(attributeNamePattern)) {
                searchPredicates.add(cb.like(cb.lower(userAttributeJoin.get(MCRUserAttribute_.name)),
                    buildSearchPattern(attributeNamePattern)));
            }
            if (isValidSearchPattern(attributeValuePattern)) {
                searchPredicates.add(cb.like(cb.lower(userAttributeJoin.get(MCRUserAttribute_.value)),
                    buildSearchPattern(attributeValuePattern)));
            }
        }

        if (!searchPredicates.isEmpty()) {
            if (searchPredicates.size() == 1) {
                predicates.add(searchPredicates.getFirst());
            } else {
                predicates.add(cb.or(searchPredicates.toArray(Predicate[]::new)));
            }
        }

        return predicates.toArray(Predicate[]::new);
    }

    private static void addEqualsPredicate(CriteriaBuilder cb, Root<MCRUser> root,
        SingularAttribute<MCRUser, String> attribute, String string, List<Predicate> predicates) {
        if (isValidSearchPattern(string)) {
            predicates.add(cb.equal(root.get(attribute), string));
        }
    }

    private static void addSearchPredicate(CriteriaBuilder cb, Root<MCRUser> root,
        SingularAttribute<MCRUser, String> attribute, String searchPattern, List<Predicate> predicates) {
        if (isValidSearchPattern(searchPattern)) {
            predicates.add(buildSearchPredicate(cb, root, attribute, searchPattern));
        }
    }

    private static Predicate buildSearchPredicate(CriteriaBuilder cb, Root<MCRUser> root,
        SingularAttribute<MCRUser, String> attribute, String searchPattern) {
        return cb.like(cb.lower(root.get(attribute)), buildSearchPattern(searchPattern));
    }

    /**
     * Searches for users in the database and returns a list of all matching users.
     * Wildcards containing * and ? for single character may be used for searching
     * by login user name or real name.
     * <p>
     * Pay attention that no role information is attached to user data. If you need
     * this information call {@link MCRUserManager#getUser(String, String)}.
     *
     * @param userPattern a wildcard pattern for the login user name, may be null
     * @param realm the realm the user belongs to, may be null
     * @param namePattern a wildcard pattern for the person's real name, may be null
     * @param mailPattern a wildcard pattern for the person's email, may be null
     * @return a list of all matching users
     */
    public static List<MCRUser> listUsers(String userPattern, String realm, String namePattern, String mailPattern) {
        return listUsers(userPattern, realm, namePattern, mailPattern, null, null, 0, Integer.MAX_VALUE);
    }

    /**
     * Searches for users in the database and returns a list of matching users.
     * Wildcards containing * and ? for single character may be used for searching
     * by login user name or real name.
     * <p>
     * Pay attention that no role information is attached to user data. If you need
     * this information call {@link MCRUserManager#getUser(String, String)}.
     *
     * @param userPattern a wildcard pattern for the login user name, may be null
     * @param realm the realm the user belongs to, may be null
     * @param namePattern a wildcard pattern for the person's real name, may be null
     * @param mailPattern a wildcard pattern for the person's email, may be null
     * @param attributeNamePattern a wildcard pattern for person's attribute names, may be null
     * @param offset an offset for matching users
     * @param limit a limit for matching users
     * @return a list of matching users in offset and limit range
     */
    public static List<MCRUser> listUsers(String userPattern, String realm, String namePattern, String mailPattern,
        String attributeNamePattern, int offset, int limit) {
        return listUsers(userPattern, realm, namePattern, mailPattern, attributeNamePattern, null, offset, limit);
    }

    /**
     * Searches for users in the database and returns a list of matching users.
     * Wildcards containing * and ? for single character may be used for searching
     * by login user name or real name.
     * <p>
     * Pay attention that no role information is attached to user data. If you need
     * this information call {@link MCRUserManager#getUser(String, String)}.
     *
     * @param userPattern a wildcard pattern for the login user name, may be null
     * @param realm the realm the user belongs to, may be null
     * @param namePattern a wildcard pattern for the person's real name, may be null
     * @param mailPattern a wildcard pattern for the person's email, may be null
     * @param attributeNamePattern a wildcard pattern for person's attribute names, may be null
     * @param attributeValuePattern a wildcard pattern for person's attribute values, may be null
     * @param offset an offset for matching users
     * @param limit a limit for matching users
     * @return a list of matching users in offset and limit range
     */
    public static List<MCRUser> listUsers(String userPattern, String realm, String namePattern, String mailPattern,
        String attributeNamePattern, String attributeValuePattern, int offset, int limit) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRUser> query = cb.createQuery(MCRUser.class);
        Root<MCRUser> user = query.from(MCRUser.class);
        return em
            .createQuery(
                query
                    .distinct(true)
                    .where(
                        buildCondition(cb, user, userPattern, realm, namePattern, mailPattern,
                            attributeNamePattern, attributeValuePattern)))
            .setFirstResult(offset)
            .setMaxResults(limit)
            .getResultList();
    }

    /**
     * Counts users in the database that match the given criteria.
     * Wildcards containing * and ? for single character may be used for searching
     * by login user name or real name.
     *
     * @param userPattern a wildcard pattern for the login user name, may be null
     * @param realm the realm the user belongs to, may be null
     * @param namePattern a wildcard pattern for the person's real name, may be null
     * @param mailPattern a wildcard pattern for the person's email, may be null
     * @return the number of matching users
     */
    public static int countUsers(String userPattern, String realm, String namePattern, String mailPattern) {
        return countUsers(userPattern, realm, namePattern, mailPattern, null, null);
    }

    /**
     * Counts users in the database that match the given criteria.
     * Wildcards containing * and ? for single character may be used for searching
     * by login user name or real name.
     *
     * @param userPattern a wildcard pattern for the login user name, may be null
     * @param realm the realm the user belongs to, may be null
     * @param namePattern a wildcard pattern for the person's real name, may be null
     * @param mailPattern a wildcard pattern for the person's email, may be null
     * @param attributeNamePattern a wildcard pattern for person's attribute names, may be null
     * @return the number of matching users
     */
    public static int countUsers(String userPattern, String realm, String namePattern, String mailPattern,
        String attributeNamePattern) {
        return countUsers(userPattern, realm, namePattern, mailPattern, attributeNamePattern, null);

    }

    /**
     * Counts users in the database that match the given criteria.
     * Wildcards containing * and ? for single character may be used for searching
     * by login user name or real name.
     *
     * @param userPattern a wildcard pattern for the login user name, may be null
     * @param realm the realm the user belongs to, may be null
     * @param namePattern a wildcard pattern for the person's real name, may be null
     * @param mailPattern a wildcard pattern for the person's email, may be null
     * @param attributeNamePattern a wildcard pattern for person's attribute names, may be null
     * @param attributeValuePattern a wildcard pattern for person's attribute names, may be null
     * @return the number of matching users
     */
    public static int countUsers(String userPattern, String realm, String namePattern, String mailPattern,
        String attributeNamePattern, String attributeValuePattern) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Number> query = cb.createQuery(Number.class);
        Root<MCRUser> user = query.from(MCRUser.class);
        return em
            .createQuery(
                query
                    .select(cb.count(user))
                    .distinct(true)
                    .where(
                        buildCondition(cb, user, userPattern, realm, namePattern, mailPattern,
                            attributeNamePattern, attributeValuePattern)))
            .getSingleResult().intValue();
    }

    /**
     * Checks the password of a login user in the default realm.
     *
     * @param userName the login user name
     * @param password the password entered in the GUI
     * @return true, if the password matches.
     */
    public static MCRUser login(String userName, String password) {
        return login(userName, password, Collections.emptyList());
    }

    /**
     * Checks the password of a login user and if the user has at least one of the allowed roles in the default realm.
     *
     * @param userName the login user name
     * @param password the password entered in the GUI
     * @param allowedRoles list of allowed roles
     * @return true, if the password matches.
     */
    public static MCRUser login(String userName, String password, List<String> allowedRoles) {
        MCRUser user = checkPassword(userName, password);
        if (user == null) {
            return null;
        }
        if (!allowedRoles.isEmpty()) {
            Collection<String> userRoles = user.getSystemRoleIDs();
            LOGGER.info(() -> "Comparing user roles " + userRoles + " against list of allowed roles " + allowedRoles);
            boolean hasAnyAllowedRole = userRoles.stream().anyMatch(allowedRoles::contains);
            if (!hasAnyAllowedRole) {
                return null;
            }
        }
        user.setLastLogin();
        updateUser(user);
        MCRSessionMgr.getCurrentSession().setUserInformation(user);
        return user;
    }

    /**
     * Returns instance of MCRUser if current user is present in this user system
     * @return MCRUser instance or null
     */
    public static MCRUser getCurrentUser() {
        MCRUserInformation userInformation = MCRSessionMgr.getCurrentSession().getUserInformation();
        if (userInformation instanceof MCRUser mcrUser) {
            return mcrUser;
        } else {
            return new MCRTransientUser(userInformation);
        }
    }

    /**
     * Returns a {@link MCRUser} instance if the login succeeds.
     * This method will return <code>null</code> if the user does not exist, no password was given or
     * the login is disabled.
     * If the {@link MCRUser#getHashType()} is not the currently preferred hash type, the stored password hash is
     * automatically updated.
     * @param userName Name of the user to login.
     * @param password clear text password.
     * @return authenticated {@link MCRUser} instance or <code>null</code>.
     */
    public static MCRUser checkPassword(String userName, String password) {
        MCRUser user = getUser(userName);
        if (user == null || user.getHashType() == null) {
            LOGGER.warn(() -> "User not found: " + userName);
            waitLoginPenalty();
            return null;
        }
        if (password == null) {
            LOGGER.warn("No password for user {} entered", userName);
            waitLoginPenalty();
            return null;
        }
        if (!user.loginAllowed()) {
            if (user.isDisabled()) {
                LOGGER.warn("User {} was disabled!", user::getUserID);
            } else {
                LOGGER.warn("Password expired for user {} on {}", user::getUserID,
                    () -> MCRXMLFunctions.getISODate(user.getValidUntil(),
                        MCRISO8601Format.COMPLETE_HH_MM_SS.toString()));
            }
            return null;
        }

        MCRPasswordCheckData hash = new MCRPasswordCheckData(user.getHashType(), user.getSalt(), user.getHash());
        MCRPasswordCheckResult result = MCRPasswordCheckManager.obtainInstance().verify(hash, password);

        if (!result.valid()) {
            waitLoginPenalty();
            return null;
        }

        if (result.deprecated()) {
            setUserPassword(user, password);
        }

        return user;

    }

    private static void waitLoginPenalty() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // exception safely ignored
        }
    }

    /**
     * Sets password of 'user' to 'password'.
     * <p>
     * Automatically updates the user in database.
     */
    public static void setPassword(MCRUser user, String password) {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        MCRUserInformation currentUser = session.getUserInformation();
        MCRUser myUser = getUser(user.getUserName(), user.getRealmID()); //only update password
        boolean allowed = MCRAccessManager.checkPermission(MCRUser2Constants.USER_ADMIN_PERMISSION)
            || currentUser.equals(myUser.getOwner())
            || (currentUser.equals(user) && myUser.hasNoOwner() || !myUser.isLocked());
        if (!allowed) {
            throw new MCRException("You are not allowed to change password of user: " + user);
        }
        setUserPassword(myUser, password);
        updateUser(myUser);
    }

    public static void setUserPassword(MCRUser user, String password) {
        setUserPassword(MCRPasswordCheckManager.obtainInstance(), user, password);
    }

    private static void setUserPassword(MCRPasswordCheckManager passwordCheckManager, MCRUser user, String password) {
        MCRPasswordCheckData data = passwordCheckManager.create(password);
        user.setHashType(data.type());
        user.setHash(data.hash());
        user.setSalt(data.salt());
    }

    private static Optional<MCRUser> getByNaturalID(EntityManager em, String userName, String realmId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRUser> query = cb.createQuery(MCRUser.class);
        Root<MCRUser> users = query.from(MCRUser.class);
        users.fetch(MCRUser_.owner.getName(), JoinType.LEFT);
        try {
            return Optional
                .of(em
                    .createQuery(query
                        .distinct(true)
                        .where(getUserRealmCriterion(cb, users, userName, realmId)))
                    .getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    private static Predicate[] getUserRealmCriterion(CriteriaBuilder cb, Root<MCRUser> root, String user,
        String realmId) {
        return new Predicate[] { cb.equal(root.get(MCRUser_.userName), user),
            cb.equal(root.get(MCRUser_.realmID), realmId == null ? MCRRealmFactory.getLocalRealm().getID() : realmId) };
    }

}
