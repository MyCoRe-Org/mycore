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

package org.mycore.datamodel.metadata.validator;

/**
 * This class represents the result of a validation process for MCR objects.
 * It provides methods to check if the validation was successful and to retrieve
 * any associated message. The class also includes a static instance representing a valid result.
 * @see #VALID
 */
public abstract class MCRValidationResult {

    /**
     * This method checks if the validation was successful.
     * @return true if the validation was successful, false otherwise
     */
    public abstract boolean isValid();

    /**
     * This method retrieves the message associated with the validation result. Should only be called if isValid()
     * returns false.
     * @return the message associated with the validation result or an empty string if the validation was successful.
     */
    public abstract String getMessage();

    public static final MCRValidationResult VALID = new MCRValidationResult() {
        @Override
        public String getMessage() {
            return "";
        }

        @Override
        public boolean isValid() {
            return true;
        }
    };
}
