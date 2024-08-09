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

package org.mycore.mcr.acl.accesskey.validation;

import java.util.Objects;

import org.mycore.access.MCRAccessManager;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyValidationException;

// TODO may use jakarta.validation
/**
 * Default implementation of the {@link MCRAccessKeyValidator} interface.
 *
 * @see org.mycore.mcr.acl.accesskey.validation.MCRAccessKeyValidator
 */
public class MCRAccessKeyValidatorImpl implements MCRAccessKeyValidator {

    protected MCRAccessKeyValidatorImpl() {
    }

    /**
     * Returns single instance.
     *
     * @return the single instance
     */
    public static MCRAccessKeyValidatorImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    @Override
    public void validateAccessKeyDto(MCRAccessKeyDto accessKeyDto) {
        if (accessKeyDto.getValue() == null || !checkValue(accessKeyDto.getValue())) {
            throw new MCRAccessKeyValidationException("A valid value is required");
        }
        if (accessKeyDto.getPermission() == null || !checkPermission(accessKeyDto.getPermission())) {
            throw new MCRAccessKeyValidationException("A valid permission is required");
        }
        if (accessKeyDto.getReference() == null || !checkReference(accessKeyDto.getReference())) {
            throw new MCRAccessKeyValidationException("A valid reference is required");
        }
    }

    @Override
    public void validateAccessKeyPartialUpdateDto(MCRAccessKeyPartialUpdateDto accessKeyDto) {
        if (accessKeyDto.getValue().isPresent() && (accessKeyDto.getValue().get() == null
            || !checkValue(accessKeyDto.getValue().get()))) {
            throw new MCRAccessKeyValidationException("A valid value is required");
        }
        if (accessKeyDto.getPermission().isPresent() && (accessKeyDto.getPermission().get() == null
            || !checkPermission(accessKeyDto.getPermission().get()))) {
            throw new MCRAccessKeyValidationException("A valid permission is required");
        }
        if (accessKeyDto.getReference().isPresent() && (accessKeyDto.getReference().get() == null
            || !checkReference(accessKeyDto.getReference().get()))) {
            throw new MCRAccessKeyValidationException("A valid reference is required");
        }
    }

    /**
     * Checks value.
     *
     * @param value the value
     * @return true is value is valid
     */
    protected static boolean checkValue(String value) {
        return value.trim().length() > 0;
    }

    /**
     * Checks reference.
     *
     * @param reference the reference
     * @return true if reference is valid
     */
    protected static boolean checkReference(String reference) {
        return reference.trim().length() > 0;
    }

    /**
     * Checks permission.
     *
     * @param permission the permission
     * @return true if permission is valid
     */
    protected static boolean checkPermission(String permission) {
        return Objects.equals(MCRAccessManager.PERMISSION_WRITE, permission)
            || Objects.equals(MCRAccessManager.PERMISSION_READ, permission);
    }

    private static class InstanceHolder {
        static final MCRAccessKeyValidatorImpl INSTANCE = new MCRAccessKeyValidatorImpl();
    }

}
