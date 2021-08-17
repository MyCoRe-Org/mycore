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

import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

/**
 * Methods for setting and removing {@link MCRAccessKey} for users.
 */
public class MCRAccessKeyUserUtils {

    /**
     * Prefix for user attribute name for value
    */
    public static final String ACCESS_KEY_PREFIX = "acckey_";

    /**
     * Adds the value of {@link MCRAccessKey} as user attribute to the current {@link MCRUser} for {@link MCRObjectID}.
     *
     * @param objectId the {@link MCRObjectID}
     * @param value the value of a {@link MCRAccessKey}
     * @throws MCRException if there is no matching {@link MCRAccessKey} with the same value.
     */
    public static synchronized void addAccessKey(final MCRObjectID objectId, final String value) 
        throws MCRException {
        final MCRUser user = MCRUserManager.getCurrentUser();
        addAccessKey(user, objectId, value);
    }

    /**
     * Adds the value of a {@link MCRAccessKey} as user attribute to a {@link MCRUser} for a {@link MCRObjectID}.
     *
     * @param user the {@link MCRUser} the value should assigned
     * @param objectId the {@link MCRObjectID}
     * @param value the value of a {@link MCRAccessKey}
     * @throws MCRException if there is no matching {@link MCRAccessKey} with the same value.
     */
    public static synchronized void addAccessKey(final MCRUser user, final MCRObjectID objectId, final String value) 
        throws MCRException {

        final String encryptedValue = MCRAccessKeyManager.encryptValue(value, objectId);
        final MCRAccessKey accessKey = MCRAccessKeyManager.getAccessKeyByValue(objectId, value);
        if (accessKey == null) {
            throw new MCRAccessKeyNotFoundException("Key does not exists.");
        }

        user.setUserAttribute(ACCESS_KEY_PREFIX + objectId, encryptedValue);
        MCRUserManager.updateUser(user);

        MCRAccessManager.invalidPermissionCache(objectId.toString(), accessKey.getType());
        MCRSessionMgr.getCurrentSession().setUserInformation(user.clone());
    }

    /**
     * Deletes access key value user attribute from current {@link MCRUser} for a {@link MCRObjectID}.
     *
     * @param objectId the {@link MCRObjectID}
     */
    public static synchronized void deleteAccessKey(final MCRObjectID objectId) {
        deleteAccessKey(MCRUserManager.getCurrentUser(), objectId);
    }

    /**
     * Deletes the access key value user attribute from given {@link MCRUser} for {@link MCRObjectID}.
     *
     * @param user the {@link MCRUser}
     * @param objectId the {@link MCRObjectID}
     */
    public static synchronized void deleteAccessKey(final MCRUser user, final MCRObjectID objectId) {
        user.getAttributes().removeIf(ua -> ua.getName().equals(ACCESS_KEY_PREFIX + objectId.toString()));
        MCRUserManager.updateUser(user);
        MCRAccessManager.invalidPermissionCache(objectId.toString(), MCRAccessManager.PERMISSION_READ);
        MCRAccessManager.invalidPermissionCache(objectId.toString(), MCRAccessManager.PERMISSION_WRITE);
    }

    /**
     * Fetches access key value from user attribute for a {@link MCRObjectID}.
     *
     * @param objectId the {@link MCRObjectID}
     * @return value or null
     */
    public static synchronized String getUserAccessKeyValue(MCRObjectID objectId) {
        return MCRSessionMgr.getCurrentSession().getUserInformation()
            .getUserAttribute(ACCESS_KEY_PREFIX + objectId.toString());
    }

    /**
     * Fetches access key from user attribute for a {@link MCRObjectID}.
     *
     * @param objectId the {@link MCRObjectID}
     * @return {@link MCRAccessKey} or null
     */
    public static synchronized MCRAccessKey getAccessKey(MCRObjectID objectId) {
        final String userKey = getUserAccessKeyValue(objectId);
        if (userKey != null) {
            return MCRAccessKeyManager.getAccessKeyByValue(objectId, userKey);
        }
        return null;
    }
}
