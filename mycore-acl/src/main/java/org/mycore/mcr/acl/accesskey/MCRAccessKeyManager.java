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

package org.mycore.mcr.acl.accesskey;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessCacheHelper;
import org.mycore.access.MCRAccessManager;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.crypt.MCRCipher;
import org.mycore.crypt.MCRCipherManager;
import org.mycore.crypt.MCRCryptKeyFileNotFoundException;
import org.mycore.crypt.MCRCryptKeyNoPermissionException;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyCollisionException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyInvalidSecretException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyInvalidTypeException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;

import jakarta.persistence.EntityManager;

/**
 * Methods to manage {@link MCRAccessKey}.
 */
public final class MCRAccessKeyManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String SECRET_STORAGE_MODE_PROP_PREFX = "MCR.ACL.AccessKey.Secret.Storage.Mode";

    private static final String SECRET_STORAGE_MODE = MCRConfiguration2
        .getStringOrThrow(SECRET_STORAGE_MODE_PROP_PREFX);

    private static final int HASHING_ITERATIONS = MCRConfiguration2
        .getInt(SECRET_STORAGE_MODE_PROP_PREFX + ".Hash.Iterations").orElse(1000);

    /**
     * Returns all access keys for given {@link MCRObjectID}.
     *
     * @param objectId the {@link MCRObjectID}
     * @return {@link MCRAccessKey} list
     */
    public static synchronized List<MCRAccessKey> listAccessKeys(final MCRObjectID objectId) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        final List<MCRAccessKey> accessKeys = em.createNamedQuery("MCRAccessKey.getWithObjectId", MCRAccessKey.class)
            .setParameter("objectId", objectId)
            .getResultList();
        for (MCRAccessKey accessKey : accessKeys) {
            em.detach(accessKey);
        }
        return accessKeys;
    }

    /**
     * Checks the quality of the permission.
     *
     * @param type permission type
     * @return true if valid or false
     */
    public static boolean isValidType(final String type) {
        return (type.equals(MCRAccessManager.PERMISSION_READ) || type.equals(MCRAccessManager.PERMISSION_WRITE));
    }

    /**
     * Checks the quality of the secret.
     *
     * @param secret the secret
     * @return true if valid or false
     */
    public static boolean isValidSecret(final String secret) {
        return secret.length() > 0;
    }

    /**
     * Encrypts secret and uses {@link MCRObjectID} as salt.
     *
     * @param secret the secret
     * @param objectId the {@link MCRObjectID}
     * @return hashed secret
     * @throws MCRException if encryption fails
     */
    public static String hashSecret(final String secret, final MCRObjectID objectId) throws MCRException {
        switch (SECRET_STORAGE_MODE) {
        case "plain":
            return secret;
        case "crypt":
            try {
                final MCRCipher cipher = MCRCipherManager.getCipher("accesskey");
                return cipher.encrypt(objectId.toString() + secret);
            } catch (MCRCryptKeyFileNotFoundException | MCRCryptKeyNoPermissionException e) {
                throw new MCRException(e);
            }
        case "hash":
            try {
                return MCRUtils.asSHA256String(HASHING_ITERATIONS, objectId.toString().getBytes(UTF_8), secret);
            } catch (NoSuchAlgorithmException e) {
                throw new MCRException("Cannot hash secret.", e);
            }
        default:
            throw new MCRException("Please configure a valid storage mode for secret.");
        }
    }

    /**
     * Creates a {@link MCRAccessKey} for given {@link MCRObjectID}.
     * Hashed the secret
     *
     * @param objectId the {@link MCRObjectID}
     * @param accessKey access key with secret
     * @throws MCRException key is not valid
     */
    public static synchronized void createAccessKey(final MCRObjectID objectId, final MCRAccessKey accessKey)
        throws MCRException {
        final String secret = accessKey.getSecret();
        if (secret == null || !isValidSecret(secret)) {
            throw new MCRAccessKeyInvalidSecretException("Incorrect secret.");
        }
        accessKey.setSecret(hashSecret(secret, objectId));
        accessKey.setCreatedBy(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID());
        accessKey.setCreated(new Date());
        accessKey.setLastModifiedBy(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID());
        accessKey.setLastModified(new Date());
        if (accessKey.getIsActive() == null) {
            accessKey.setIsActive(true);
        }
        addAccessKey(objectId, accessKey);
    }

    /**
     * Adds a {@link MCRAccessKey} for given {@link MCRObjectID}.
     * Checks for a {@link MCRAccessKey} collision
     *
     * @param objectId the {@link MCRObjectID}
     * @param accessKey access key with hashed secret
     * @throws MCRException key is not valid
     */
    private static synchronized void addAccessKey(final MCRObjectID objectId, final MCRAccessKey accessKey)
        throws MCRException {
        final String secret = accessKey.getSecret();
        if (secret == null) {
            LOGGER.debug("Incorrect secret.");
            throw new MCRAccessKeyInvalidSecretException("Incorrect secret.");
        }
        final String type = accessKey.getType();
        if (type == null || !isValidType(type)) {
            LOGGER.debug("Invalid permission type.");
            throw new MCRAccessKeyInvalidTypeException("Invalid permission type.");
        }
        if (getAccessKeyWithSecret(objectId, secret) == null) {
            accessKey.setId(0); //prevent collision
            accessKey.setObjectId(objectId);
            final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
            em.persist(accessKey);
            em.detach(accessKey);
        } else {
            LOGGER.debug("Key collision.");
            throw new MCRAccessKeyCollisionException("Key collision.");
        }
    }

    /**
     * Adds {@link MCRAccessKey} list for given {@link MCRObjectID}.
     *
     * @param objectId the {@link MCRObjectID}
     * @param accessKeys the {@link MCRAccessKey} list
     * @throws MCRAccessKeyException key is not valid
     */
    public static synchronized void addAccessKeys(final MCRObjectID objectId, final List<MCRAccessKey> accessKeys)
        throws MCRAccessKeyException {
        for (MCRAccessKey accessKey : accessKeys) { //Transaktion
            addAccessKey(objectId, accessKey);
        }
    }

    /**
     * Deletes all {@link MCRAccessKey}.
     */
    public static void clearAccessKeys() {
        MCREntityManagerProvider.getCurrentEntityManager()
            .createNamedQuery("MCRAccessKey.clear")
            .executeUpdate();
    }

    /**
     * Deletes the all {@link MCRAccessKey} for given {@link MCRObjectID}.
     *
     * @param objectId the {@link MCRObjectID}
     */
    public static void clearAccessKeys(final MCRObjectID objectId) {
        MCREntityManagerProvider.getCurrentEntityManager()
            .createNamedQuery("MCRAccessKey.clearWithObjectId")
            .setParameter("objectId", objectId)
            .executeUpdate();
    }

    /**
     * Removes {@link MCRAccessKey} for given {@link MCRObjectID} and secret.
     *
     * @param objectId the {@link MCRObjectID}
     * @param secret the secret
     */
    public static synchronized void removeAccessKey(final MCRObjectID objectId, final String secret)
        throws MCRAccessKeyNotFoundException {
        final MCRAccessKey accessKey = getAccessKeyWithSecret(objectId, secret);
        if (accessKey == null) {
            LOGGER.debug("Key does not exist.");
            throw new MCRAccessKeyNotFoundException("Key does not exist.");
        } else {
            MCRAccessCacheHelper.clearAllPermissionCaches(objectId.toString());
            final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
            em.remove(em.contains(accessKey) ? accessKey : em.merge(accessKey));
        }
    }

    /**
     * Updates {@link MCRAccessKey}
     *
     * @param objectId the {@link MCRObjectID}
     * @param secret the access key secret 
     * @param updatedAccessKey access key
     * @throws MCRException if update fails
     */
    public static synchronized void updateAccessKey(final MCRObjectID objectId, final String secret,
        final MCRAccessKey updatedAccessKey) throws MCRException {
        final MCRAccessKey accessKey = getAccessKeyWithSecret(objectId, secret);
        if (accessKey != null) {
            final String type = updatedAccessKey.getType();
            if (type != null && !accessKey.getType().equals(type)) {
                if (isValidType(type)) {
                    MCRAccessCacheHelper.clearAllPermissionCaches(objectId.toString());
                    accessKey.setType(type);
                } else {
                    LOGGER.debug("Unkown Type.");
                    throw new MCRAccessKeyInvalidTypeException("Unknown permission type.");
                }
            }
            final Boolean isActive = updatedAccessKey.getIsActive();
            if (isActive != null) {
                MCRAccessCacheHelper.clearAllPermissionCaches(objectId.toString());
                accessKey.setIsActive(isActive);
            }
            final Date expiration = updatedAccessKey.getExpiration();
            if (expiration != null) {
                MCRAccessCacheHelper.clearAllPermissionCaches(objectId.toString());
                accessKey.setExpiration(expiration);
            }
            final String comment = updatedAccessKey.getComment();
            if (comment != null) {
                accessKey.setComment(comment);
            }
            accessKey.setLastModifiedBy(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID());
            accessKey.setLastModified(new Date());
            final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
            em.merge(accessKey);
        } else {
            LOGGER.debug("Key does not exist.");
            throw new MCRAccessKeyNotFoundException("Key does not exist.");
        }
    }

    /**
     * Return the {@link MCRAccessKey} for given {@link MCRObjectID} and secret.
     *
     * @param objectId the {@link MCRObjectID}
     * @param secret the hashed secret 
     * @return the {@link MCRAccessKey}
     */
    public static synchronized MCRAccessKey getAccessKeyWithSecret(final MCRObjectID objectId, final String secret) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        final MCRAccessKey accessKey = em.createNamedQuery("MCRAccessKey.getWithSecret", MCRAccessKey.class)
            .setParameter("objectId", objectId)
            .setParameter("secret", secret)
            .getResultList()
            .stream()
            .findFirst()
            .orElse(null);
        if (accessKey != null) {
            em.detach(accessKey);
        }
        return accessKey;
    }

    /**
     * Return the access keys for given {@link MCRObjectID} and type.
     *
     * @param objectId the {@link MCRObjectID}
     * @param type the type
     * @return {@link MCRAccessKey} list
     */
    public static synchronized List<MCRAccessKey> listAccessKeysWithType(final MCRObjectID objectId,
        final String type) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        final List<MCRAccessKey> accessKeys = em.createNamedQuery("MCRAccessKey.getWithType", MCRAccessKey.class)
            .setParameter("objectId", objectId)
            .setParameter("type", type)
            .getResultList();
        for (MCRAccessKey accessKey : accessKeys) {
            em.detach(accessKey);
        }
        return accessKeys;
    }
}
