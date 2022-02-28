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

package org.mycore.iiif.common;

import static org.mycore.frontend.MCRFrontendUtil.BASE_URL_ATTRIBUTE;

import java.io.IOException;

import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;

public class MCRIIIFBaseURLFilter implements ContainerRequestFilter {

    @Context
    private HttpServletRequest httpRequest;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // set BASE_URL_ATTRIBUTE to MCRSession
        if (httpRequest.getAttribute(BASE_URL_ATTRIBUTE) != null) {
            final MCRSession currentSession = MCRSessionMgr.getCurrentSession();
            if (currentSession != null) {
                currentSession.put(BASE_URL_ATTRIBUTE, httpRequest.getAttribute(BASE_URL_ATTRIBUTE));
            }
        }
    }
}
