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

package org.mycore.ocfl;

import java.io.Serial;

import org.mycore.common.MCRException;

/**
 * Represents an exception specific to the OCFL (Oxford Common File Layout).
 * <p>
 * This exception extends {@link MCRException} and is used to indicate errors related to OCFL operations
 * within the MyCoRe framework. It supports standard exception messaging and cause chaining.
 * </p>
 *
 * @see MCRException
 */
public class MCROCFLException extends MCRException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code MCROCFLException} with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception.
     */
    public MCROCFLException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code MCROCFLException} with the specified cause.
     *
     * @param cause the underlying cause of this exception.
     */
    public MCROCFLException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code MCROCFLException} with the specified detail message and cause.
     *
     * @param message the detail message explaining the reason for the exception.
     * @param cause the underlying cause of this exception.
     */
    public MCROCFLException(String message, Throwable cause) {
        super(message, cause);
    }

}
