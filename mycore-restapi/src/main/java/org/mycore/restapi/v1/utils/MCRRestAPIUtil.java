/* 
 * $Revision: 34285 $ $Date: 2016-01-07 14:05:50 +0100 (Do, 07 Jan 2016) $
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 */
package org.mycore.restapi.v1.utils;

import java.net.UnknownHostException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response.Status;

import org.mycore.access.mcrimpl.MCRAccessControlSystem;
import org.mycore.access.mcrimpl.MCRAccessRule;
import org.mycore.access.mcrimpl.MCRIPAddress;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUserInformation;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.restapi.v1.errors.MCRRestAPIError;
import org.mycore.restapi.v1.errors.MCRRestAPIException;
import org.mycore.user2.MCRUserManager;

/**
 * This class contains some generic utility functions for the REST API
 * 
 * @author Robert Stephan
 */
public class MCRRestAPIUtil {

    /**
     * The REST API access permissions (read, write)
     */
    public enum MCRRestAPIACLPermission {
        READ {
            public String toString() {
                return "read";
            }
        },

        WRITE {
            public String toString() {
                return "write";
            }
        }
    }

    /**
     * checks if the given REST API operation is allowed
     * @param request - the HTTP request
     * @param permission "read" or "write"
     * @param path - the REST API path, e.g. /v1/messages
     * 
     * @throws MCRRestAPIException if access is restricted
     */
    public static void checkRestAPIAccess(HttpServletRequest request, MCRRestAPIACLPermission permission, String path)
        throws MCRRestAPIException {
        //save the current user and set REST API user into session, 
        //because ACL System can only validate the current user in session.
        MCRUserInformation oldUser = MCRSessionMgr.getCurrentSession().getUserInformation();
        try {
            String userID = MCRJSONWebTokenUtil.retrieveUsernameFromAuthenticationToken(request);
            if (userID != null) {
                if (MCRSystemUserInformation.getGuestInstance().getUserID().equals(userID)) {
                    MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.getGuestInstance());
                } else {
                    MCRSessionMgr.getCurrentSession().setUserInformation(MCRUserManager.getUser(userID));
                }
            }
            MCRIPAddress theIP = new MCRIPAddress(MCRFrontendUtil.getRemoteAddr(request));
            String thePath = path.startsWith("/") ? path : "/" + path;

            boolean hasAPIAccess = ((MCRAccessControlSystem) MCRAccessControlSystem.instance()).checkAccess("restapi:/",
                permission.toString(), userID, theIP);
            if (hasAPIAccess) {
                MCRAccessRule rule = (MCRAccessRule) MCRAccessControlSystem.instance()
                    .getAccessRule("restapi:" + thePath, permission.toString());
                if (rule != null) {
                    if (rule.checkAccess(userID, new Date(), theIP)) {
                        return;
                    }
                } else {
                    return;
                }
            }
        } catch (UnknownHostException e) {
            // ignore
        } finally {
            MCRSessionMgr.getCurrentSession().setUserInformation(oldUser);
        }
        throw new MCRRestAPIException(Status.FORBIDDEN,
            new MCRRestAPIError(MCRRestAPIError.CODE_ACCESS_DENIED, "REST-API action is not allowed.",
                "Check access right '" + permission + "' on ACLs 'restapi:/' and 'restapi:" + path + "'!"));
    }
}
