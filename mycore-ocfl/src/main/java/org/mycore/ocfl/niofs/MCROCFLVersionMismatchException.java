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

package org.mycore.ocfl.niofs;

import java.io.Serial;

import org.mycore.common.MCRException;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.MCROCFLException;

/**
 * This exception is thrown when there is a mismatch between the expected 
 * and actual owner or version of an {@link MCRVersionedPath}.
 *
 * <p>The exception is typically triggered when attempting to perform operations 
 * on a {@code MCRVersionedPath} that belongs to a different owner or version 
 * than what is expected.</p>
 *
 * @see MCRVersionedPath
 * @see MCRException
 */
public class MCROCFLVersionMismatchException extends MCROCFLException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message, explaining the cause of the exception.
     */
    public MCROCFLVersionMismatchException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param cause the cause of the exception, which can be another throwable.
     */
    public MCROCFLVersionMismatchException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message, explaining the cause of the exception.
     * @param cause   the cause of the exception, which can be another throwable.
     */
    public MCROCFLVersionMismatchException(String message, Throwable cause) {
        super(message, cause);
    }

}
