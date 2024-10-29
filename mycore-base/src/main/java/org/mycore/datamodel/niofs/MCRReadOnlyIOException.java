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

package org.mycore.datamodel.niofs;

import java.io.IOException;

/**
 * Thrown to indicate that an operation attempted to modify a read-only file or directory.
 */
public class MCRReadOnlyIOException extends IOException {

    /**
     * Constructs a new read-only exception with the specified detail message.
     * The message can be retrieved later by the {@link Throwable#getMessage()} method.
     *
     * @param message The detailed message which explains the reason the exception is thrown.
     */
    public MCRReadOnlyIOException(String message) {
        super(message);
    }

    /**
     * Constructs a new read-only exception with the specified cause. The cause is the
     * underlying reason for this exception.
     *
     * @param cause The cause (which is saved for later retrieval by the {@link Throwable#getCause()} method).
     *              A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public MCRReadOnlyIOException(Throwable cause) {
        super(cause);
    }

}
