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

import org.apache.commons.lang3.StringUtils;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyValidationException;

/**
 * Default implementation of the {@link MCRAccessKeyValidator} interface.
 *
 * @see org.mycore.mcr.acl.accesskey.validation.MCRAccessKeyValidator
 */
public class MCRAccessKeyValidatorImpl implements MCRAccessKeyValidator {

    @Override
    public void validateAccessKeyDto(MCRAccessKeyDto accessKeyDto) {
        validateSecret(accessKeyDto.getSecret());
        validatePermission(accessKeyDto.getPermission());
        validateReference(accessKeyDto.getReference());
    }

    @Override
    public void validateAccessKeyPartialUpdateDto(MCRAccessKeyPartialUpdateDto accessKeyDto) {
        if (accessKeyDto.getSecret().isPresent()) {
            validateSecret(accessKeyDto.getSecret().get());
        }
        if (accessKeyDto.getPermission().isPresent()) {
            validatePermission(accessKeyDto.getPermission().get());
        }
        if (accessKeyDto.getReference().isPresent()) {
            validateReference(accessKeyDto.getReference().get());
        }
    }

    /**
     * Validates secret.
     *
     * @param secret the secret
     * @throws MCRAccessKeyValidationException if secret is invalid
     */
    protected static void validateSecret(String secret) {
        if (StringUtils.isBlank(secret)) {
            throw new MCRAccessKeyValidationException("A valid secret is required");
        }
    }

    /**
     * Validates reference.
     *
     * @param reference the reference
     * @throws MCRAccessKeyValidationException if reference is invalid
     */
    protected static void validateReference(String reference) {
        if (StringUtils.isBlank(reference)) {
            throw new MCRAccessKeyValidationException("A valid permission is required");
        }
    }

    /**
     * Validates permission.
     *
     * @param permission the permission
     * @throws MCRAccessKeyValidationException if permission is invalid
     */
    protected static void validatePermission(String permission) {
        if (StringUtils.isBlank(permission)) {
            throw new MCRAccessKeyValidationException("A valid permission is required");
        }
    }

}
