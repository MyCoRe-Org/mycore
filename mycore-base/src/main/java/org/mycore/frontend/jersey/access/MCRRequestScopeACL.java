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

import java.util.Objects;
import java.util.function.Supplier;

import org.mycore.access.MCRAccessInterface;
import org.mycore.common.MCRUserInformation;

import jakarta.ws.rs.container.ContainerRequestContext;

public interface MCRRequestScopeACL extends MCRAccessInterface {

    boolean isPrivate();

    static MCRRequestScopeACL extractFromRequestContext(ContainerRequestContext requestContext) {
        Object property = requestContext.getProperty(MCRRequestScopeACLFilter.ACL_INSTANT_KEY);
        Objects.requireNonNull(property, "Please register " + MCRRequestScopeACLFilter.class);
        if (property instanceof Supplier) {
            @SuppressWarnings("unchecked")
            MCRRequestScopeACL requestScopeACL = ((Supplier<MCRRequestScopeACL>) property).get();
            requestContext.setProperty(MCRRequestScopeACLFilter.ACL_INSTANT_KEY, requestScopeACL);
            property = requestScopeACL;
        }
        return (MCRRequestScopeACL) property;
    }

    @Override
    default boolean checkPermissionForUser(String permission, MCRUserInformation userInfo) {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean checkPermission(String id, String permission, MCRUserInformation userInfo) {
        throw new UnsupportedOperationException();
    }

}
