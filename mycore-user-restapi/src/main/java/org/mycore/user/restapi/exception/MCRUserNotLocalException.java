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

package org.mycore.user.restapi.exception;

import java.io.Serial;

/**
 * Thrown when a self-service change is attempted for a user who is not in the local realm.
 * <p>
 * The data of users authenticated via an external realm (LDAP, Shibboleth, CAS, ...) is
 * managed by that realm and must not be changed here.
 */
public class MCRUserNotLocalException extends MCRUserException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new exception for the given user ID.
     *
     * @param userId the ID of the user who is not in the local realm
     */
    public MCRUserNotLocalException(String userId) {
        super("User '" + userId + "' is not in the local realm; its data is managed by its realm");
    }
}
