/*
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

package org.mycore.mcr.acl.accesskey;

import static org.mycore.access.MCRAccessManager.PERMISSION_READ;
import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyCollisionException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyInvalidTypeException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyInvalidValueException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;

/**
 * Methods to manage {@link MCRAccessKey}.
 */
public final class MCRAccessKeyManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int HASH_ITERATIONS = MCRConfiguration2
        .getInt("MCR.ACL.AccessKey.Value.HashIterations").orElse(1000);

    /**
     * Returns all access keys for given {@link MCRObjectID}.
     *
     * @param objectId the {@link MCRObjectID}
     * @return {@link MCRAccessKey} list
     */
    public static synchronized List<MCRAccessKey> getAccessKeys(final MCRObjectID objectId) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        return em.createNamedQuery("MCRAccessKey.getById", MCRAccessKey.class)
            .setParameter("objId", objectId)
            .getResultList();
    }

    /**
     * Checks the quality of the permission.
     *
     * @param type permission type
     * @return true if valid or false
     */
    public static boolean isValidType(String type) {
        return (type.equals(PERMISSION_READ) || type.equals(PERMISSION_WRITE));
    }

    /**
     * Checks the quality of the value.
     *
     * @param value the value 
     * @return true if valid or false
     */
    public static boolean isValidValue(String value) {
        return value.length() > 0;
    }

    /**
     * Encrypts value and uses {@link MCRObjectID} as salt.
     *
     * @param value the key value
     * @param objectId the {@link MCRObjectID}
     * @return encrypted value as SHA256 string
     * @throws MCRException if encryption fails
     */
    public static String encryptValue(final String value, final MCRObjectID objectId) throws MCRException {
        try {
            return MCRUtils.asSHA256String(HASH_ITERATIONS, objectId.toString().getBytes(StandardCharsets.UTF_8), 
                value);
        } catch(NoSuchAlgorithmException e) {
            throw new MCRException("Encryption failed.");
        }
    }

    /**
     * Adds a {@link MCRAccessKey} for given {@link MCRObjectID}.
     * Checks for a {@link MCRAccessKey} collision
     *
     * @param accessKey the access key
     * @throws MCRException key is not valid
     */
    public static synchronized void addAccessKey(final MCRAccessKey accessKey) throws MCRException {
        final MCRObjectID objectId = accessKey.getObjectId();
        if (objectId == null) {
            LOGGER.warn("Object id is required.");
            throw new MCRAccessKeyException("Object id is required.");
        }
        final String type = accessKey.getType();
        if (type == null || !isValidType(type)) {
            LOGGER.warn("Invalid permission type.");
            throw new MCRAccessKeyInvalidTypeException("Invalid permission type.");
        }
        final String value = accessKey.getValue();
        if (value == null || !isValidValue(value)) {
            LOGGER.warn("Incorrect value.");
            throw new MCRAccessKeyInvalidValueException("Incorrect value.");
        }
        final String encryptedValue = encryptValue(value, objectId);
        if (getAccessKeyByValue(objectId, encryptedValue) == null) {
            accessKey.setValue(encryptedValue);
            if (accessKey.getCreator() == null || accessKey.getCreation() == null) {
                accessKey.setCreator(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID());
                accessKey.setCreation(new Date());
            }
            if (accessKey.getLastChanger() == null || accessKey.getLastChange() == null) {
                accessKey.setLastChanger(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID());
                accessKey.setLastChange(new Date());
            }
            final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
            em.persist(accessKey);
        } else {
            LOGGER.warn("Key collision.");
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
        for (MCRAccessKey accessKey : accessKeys) {
            accessKey.setObjectId(objectId);
            accessKey.setId(0); //prevent collision
            addAccessKey(accessKey);
        }
    }

    /**
     * Deletes all {@link MCRAccessKey}.
     */
    public static void clearAccessKeys() {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.createNamedQuery("MCRAccessKey.clear")
            .executeUpdate();
    }

    /**
     * Deletes the all {@link MCRAccessKey} for given {@link MCRObjectID}.
     *
     * @param objectId the {@link MCRObjectID}
     */
    public static void clearAccessKeys(final MCRObjectID objectId) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.createNamedQuery("MCRAccessKey.clearById")
            .setParameter("objId", objectId)
            .executeUpdate();
    }

    /**
     * Deletes {@link MCRAccessKey} for given {@link MCRObjectID} and value.
     *
     * @param objectId the {@link MCRObjectID}
     * @param value the value
     */
    public static void deleteAccessKey(final MCRObjectID objectId, final String value)
        throws MCRAccessKeyNotFoundException {
        final MCRAccessKey accessKey = getAccessKeyByValue(objectId, value);
        if (accessKey == null) {
            LOGGER.warn("Key does not exists.");
            throw new MCRAccessKeyNotFoundException("Key does not exists.");
        } else {
            deleteAccessKey(accessKey);
        }
    }

    /**
     * Deletes {@link MCRAccessKey}.
     *
     * @param accessKey the {@link MCRAccessKey}
     */
    public static void deleteAccessKey(final MCRAccessKey accessKey) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.remove(accessKey);
        MCRAccessManager.invalidPermissionCache(accessKey.getObjectId().toString(), accessKey.getType());
    }

    /**
     * Updates {@link MCRAccessKey}
     *
     * @param updatedAccessKey the new {@link MCRAccessKey}
     * @throws MCRException if update fails
     */
    public static void updateAccessKey(final MCRAccessKey updatedAccessKey) throws MCRException{
        final MCRObjectID objectId = updatedAccessKey.getObjectId();
        if (objectId == null) {
            LOGGER.warn("Object id is required.");
            throw new MCRAccessKeyException("Object id is required.");
        }
        String value = updatedAccessKey.getValue();
        if (value == null) {
            LOGGER.warn("Value is required.");
            throw new MCRAccessKeyInvalidValueException("Value is required.");
        }
        final MCRAccessKey accessKey = getAccessKeyByValue(objectId, value);
        if (accessKey == null) {
            LOGGER.warn("Key does not exists.");
            throw new MCRAccessKeyNotFoundException("Key does not exists.");
        }
        final String type = updatedAccessKey.getType();
        if (type != null && !accessKey.getType().equals(type)) {
            if (!isValidType(type)) {
                LOGGER.warn("Unkown Type.");
                throw new MCRAccessKeyInvalidTypeException("Unknown permission type.");
            }
            MCRAccessManager.invalidPermissionCache(objectId.toString(), accessKey.getType());
            accessKey.setType(type);
        }
        final String comment = updatedAccessKey.getComment();
        if (comment != null) {
            accessKey.setComment(comment);
        }
        accessKey.setLastChanger(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID());
        accessKey.setLastChange(new Date());
    }

    /**
     * Return the {@link MCRAccessKey} for given {@link MCRObjectID} and value.
     *
     * @param objectId the {@link MCRObjectID}
     * @param value the key value
     * @return the {@link MCRAccessKey}
     */
    public static synchronized MCRAccessKey getAccessKeyByValue(final MCRObjectID objectId, final String value) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        return em.createNamedQuery("MCRAccessKey.getByValue", MCRAccessKey.class)
            .setParameter("objId", objectId)
            .setParameter("value", value)
            .getResultList()
            .stream()
            .findFirst()
            .orElse(null);
    }

    /**
     * Return the access key for given {@link MCRObjectID} and value.
     *
     * @param objectId the {@link MCRObjectID}
     * @param type the type
     * @return {@link MCRAccessKey} list
     */
    public static synchronized List<MCRAccessKey> getAccessKeysByType(final MCRObjectID objectId, final String type) {
        final EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        return em.createNamedQuery("MCRAccessKey.getByType", MCRAccessKey.class)
            .setParameter("objId", objectId)
            .setParameter("type", type)
            .getResultList();
    }
}
