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

package org.mycore.restapi.v1.errors;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.restapi.v1.utils.MCRRestAPIUtil;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

/**
 * Maps response status {@link jakarta.ws.rs.core.Response.Status#FORBIDDEN} to
 * {@link jakarta.ws.rs.core.Response.Status#UNAUTHORIZED} if current user is guest.
 */
public class MCRForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {
    @Context
    Application app;

    public Response toResponse(ForbiddenException ex) {
        String userID = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
        if (userID.equals(MCRSystemUserInformation.getGuestInstance().getUserID())) {
            LogManager.getLogger().warn("Guest detected");
            return Response.fromResponse(ex.getResponse())
                .status(Response.Status.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, MCRRestAPIUtil.getWWWAuthenticateHeader("Basic", null, app))
                .build();
        }
        return ex.getResponse();
    }
}
