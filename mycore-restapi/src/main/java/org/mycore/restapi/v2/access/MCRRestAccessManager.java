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
     * Checks access to a REST API resource based on a given permission and path.
     *
     * This method first checks whether a rule applies to the entire REST API
     * ({@code REST_API_OBJECT_ID}). If this rule exists and the required permission is granted,
     * it proceeds to check whether a specific rule exists for the given path.
     * Based on the rules, it determines whether access to the path is allowed or not.
     *
     * @param aclProvider an {@code MCRAccessInterface} used to check permissions
     * @param permission the permission to be checked
     * @param path the path of the REST API resource for which access is being checked
     * @return {@code true} if access is granted, otherwise {@code false}.
     */
    public static boolean checkRestAPIAccess(MCRAccessInterface aclProvider, String permission, String path) {
        final MCRAccessInterface acl = MCRAccessManager.getAccessImpl(); // TODO use aclProvider instead?
        final String objectId = getObjectId(path);
        if ((acl instanceof MCRRuleAccessInterface ruleAccess) && !ruleAccess.hasRule(REST_API_OBJECT_ID, permission)
            && ruleAccess.hasRule(objectId, permission)) {
            return aclProvider.checkPermission(objectId, permission);
        }
        if (!aclProvider.checkPermission(REST_API_OBJECT_ID, permission)) {
            return false;
        }
        return checkHasRule(acl, objectId, permission) ? aclProvider.checkPermission(objectId, permission) : true;
    }

    private static boolean checkHasRule(MCRAccessInterface aclProvider, String objectId, String permission) {
        return !(aclProvider instanceof MCRRuleAccessInterface ruleAccess) || ruleAccess.hasRule(objectId, permission);
    }

    private static String getObjectId(String path) {
        return path.startsWith("/") ? REST_API_OBJECT_ID + path.substring(1) : REST_API_OBJECT_ID + path;
    }

}
