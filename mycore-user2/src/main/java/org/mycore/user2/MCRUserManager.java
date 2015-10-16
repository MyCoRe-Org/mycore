/**
 * $Revision$ 
 * $Date$
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

package org.mycore.user2;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.InstantiationException;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.mycore.access.MCRAccessManager;
import org.mycore.backend.hibernate.MCRHIBConnection;
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
    private static final MCRHIBConnection MCRHIB_CONNECTION = MCRHIBConnection.instance();

    private static final int HASH_ITERATIONS = MCRConfiguration.instance()
            .getInt(MCRUser2Constants.CONFIG_PREFIX + "HashIterations", 1000);

    private static final Logger LOGGER = Logger.getLogger(MCRUserManager.class);

    private static final SecureRandom SECURE_RANDOM;

    static {
        try {
            SECURE_RANDOM = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new InstantiationException("Could not initialize secure SECURE_RANDOM number", MCRUserManager.class,
                    e);
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
        if (!userName.contains("@"))
            return getUser(userName, MCRRealmFactory.getLocalRealm());
        else {
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
        Session session = MCRHIB_CONNECTION.getSession();
        MCRUser mcrUser = getByNaturalID(session, userName, realmId);
        if (mcrUser == null) {
            LOGGER.warn("Could not find requested user: " + userName + "@" + realmId);
            return null;
        }
        return setRoles(mcrUser);
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
        Session session = MCRHIB_CONNECTION.getSession();
        Criteria criteria = getUserCriteria(session);
        criteria.add(getUserRealmCriterion(userName, realm));
        criteria.setProjection(Projections.rowCount());
        int count = ((Number) criteria.uniqueResult()).intValue();
        return count != 0;
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

        Session session = MCRHIB_CONNECTION.getSession();
        session.save(user);
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
        Session session = MCRHIB_CONNECTION.getSession();
        MCRUser inDb = getByNaturalID(session, user.getUserName(), user.getRealmID());
        if (inDb == null) {
            createUser(user);
            return;
        }
        user.internalID = inDb.internalID;
        session.evict(inDb);
        session.update(user);
        MCRRoleManager.unassignRoles(user);
        MCRRoleManager.storeRoleAssignments(user);
    }

    /**
     * Deletes a user from the given database
     * 
     * @param userName the login name of the user to delete, in the default realm. 
     */
    public static void deleteUser(String userName) {
        if (!userName.contains("@"))
            deleteUser(userName, MCRRealmFactory.getLocalRealm());
        else {
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
        Session session = MCRHIB_CONNECTION.getSession();
        MCRUser user = getUser(userName, realmId);
        MCRRoleManager.unassignRoles(user);
        session.delete(user);
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
        Session session = MCRHIB_CONNECTION.getSession();
        Criteria criteria = getUserCriteria(session);
        criteria.add(Restrictions.eq("owner", owner));
        criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        @SuppressWarnings("unchecked")
        List<MCRUser> results = criteria.list();
        return results;
    }

    private static Criteria buildCondition(String userPattern, String realm, String namePattern) {
        if ("".equals(realm))
            realm = null;
        if ("".equals(userPattern))
            userPattern = null;
        if ("".equals(namePattern))
            namePattern = null;
        Session session = MCRHIB_CONNECTION.getSession();
        Criteria criteria = getUserCriteria(session);

        if (realm != null) {
            criteria.add(Restrictions.eq("realmID", realm));
        }
        Criterion userRestriction = null;
        if (userPattern != null) {
            userPattern = userPattern.replace('*', '%').replace('?', '.');
            userRestriction = Restrictions.ilike("userName", userPattern);
        }
        Criterion nameRestriction = null;
        if (namePattern != null) {
            namePattern = namePattern.replace('*', '%').replace('?', '.');
            nameRestriction = Restrictions.ilike("realName", namePattern);
        }
        if (userRestriction != null && nameRestriction != null) {
            criteria.add(Restrictions.or(userRestriction, nameRestriction));
        } else if (userRestriction != null) {
            criteria.add(userRestriction);
        } else if (nameRestriction != null) {
            criteria.add(nameRestriction);
        }
        criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria;
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
        Criteria condition = buildCondition(userPattern, realm, namePattern);
        @SuppressWarnings("unchecked")
        List<MCRUser> results = condition.list();
        return results;
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
        Criteria condition = buildCondition(userPattern, realm, namePattern);
        condition.setProjection(Projections.rowCount());
        return ((Number) condition.uniqueResult()).intValue();
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
            waitLoginPanalty();
            return null;
        }
        if (!user.loginAllowed()) {
            if (user.isDisabled()) {
                LOGGER.warn("User " + user.getUserID() + " was disabled!");
            } else {
                LOGGER.warn("Password expired for user " + user.getUserID() + " on "
                        + MCRXMLFunctions.getISODate(user.getValidUntil(), MCRISO8601Format.F_COMPLETE_HH_MM_SS));
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
        byte[] salt = SECURE_RANDOM.generateSeed(8);
        return salt;
    }

    private static MCRUser getByNaturalID(Session session, String userName, String realmId) {
        final Criteria criteria = getUserCriteria(session);
        criteria.setFetchMode("owner", FetchMode.JOIN);
        return (MCRUser) criteria.setCacheable(true).add(getUserRealmCriterion(userName, realmId)).uniqueResult();
    }

    private static Criteria getUserCriteria(Session session) {
        return session.createCriteria(MCRUser.class);
    }

    private static Criterion getUserRealmCriterion(String user, String realmId) {
        if (realmId == null) {
            realmId = MCRRealmFactory.getLocalRealm().getID();
        }
        return Restrictions.naturalId().set("userName", user).set("realmID", realmId);
    }
}
