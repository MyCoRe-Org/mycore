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

package org.mycore.mcr.acl.accesskey.validation;

import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyValidationException;

/**
 * Interface for validating access keys.
 * This interface defines methods for validating access keys. Implementations of this interface
 * should provide functionality to check whether a given access key is valid based on the specific
 * criteria or rules of the application.
 */
public interface MCRAccessKeyValidator {

    /**
     * Validates the given {@link MCRAccessKeyDto}.
     *
     * @param accessKeyDto the access key DTO to be validated
     * @throws MCRAccessKeyValidationException if the access key DTO is invalid
     */
    void validateAccessKeyDto(MCRAccessKeyDto accessKeyDto);

    /**
     * Validates the given {@link MCRAccessKeyPartialUpdateDto}.
     *
     * @param accessKeyDto the access key DTO to be validated
     * @throws MCRAccessKeyValidationException if the access key DTO is invalid
     */
    void validateAccessKeyPartialUpdateDto(MCRAccessKeyPartialUpdateDto accessKeyDto);

}
