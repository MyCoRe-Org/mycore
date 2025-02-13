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

package org.mycore.common.digest;

import org.mycore.common.MCRException;

import java.io.Serial;

/**
 * Exception thrown when an error occurs in digest validation.
 */
public class MCRDigestValidationException extends MCRException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new digest validation exception with the specified detail message.
     *
     * @param message The detail message.
     */
    public MCRDigestValidationException(String message) {
        super(message);
    }

}
