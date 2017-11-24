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

package org.mycore.common.config;

import org.mycore.common.MCRException;

/**
 * Instances of this class represent an exception thrown because of an error in the MyCoRe configuration. Normally this
 * will be the case when a configuration property that is required is not set or has an illegal value.
 * 
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRConfigurationException extends MCRException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new MCRConfigurationException with an error message
     * 
     * @param message
     *            the error message for this exception
     */
    public MCRConfigurationException(String message) {
        super(message);
    }

    /**
     * Creates a new MCRConfigurationException with an error message and a reference to an exception thrown by an
     * underlying system.
     * 
     * @param message
     *            the error message for this exception
     * @param cause
     *            the exception that was thrown by an underlying system
     */
    public MCRConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
