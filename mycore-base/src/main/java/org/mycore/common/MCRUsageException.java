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
 * Instances of MCRUsageException are thrown when the MyCoRe API is used in an
 * illegal way. For example, this could happen when you provide illegal
 * arguments to a method.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRUsageException extends MCRException {
    /**
     * Creates a new MCRUsageException with an error message
     * 
     * @param message
     *            the error message for this exception
     */
    public MCRUsageException(String message) {
        super(message);
    }

    /**
     * Creates a new MCRUsageException with an error message and a reference to
     * an exception thrown by an underlying system.
     * 
     * @param message
     *            the error message for this exception
     * @param exception
     *            the exception that was thrown by an underlying system
     */
    public MCRUsageException(String message, Exception exception) {
        super(message, exception);
    }
}
