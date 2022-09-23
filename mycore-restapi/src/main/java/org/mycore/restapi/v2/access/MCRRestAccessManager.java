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

public class MCRRestAccessManager {

    private static final String REST_API_OBJECT_ID = "restapi:/";

    public static boolean checkRestAPIAccess(final MCRAccessInterface aclProvider,
        final MCRRestAPIACLPermission permission, final String path) {
        final MCRAccessInterface acl = MCRAccessManager.getAccessImpl();
        final String permStr = permission.toString();
        if (aclProvider.checkPermission(REST_API_OBJECT_ID, permStr)) {
            final String objectId = path.startsWith("/") ? REST_API_OBJECT_ID + path.substring(1)
                : REST_API_OBJECT_ID + path;
            if (!(acl instanceof MCRRuleAccessInterface)
                || ((MCRRuleAccessInterface) acl).hasRule(objectId, permStr)) {
                if (aclProvider.checkPermission(objectId, permStr)) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }
}
