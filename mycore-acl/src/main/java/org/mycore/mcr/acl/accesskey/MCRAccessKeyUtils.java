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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserAttribute;
import org.mycore.user2.MCRUserManager;

/**
 * Methods for setting and removing {@link MCRAccessKey} for users.
 */
public class MCRAccessKeyUtils {

    /**
     * Prefix for user attribute name for value
     */
    public static final String ACCESS_KEY_PREFIX = "acckey_";

    /**
     * Adds the value of {@link MCRAccessKey} as an attribute to a {@link MCRSession} for {@link MCRObjectID}.
     *
     * @param session the {@link MCRSession}
     * @param objectId the {@link MCRObjectID}
     * @param value the value of a {@link MCRAccessKey}
     * @throws MCRException if there is no matching {@link MCRAccessKey} with the same value.
     */
    public static synchronized void addAccessKeySecret(final MCRSession session, final MCRObjectID objectId, 
        final String value) throws MCRException {
        final String secret = MCRAccessKeyManager.hashSecret(value, objectId);

        final MCRAccessKey accessKey = MCRAccessKeyManager.getAccessKeyWithSecret(objectId, secret);
        if (accessKey == null) {
            throw new MCRAccessKeyNotFoundException("Key does not exists.");
        }
        
        session.put(getAttributeName(objectId), secret);
        MCRAccessManager.invalidPermissionCache(objectId.toString(), accessKey.getType());
    }

    /**
     * Adds the value of a {@link MCRAccessKey} as user attribute to a {@link MCRUser} for a {@link MCRObjectID}.
     *
     * @param user the {@link MCRUser} the value should assigned
     * @param objectId the {@link MCRObjectID}
     * @param value the value of a {@link MCRAccessKey}
     * @throws MCRException if there is no matching {@link MCRAccessKey} with the same value.
     */
    public static synchronized void addAccessKeySecret(final MCRUser user, final MCRObjectID objectId, String value)
        throws MCRException {

        final String secret = MCRAccessKeyManager.hashSecret(value, objectId);
        final MCRAccessKey accessKey = MCRAccessKeyManager.getAccessKeyWithSecret(objectId, secret);
        if (accessKey == null) {
            throw new MCRAccessKeyNotFoundException("Key does not exists.");
        }

        user.setUserAttribute(getAttributeName(objectId), secret);
        MCRUserManager.updateUser(user);

        MCRAccessManager.invalidPermissionCache(objectId.toString(), accessKey.getType());
    }

    /**
     * Adds the value of {@link MCRAccessKey} as an attribute to the current {@link MCRSession} for {@link MCRObjectID}.
     *
     * @param objectId the {@link MCRObjectID}
     * @param value the value of a {@link MCRAccessKey}
     * @throws MCRException if there is no matching {@link MCRAccessKey} with the same value.
     */
    public static synchronized void addAccessKeySecretToCurrentSession(final MCRObjectID objectId, final String value)
        throws MCRException {
        addAccessKeySecret(MCRSessionMgr.getCurrentSession(), objectId, value);
    }

    /**
     * Adds the value of {@link MCRAccessKey} as user attribute to the current {@link MCRUser} for {@link MCRObjectID}.
     *
     * @param objectId the {@link MCRObjectID}
     * @param value the value of a {@link MCRAccessKey}
     * @throws MCRException if there is no matching {@link MCRAccessKey} with the same value.
     */
    public static synchronized void addAccessKeySecretToCurrentUser(final MCRObjectID objectId, final String value) 
        throws MCRException {
        addAccessKeySecret(MCRUserManager.getCurrentUser(), objectId, value);
    }

    /**
     * Lists all users which own at least an access key user attribute in given range
     *
     * @param offset the offset
     * @param limit the limit
     * @return a list with all users which own at least an access key in given range
     */
    private static List<MCRUser> listUsersWithAccessKey(final int offset, final int limit) {
        return MCRUserManager.listUsers(null, null, null, null, ACCESS_KEY_PREFIX + "*", offset, limit);
    }

