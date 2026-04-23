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
 * Thrown when user data fails validation.
 */
public class MCRUserValidationException extends MCRUserException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new exception for the given user ID and reason.
     *
     * @param userId the ID of the user that failed validation
     * @param message the message why validation failed
     * @param cause the cause
     */
    public MCRUserValidationException(String userId, String message, Throwable cause) {
        super("User '" + userId + "' validation failed: " + message, cause);
    }

    /**
     * Creates a new exception for reason.
     *
     * @param message the message why validation failed
     * @param cause the cause
     */
    public MCRUserValidationException(String message, Throwable cause) {
        super("User validation failed: " + message, cause);
    }
}
