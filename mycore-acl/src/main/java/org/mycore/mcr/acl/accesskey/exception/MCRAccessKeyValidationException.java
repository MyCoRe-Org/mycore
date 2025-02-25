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

package org.mycore.mcr.acl.accesskey.exception;

import java.io.Serial;

/**
 * Exception thrown when an access key validation fails.
 *
 * The {@code AccessKeyValidationException} is a specialized {@link MCRAccessKeyException} that
 * indicates an issue with the validation of an access key. This exception is typically used
 * to signal that the provided data for an access key does not meet the required constraints or rules.
 */
public class MCRAccessKeyValidationException extends MCRAccessKeyException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code AccessKeyValidationException} with the specified detail message.
     *
     * @param errorMessage the detail message explaining the reason for the exception
     */
    public MCRAccessKeyValidationException(String errorMessage) {
        super(errorMessage);
    }

}
