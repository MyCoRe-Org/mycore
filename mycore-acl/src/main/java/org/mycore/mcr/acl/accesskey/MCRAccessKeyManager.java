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

import org.mycore.access.MCRAccessCacheHelper;
import org.mycore.access.MCRAccessManager;
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

/**
 * Methods to manage {@link MCRAccessKey}.
 */
public final class MCRAccessKeyManager {

    private static final String SECRET_STORAGE_MODE_PROP_PREFX = "MCR.ACL.AccessKey.Secret.Storage.Mode";

    private static final String SECRET_STORAGE_MODE = MCRConfiguration2
        .getStringOrThrow(SECRET_STORAGE_MODE_PROP_PREFX);

    private static final int HASHING_ITERATIONS = MCRConfiguration2
        .getInt(SECRET_STORAGE_MODE_PROP_PREFX + ".Hash.Iterations").orElse(1000);

    private static final MCRAccessKeyDAO DAO = new MCRAccessKeyDAO();

    /**
     * Returns all {@link MCRAccessKey} for given {@link MCRObjectID}.
     * 
     * @param objectId the MCRObjectID
     * @return MCRAccessKey list
     */
    public static synchronized List<MCRAccessKey> listAccessKeys(MCRObjectID objectId) {
        return (List) DAO.findAll(objectId);
    }

    /**
     * Checks the quality of the permission.
     * 
     * @param type permission type
     * @return true if valid
     */
    public static boolean isValidType(String type) {
        return (MCRAccessManager.PERMISSION_READ.equals(type) || MCRAccessManager.PERMISSION_WRITE.equals(type));
    }

    /**
     * Checks the quality of the secret.
     * 
     * @param secret the secret
     * @return true if valid
     */
    public static boolean isValidSecret(String secret) {
        return secret.length() > 0;
    }

    /**
     * Encrypts secret and uses {@link MCRObjectID} as salt.
     * 
     * @param secret the secret
     * @param objectId the MCRObjectID
     * @return hashed secret
     * @throws MCRException if encryption fails
     */
    public static String hashSecret(String secret, MCRObjectID objectId) throws MCRException {
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
     * 
     * @param objectId the MCRObjectID
     * @param accessKey the MCRAccessKey
     * @throws MCRAccessKeyInvalidSecretException if secret is not valid
     * @throws MCRAccessKeyNotFoundException if key does not exist
     * @throws MCRAccessKeyCollisionException if secret exists for MCRObjectID
     */
    public static synchronized void createAccessKey(MCRObjectID objectId, MCRAccessKey accessKey) {
        final String secret = accessKey.getSecret();
        if (secret == null || !isValidSecret(secret)) {
            throw new MCRAccessKeyInvalidSecretException();
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
     * Adds {@link MCRAccessKey} list for given {@link MCRObjectID}.
     * 
     * @param objectId the MCRObjectID
     * @param accessKeys the MCRAccessKey list
     * @throws MCRAccessKeyException if key is not valid
     */
    public static synchronized void addAccessKeys(MCRObjectID objectId, List<MCRAccessKey> accessKeys) {
        accessKeys.forEach(a -> addAccessKey(objectId, a));
    }

    /**
     * Deletes all {@link MCRAccessKey}.
     */
    public static void clearAccessKeys() {
        DAO.deleteAll();
    }

    /**
     * Deletes the all {@link MCRAccessKey} for given {@link MCRObjectID}.
     * 
     * @param objectId the MCRObjectID
     */
    public static void clearAccessKeys(MCRObjectID objectId) {
        DAO.deleteAll(objectId);
    }

    /**
     * Removes {@link MCRAccessKey} for given {@link MCRObjectID} and secret.
     * 
     * @param objectId the MCRObjectID
     * @param secret the secret
     * @throws MCRAccessKeyNotFoundException if MCRAccessKey does not exist
     */
    public static synchronized void removeAccessKey(MCRObjectID objectId, String secret) {
        final MCRAccessKey accessKey = getAccessKeyWithSecret(objectId, secret);
        if (accessKey == null) {
            throw new MCRAccessKeyNotFoundException();
        } else {
            MCRAccessCacheHelper.clearAllPermissionCaches(objectId.toString());
            DAO.delete(accessKey);
        }
    }

    /**
     * Updates {@link MCRAccessKey}.
     * 
     * @param objectId the MCRObjectID
     * @param secret the secret 
     * @param updatedAccessKey the MCRAccessKey
     * @throws MCRAccessKeyNotFoundException if key does not exist
     * @throws MCRAccessKeyInvalidTypeException if type is invalid
     */
    public static synchronized void updateAccessKey(MCRObjectID objectId, String secret,
        MCRAccessKey updatedAccessKey) {
        final MCRAccessKey accessKey = getAccessKeyWithSecret(objectId, secret);
        if (accessKey != null) {
            final String type = updatedAccessKey.getType();
            if (type != null && !accessKey.getType().equals(type)) {
                if (isValidType(type)) {
                    MCRAccessCacheHelper.clearAllPermissionCaches(objectId.toString());
                    accessKey.setType(type);
                } else {
                    throw new MCRAccessKeyInvalidTypeException();
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
            DAO.update(accessKey);
        } else {
            throw new MCRAccessKeyNotFoundException();
        }
    }

    /**
     * Returns the {@link MCRAccessKey} for given {@link MCRObjectID} and secret.
     * 
     * @param objectId the MCRObjectID
     * @param secret the hashed secret 
     * @return the MCRAccessKey or null
     */
    public static synchronized MCRAccessKey getAccessKeyWithSecret(MCRObjectID objectId, String secret) {
        return DAO.findBySecret(objectId, secret);
    }

    /**
     * Returns the {@link MCRAccessKey}s for given {@link MCRObjectID} and type.
     * 
     * @param objectId the MCRObjectID
     * @param type the type
     * @return MCRAccessKey list
     */
    public static synchronized List<MCRAccessKey> listAccessKeysWithType(MCRObjectID objectId, String type) {
        return (List) DAO.findAllByType(objectId, type);
    }

    private static synchronized void addAccessKey(MCRObjectID objectId, MCRAccessKey accessKey) {
        final String secret = accessKey.getSecret();
        if (secret == null) {
            throw new MCRAccessKeyInvalidSecretException();
        }
        final String type = accessKey.getType();
        if (type == null || !isValidType(type)) {
            throw new MCRAccessKeyInvalidTypeException();
        }
        if (getAccessKeyWithSecret(objectId, secret) == null) {
            accessKey.setId(0); //prevent collision
            accessKey.setObjectId(objectId);
            DAO.create(accessKey);
        } else {
            throw new MCRAccessKeyCollisionException();
        }
    }
}
