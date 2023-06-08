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

import org.mycore.common.MCRException;

/**
 * General mycore-orcid2 exception.
 */
public class MCRORCIDException extends MCRException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new MCRORCIDException with message.
     * 
     * @param message the message
     */
    public MCRORCIDException(String message) {
        super(message);
    }

    /**
     * Creates a new MCRORCIDException with message and cause.
     * 
     * @param message the message
     * @param cause the cause
     */
    public MCRORCIDException(String message, Throwable cause) {
        super(message, cause);
    }
}
