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
 * Thrown when the caller is not permitted to write the password of a user, e.g. because the
 * user is locked and owned by someone else.
 */
public class MCRUserPasswordNotAllowedException extends MCRUserException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new exception for the given user ID.
     *
     * @param userId the ID of the user whose password must not be written
     * @param cause the cause
     */
    public MCRUserPasswordNotAllowedException(String userId, Throwable cause) {
        super("Not allowed to change password of user '" + userId + "'", cause);
    }
}
