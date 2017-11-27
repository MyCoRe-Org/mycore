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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRISO8601Format;

/**
 * Manages all users using a database table.
 *
 * @author Frank L\u00fctzenkirchen
 * @author Thomas Scheffler (yagee)
 */
public class MCRUserManager {

    private static final int HASH_ITERATIONS = MCRConfiguration.instance()
        .getInt(MCRUser2Constants.CONFIG_PREFIX + "HashIterations", 1000);

    private static final Logger LOGGER = LogManager.getLogger();

    private static final SecureRandom SECURE_RANDOM;

    static {
        try {
            SECURE_RANDOM = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new MCRException("Could not initialize secure SECURE_RANDOM number", e);
        }
    }

    /** The table that stores login user information */
    static String table;

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
            return getUser(parts[0], parts[1]);
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
        return propertyQuery.getResultList()
            .stream()
            .filter(u -> u.getAttributes().get(attrName).equals(attrValue));
    }

    private static MCRUser setRoles(MCRUser mcrUser) {
        Collection<MCRCategoryID> roleIDs = MCRRoleManager.getRoleIDs(mcrUser);
        mcrUser.getSystemRoleIDs().clear();
        mcrUser.getExternalRoleIDs().clear();
        for (MCRCategoryID roleID : roleIDs) {
            if (roleID.getRootID().equals(MCRUser2Constants.ROLE_CLASSID.getRootID())) {
                mcrUser.getSystemRoleIDs().add(roleID.getID());
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

        if (user instanceof MCRTransientUser) {
            createUser((MCRTransientUser) user);
            return;
        }

        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.persist(user);
        LOGGER.info(() -> "user saved: " + user.getUserID());
        MCRRoleManager.storeRoleAssignments(user);
    }

    /**
     * Creates and store a new login user in the database, do also attribute mapping is needed.
     * This will also store role membership information.
     *
     * @param user the user to create in the database.
     */
    public static void createUser(MCRTransientUser user) {
        if (isInvalidUser(user)) {
            throw new MCRException("User is invalid: " + user.getUserID());
        }

        createUser(user.clone());
    }

    /**
     * Checks whether the user is invalid.
     *
     * MCRUser is not allowed to overwrite information returned by {@link MCRSystemUserInformation#getGuestInstance()} or {@link MCRSystemUserInformation#getSystemUserInstance()}.
     * @return true if {@link #createUser(MCRUser)} or {@link #updateUser(MCRUser)} would reject the given user
     */
    public static boolean isInvalidUser(MCRUser user) {
        if (MCRSystemUserInformation.getGuestInstance().getUserID().equals(user.getUserID())) {
            return true;
        }
        return MCRSystemUserInformation.getSystemUserInstance().getUserID().equals(user.getUserID());
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
            em.detach(db);
            em.merge(user);
            MCRRoleManager.unassignRoles(user);
            MCRRoleManager.storeRoleAssignments(user);
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
                .where(cb.equal(users.get(MCRUser_.owner), owner)))
            .getResultList();
    }

    private static Predicate[] buildCondition(CriteriaBuilder cb, Root<MCRUser> root, Optional<String> userPattern,
        Optional<String> realm, Optional<String> namePattern) {
        ArrayList<Predicate> predicates = new ArrayList<>(3);
        realm
            .filter(s -> !s.isEmpty())
            .map(s -> cb.equal(root.get(MCRUser_.realmID), s))
            .ifPresent(predicates::add);

        Optional<Predicate> userPredicate = userPattern
            .filter(s -> !s.isEmpty())
            .map(s -> s.replace('*', '%'))
            .map(s -> s.replace('?', '_'))
            .map(s -> s.toLowerCase(MCRSessionMgr.getCurrentSession().getLocale()))
            .map(s -> cb.like(cb.lower(root.get(MCRUser_.userName)), s));

        Optional<Predicate> namePredicate = namePattern
            .filter(s -> !s.isEmpty())
            .map(s -> s.replace('*', '%'))
            .map(s -> s.replace('?', '_'))
            .map(s -> s.toLowerCase(MCRSessionMgr.getCurrentSession().getLocale()))
            .map(s -> cb.like(cb.lower(root.get(MCRUser_.realName)), s));

        if (userPattern.isPresent() && namePredicate.isPresent()) {
            predicates.add(cb.or(userPredicate.get(), namePredicate.get()));
        } else {
            userPredicate.ifPresent(predicates::add);
            namePredicate.ifPresent(predicates::add);
        }
        return predicates.toArray(new Predicate[predicates.size()]);
    }

    /**
     * Searches for users in the database and returns a list of matching users.
     * Wildcards containing * and ? for single character may be used for searching
     * by login user name or real name.
     *
     * Pay attention that no role information is attached to user data. If you need
     * this information call {@link MCRUserManager#getUser(String, String)}.
     *
     * @param userPattern a wildcard pattern for the login user name, may be null
     * @param namePattern a wildcard pattern for the person's real name, may be null
     * @param realm the realm the user belongs to, may be null
     * @return a list of matching users
     */
    public static List<MCRUser> listUsers(String userPattern, String realm, String namePattern) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRUser> query = cb.createQuery(MCRUser.class);
        Root<MCRUser> user = query.from(MCRUser.class);
        return em
            .createQuery(
                query
                    .where(
                        buildCondition(cb, user, Optional.ofNullable(userPattern), Optional.ofNullable(realm),
                            Optional.ofNullable(namePattern))))
            .getResultList();
    }

    /**
     * Counts users in the database that match the given criteria.
     * Wildcards containing * and ? for single character may be used for searching
     * by login user name or real name.
     *
     * @param userPattern a wildcard pattern for the login user name, may be null
     * @param namePattern a wildcard pattern for the person's real name, may be null
     * @param realm the realm the user belongs to, may be null
     * @return the number of matching users
     */
    public static int countUsers(String userPattern, String realm, String namePattern) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Number> query = cb.createQuery(Number.class);
        Root<MCRUser> user = query.from(MCRUser.class);
        return em
            .createQuery(
                query
                    .select(cb.count(user))
                    .where(
                        buildCondition(cb, user, Optional.ofNullable(userPattern), Optional.ofNullable(realm),
                            Optional.ofNullable(namePattern))))
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
        MCRUser user = checkPassword(userName, password);
        if (user == null) {
            return null;
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
        if (userInformation instanceof MCRUser) {
            return (MCRUser) userInformation;
        } else {
            return new MCRTransientUser(userInformation);
        }
    }

    /**
     * Returns a {@link MCRUser} instance if the login succeeds.
     * This method will return <code>null</code> if the user does not exist or the login is disabled.
     * If the {@link MCRUser#getHashType()} is {@link MCRPasswordHashType#crypt}, {@link MCRPasswordHashType#md5} or {@link MCRPasswordHashType#sha1}
     * the hash value is automatically upgraded to {@link MCRPasswordHashType#sha256}.
     * @param userName Name of the user to login.
     * @param password clear text password.
     * @return authenticated {@link MCRUser} instance or <code>null</code>.
     */
    public static MCRUser checkPassword(String userName, String password) {
        MCRUser user = getUser(userName);
        if (user == null || user.getHashType() == null) {
            LOGGER.warn(() -> "User not found: " + userName);
            waitLoginPanalty();
            return null;
        }
        if (!user.loginAllowed()) {
            if (user.isDisabled()) {
                LOGGER.warn("User {} was disabled!", user.getUserID());
            } else {
                LOGGER.warn("Password expired for user {} on {}", user.getUserID(),
                    MCRXMLFunctions.getISODate(user.getValidUntil(), MCRISO8601Format.COMPLETE_HH_MM_SS.toString()));
            }
            return null;
        }
        try {
            switch (user.getHashType()) {
                case crypt:
                    //Wahh! did we ever thought about what "salt" means for passwd management?
                    String passwdHash = user.getPassword();
                    String salt = passwdHash.substring(0, 3);
                    if (!MCRUtils.asCryptString(salt, password).equals(passwdHash)) {
                        //login failed
                        waitLoginPanalty();
                        return null;
                    }
                    //update to SHA-256
                    updatePasswordHashToSHA256(user, password);
                    break;
                case md5:
                    if (!MCRUtils.asMD5String(1, null, password).equals(user.getPassword())) {
                        waitLoginPanalty();
                        return null;
                    }
                    //update to SHA-256
                    updatePasswordHashToSHA256(user, password);
                    break;
                case sha1:
                    if (!MCRUtils.asSHA1String(HASH_ITERATIONS, Base64.getDecoder().decode(user.getSalt()), password)
                        .equals(user.getPassword())) {
                        waitLoginPanalty();
                        return null;
                    }
                    //update to SHA-256
                    updatePasswordHashToSHA256(user, password);
                    break;
                case sha256:
                    if (!MCRUtils.asSHA256String(HASH_ITERATIONS, Base64.getDecoder().decode(user.getSalt()), password)
                        .equals(user.getPassword())) {
                        waitLoginPanalty();
                        return null;
                    }
                    break;
                default:
                    throw new MCRException("Cannot validate hash type " + user.getHashType());
            }
        } catch (NoSuchAlgorithmException e) {
            throw new MCRException("Error while validating login", e);
        }
        return user;
    }

    private static void waitLoginPanalty() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }
    }

    /**
     * Sets password of 'user' to 'password'.
     *
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
        updatePasswordHashToSHA256(myUser, password);
        updateUser(myUser);
    }

    static void updatePasswordHashToSHA256(MCRUser user, String password) {
        String newHash;
        byte[] salt = generateSalt();
        try {
            newHash = MCRUtils.asSHA256String(HASH_ITERATIONS, salt, password);
        } catch (Exception e) {
            throw new MCRException("Could not update user password hash to SHA-256.", e);
        }
        user.setSalt(Base64.getEncoder().encodeToString(salt));
        user.setHashType(MCRPasswordHashType.sha256);
        user.setPassword(newHash);
    }

    private static byte[] generateSalt() {
        return SECURE_RANDOM.generateSeed(8);
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
        if (realmId == null) {
            realmId = MCRRealmFactory.getLocalRealm().getID();
        }
        return new Predicate[] { cb.equal(root.get(MCRUser_.userName), user),
            cb.equal(root.get(MCRUser_.realmID), realmId) };
    }
}
