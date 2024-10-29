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

package org.mycore.restapi.v2.access;

import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.access.MCRRuleAccessInterface;

/**
 * The {@code MCRRestAccessManager} class manages access to REST API resources.
 */
public class MCRRestAccessManager {

    private static final String REST_API_OBJECT_ID = "restapi:/";

    /**
     * Checks access to the REST API based on general and specific permissions and rules.
     *
     * This method evaluates both general permissions and specific permissions for a given
     * object identified by its path. First, it evaluates the rules for general and specific permissions.
     * If no specific rule for the object exists, only the general permission is considered.
     * If no general rule exists, the specific permission will determine access.
     * If both rules are present, both permissions must be granted.
     *
     * @param aclProvider an {@code MCRAccessInterface} used to check permissions
     * @param permission the permission to be checked
     * @param path the path of the REST API resource for which access is being checked
     * @return {@code true} if access is granted, otherwise {@code false}.
     */
    public static boolean checkRestAPIAccess(MCRAccessInterface aclProvider, String permission, String path) {
        final MCRAccessInterface acl = MCRAccessManager.getAccessImpl();
        final String objectId = getObjectId(path);
        final boolean hasGeneralPermission = aclProvider.checkPermission(REST_API_OBJECT_ID, permission);
        // check has no specific rule
        if (!checkHasRule(acl, objectId, permission)) {
            return hasGeneralPermission;
        }
        final boolean hasSpecificPermission = aclProvider.checkPermission(objectId, permission);
        // check has no general rule
        if (!checkHasRule(acl, REST_API_OBJECT_ID, permission)) {
            return hasSpecificPermission;
        }
        return hasGeneralPermission && hasSpecificPermission;
    }

    private static boolean checkHasRule(MCRAccessInterface aclProvider, String objectId, String permission) {
        return !(aclProvider instanceof MCRRuleAccessInterface ruleAccess) || ruleAccess.hasRule(objectId, permission);
    }

    private static String getObjectId(String path) {
        return path.startsWith("/") ? REST_API_OBJECT_ID + path.substring(1) : REST_API_OBJECT_ID + path;
    }

}
