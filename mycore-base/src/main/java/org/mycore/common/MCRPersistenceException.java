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

package org.mycore.common;

/**
 * Instances of this class represent a general exception thrown by the
 * persistency layer of the MyCoRe implementation. This will be the case when
 * the datastore reports an error, for example.
 * 
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRPersistenceException extends MCRException {
    /**
     * Creates a new MCRPersistenceException with an error message
     * 
     * @param message
     *            the error message for this exception
     */
    public MCRPersistenceException(String message) {
        super(message);
    }

    /**
     * Creates a new MCRPersistenceException with an error message and a
     * reference to an exception thrown by an underlying system.
     * 
     * @param message
     *            the error message for this exception
     * @param exception
     *            the exception that was thrown by an underlying system
     */
    public MCRPersistenceException(String message, Exception exception) {
        super(message, exception);
    }
}
