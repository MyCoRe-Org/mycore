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

import java.util.Arrays;

import jakarta.ws.rs.HttpMethod;

import org.mycore.access.MCRAccessManager;

/**
 * The REST API access permissions (read, write, delete)
 */
public enum MCRRestAPIACLPermission {
    READ(MCRAccessManager.PERMISSION_READ), WRITE(MCRAccessManager.PERMISSION_WRITE),
    DELETE(MCRAccessManager.PERMISSION_DELETE);

    private final String value;

    MCRRestAPIACLPermission(final String value) {
        this.value = value;
    }

    public static MCRRestAPIACLPermission resolve(final String permission) {
        return Arrays.stream(values())
            .filter(object -> object.value.equalsIgnoreCase(permission))
            .findFirst()
            .orElse(null);
    }

    @Override
    public String toString() {
        return this.value;
    }

    public static MCRRestAPIACLPermission fromMethod(final String method) {
        return switch (method) {
            case HttpMethod.GET, HttpMethod.HEAD -> READ;
            case HttpMethod.DELETE -> DELETE;
            case HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH -> WRITE;
            default -> null;
        };
    }
}
