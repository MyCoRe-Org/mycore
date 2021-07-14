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

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.mycore.access.MCRAccessManager;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.backend.MCRAccessKey;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyCollisionException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyInvalidTypeException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyInvalidValueException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;

/**
 * Methods to manage {@link MCRAccessKey}.
 */
public final class MCRAccessKeyManager {

    private static final Logger LOGGER = LogManager.getLogger();

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
    private static boolean isValidType(String type) {
        return (type.equals(MCRAccessManager.PERMISSION_READ)
            || type.equals(MCRAccessManager.PERMISSION_WRITE));
    }

    /**
     * Checks the quality of the value.
     *
     * @param value the value 
     * @return true if valid or false
     */
    private static boolean isValidValue(String value) {
        return value.length() > 0;
    }

    /**
     * Adds a {@link MCRAccessKey} for given {@link MCRObjectID}.
     * Checks for a {@link MCRAccessKey} collision
     *
     * @param accessKey the access key
     * @throws MCRAccessKeyException key is not valid
     */
    public static synchronized void addAccessKey(final MCRAccessKey accessKey) throws MCRAccessKeyException {
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
        if (getAccessKeyByValue(objectId, value) == null) {
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
     * @param objectId {@link MCRAccessKey} of the key to be updated
     * @param currentValue current value of the key to be updated
     * @param newAccessKey the new {@link MCRAccessKey}
     * @throws MCRAccessKeyException if update fails
     */
    public static synchronized void updateAccessKey(final MCRObjectID objectId,
        final String currentValue, final MCRAccessKey newAccessKey)
        throws MCRAccessKeyException {
        final String type = newAccessKey.getType();
        if (type == null) {
            LOGGER.warn("Permission type is required");
            throw new MCRAccessKeyInvalidTypeException("Permission type is required.");
        }
        final String value = newAccessKey.getValue();
        if (value == null) {
            LOGGER.warn("Value is required.");
            throw new MCRAccessKeyInvalidValueException("Value is required.");
        }
        final MCRAccessKey accessKey = getAccessKeyByValue(objectId, currentValue);
        if (accessKey != null) {
            if (accessKey.getValue().equals(value) && accessKey.getType().equals(type)) {
                LOGGER.info("Nothing to update.");
            } else {
                if (accessKey.getValue().equals(value)) {
                    if (isValidType(type)) {
                        MCRAccessManager.invalidPermissionCache(objectId.toString(), accessKey.getType());
                        accessKey.setType(type);
                        MCRAccessManager.invalidPermissionCache(objectId.toString(), accessKey.getType());
                    } else {
                        LOGGER.warn("Unkown Type.");
                        throw new MCRAccessKeyInvalidTypeException("Unknown permission type.");
                    }
                } else {
                    if (getAccessKeyByValue(objectId, value) == null) {
                        if (!accessKey.getType().equals(type)) {
                            if (isValidType(type)) {
                                MCRAccessManager.invalidPermissionCache(objectId.toString(), accessKey.getType());
                                accessKey.setType(type);
                                MCRAccessManager.invalidPermissionCache(objectId.toString(), accessKey.getType());
                            } else {
                                LOGGER.warn("Unkown Type.");
                                throw new MCRAccessKeyInvalidTypeException("Unknown permission type.");
                            }
                        } else {
                            if (isValidValue(value)) {
                                accessKey.setValue(value);
                                MCRAccessManager.invalidPermissionCache(objectId.toString(), accessKey.getType());
                            } else {
                                LOGGER.warn("Incorrect Value.");
                                throw new MCRAccessKeyInvalidValueException("Incorrect Value.");
                            }
                        }
                    } else {
                        LOGGER.warn("Key collision.");
                        throw new MCRAccessKeyCollisionException("Key collision.");
                    }
                }
            }
        } else {
            LOGGER.warn("Key does not exists.");
            throw new MCRAccessKeyNotFoundException("Key does not exists.");
        }
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
