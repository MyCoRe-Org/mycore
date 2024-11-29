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

package org.mycore.common;

/**
 * Exception thrown to indicate an error during transaction handling in the
 * {@link MCRTransactionManager} or any other transaction management component.
 *
 * <p>This exception is typically thrown when a transaction cannot be started, committed,
 * or rolled back due to an underlying issue, such as a database connection failure
 * or an illegal transaction state.</p>
 */
public class MCRTransactionException extends MCRException {

    /**
     * Constructs a new {@code MCRTransactionException} with the specified detail message.
     *
     * @param message the detail message, which provides information about the error.
     */
    public MCRTransactionException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code MCRTransactionException} with the specified detail message and cause.
     *
     * @param message the detail message, which provides information about the error.
     * @param cause the cause of the error, which may be another throwable.
     */
    public MCRTransactionException(String message, Throwable cause) {
        super(message, cause);
    }

}
