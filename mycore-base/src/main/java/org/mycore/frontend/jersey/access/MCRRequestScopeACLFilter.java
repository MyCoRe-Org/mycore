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

package org.mycore.frontend.jersey.access;

import java.util.function.Supplier;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;

@Priority(Priorities.AUTHENTICATION - 1)
public class MCRRequestScopeACLFilter implements ContainerRequestFilter {

    public static final String ACL_INSTANT_KEY = "requestScopeACL";

    private static final MCRRequestScopeACLFactory ACL_FACTORY = new MCRRequestScopeACLFactory();

    @Override
    public void filter(ContainerRequestContext requestContext) {
        requestContext.setProperty(ACL_INSTANT_KEY, (Supplier<MCRRequestScopeACL>) ACL_FACTORY::provide);
    }

}