    /**
     * Cleans all access key secret attributes of users if the corresponding key does not exist.
     */
    public static void cleanUpUserAttributes() {
        final Set<MCRUserAttribute> validAttributes = new HashSet<>();
        final Set<MCRUserAttribute> deadAttributes = new HashSet<>();
        int offset = 0;
        final int limit = 1024;
        List<MCRUser> users = new ArrayList<>();
        do {
            users = listUsersWithAccessKey(offset * limit, limit);
            for (final MCRUser user : users) {
                final List<MCRUserAttribute> attributes = user.getAttributes()
                    .stream()
                    .filter(attribute -> attribute.getName().startsWith(MCRAccessKeyUtils.ACCESS_KEY_PREFIX))
                    .filter(attribute -> !validAttributes.contains(attribute))
                    .collect(Collectors.toList());
                for (MCRUserAttribute attribute : attributes) {
                    final String attributeName = attribute.getName();
                    final MCRObjectID objectId = MCRObjectID.getInstance(attributeName.substring(
                        attributeName.indexOf("_") + 1));
                    if (deadAttributes.contains(attribute)) {
                        MCRAccessKeyUtils.removeAccessKeySecret(user, objectId);
                    } else {
                        if (MCRAccessKeyManager.getAccessKeyWithSecret(objectId, attribute.getValue()) != null) {
                            validAttributes.add(attribute);
                        } else {
                            MCRAccessKeyUtils.removeAccessKeySecret(user, objectId);
                            deadAttributes.add(attribute);
                        }
                    }
                }
            }
            offset++;
        }
        while (users.size() == limit);
    }

    /**
     * Fetches access key value from session attribute for a {@link MCRObjectID}.
     *
     * @param session the {@link MCRSession}
     * @param objectId the {@link MCRObjectID}
     * @return secret or null
     */
    public static synchronized String getAccessKeySecret(final MCRSession session, final MCRObjectID objectId) {
        final Object secret = session.get(getAttributeName(objectId));
        if (secret != null) {
            return (String) secret;
        }
        return null;
    }

    /**
     * Fetches access key value from user attribute for a {@link MCRObjectID}.
     *
     * @param userInformation the {@link MCRUserInformation}
     * @param objectId the {@link MCRObjectID}
     * @return secret or null
     */
    public static synchronized String getAccessKeySecret(final MCRUserInformation userInformation, 
        final MCRObjectID objectId) {
        return userInformation.getUserAttribute(getAttributeName(objectId));
    }

    /**
     * Fetches access key value from session attribute for a {@link MCRObjectID}.
     *
     * @param objectId the {@link MCRObjectID}
     * @return secret or null
     */
    public static synchronized String getAccessKeySecretFromCurrentSession(final MCRObjectID objectId) {
        return getAccessKeySecret(MCRSessionMgr.getCurrentSession(), objectId);
    }

    /**
     * Fetches access key value from user attribute for a {@link MCRObjectID}.
     *
     * @param objectId the {@link MCRObjectID}
     * @return secret or null
     */
    public static synchronized String getAccessKeySecretFromCurrentUser(final MCRObjectID objectId) {
        return getAccessKeySecret(MCRSessionMgr.getCurrentSession().getUserInformation(), objectId);
    }

    /**
     * Returns the attribute name for user and session of an access key value
     *
     * @param objectId the {@link MCRObjectID}
     * @return the attribute name
     */ 
    private static String getAttributeName(final MCRObjectID objectId) {
        return ACCESS_KEY_PREFIX + objectId.toString();
    }

    /**
     * Deletes the access key value attribute from given {@link MCRSession} for {@link MCRObjectID}.
     *
     * @param session the {@link MCRSession}
     * @param objectId the {@link MCRObjectID}
     */
    public static synchronized void removeAccessKeySecret(final MCRSession session, final MCRObjectID objectId) {
        session.deleteObject(getAttributeName(objectId));
        MCRAccessManager.invalidPermissionCache(objectId.toString(), MCRAccessManager.PERMISSION_READ);
        MCRAccessManager.invalidPermissionCache(objectId.toString(), MCRAccessManager.PERMISSION_WRITE);
    }

    /**
     * Deletes the access key value user attribute from given {@link MCRUser} for {@link MCRObjectID}.
     *
     * @param user the {@link MCRUser}
     * @param objectId the {@link MCRObjectID}
     */
    public static synchronized void removeAccessKeySecret(final MCRUser user, final MCRObjectID objectId) {
        user.getAttributes().removeIf(ua -> ua.getName().equals(getAttributeName(objectId)));
        MCRUserManager.updateUser(user);
        MCRAccessManager.invalidPermissionCache(objectId.toString(), MCRAccessManager.PERMISSION_READ);
        MCRAccessManager.invalidPermissionCache(objectId.toString(), MCRAccessManager.PERMISSION_WRITE);
    }

    /**
     * Deletes access key value attribute from current {@link MCRSession} for a {@link MCRObjectID}.
     *
     * @param objectId the {@link MCRObjectID}
     */
    public static synchronized void removeAccessKeySecretFromCurrentSession(final MCRObjectID objectId) {
        removeAccessKeySecret(MCRSessionMgr.getCurrentSession(), objectId);
    }

    /**
     * Deletes access key value user attribute from current {@link MCRUser} for a {@link MCRObjectID}.
     *
     * @param objectId the {@link MCRObjectID}
     */
    public static synchronized void removeAccessKeySecretFromCurrentUser(final MCRObjectID objectId) {
        removeAccessKeySecret(MCRUserManager.getCurrentUser(), objectId);
    }
}
