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

package org.mycore.orcid2.exception;

/**
 * This exception concerns errors when a work already exists in a ORCID profile.
 */
public class MCRORCIDWorkAlreadyExistsException extends MCRORCIDException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new MCRORCIDWorkAlreadyExistsException with default message.
     */
    public MCRORCIDWorkAlreadyExistsException() {
        super("Work already exists");
    }

    /**
     * Creates a new MCRORCIDWorkAlreadyExistsException with default message and cause.
     * 
     * @param cause the cause
     */
    public MCRORCIDWorkAlreadyExistsException(Throwable cause) {
        super("Work already exists", cause);
    }
}
