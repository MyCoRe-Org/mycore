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

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;
import org.mycore.mcr.acl.accesskey.dto.util.MCRNullable;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyValidationException;

public class MCRAccessKeyValidatorImplTest {

    private static final String TEST_REFERENCE = "testReference";

    private static final String TEST_PERMISSION = "writedb";

    private static final String TEST_SECRET = "testSecret";

    @Test
    public void testValidateAccessKeyDto() {
        final MCRAccessKeyValidatorImpl validator = new MCRAccessKeyValidatorImpl();
        final MCRAccessKeyDto accessKeyDto = new MCRAccessKeyDto();
        accessKeyDto.setPermission(TEST_PERMISSION);
        accessKeyDto.setReference(TEST_REFERENCE);
        accessKeyDto.setSecret(TEST_SECRET);
        validator.validateAccessKeyDto(accessKeyDto);
    }

    @Test
    public void testValidateAccessKeyDto_noReference() {
        assertThrows(
            MCRAccessKeyValidationException.class,
            () -> {
                final MCRAccessKeyValidatorImpl validator = new MCRAccessKeyValidatorImpl();
                final MCRAccessKeyDto accessKeyDto = new MCRAccessKeyDto();
                accessKeyDto.setPermission(TEST_PERMISSION);
                accessKeyDto.setSecret(TEST_SECRET);
                validator.validateAccessKeyDto(accessKeyDto);
            });
    }

    @Test
    public void testValidateAccessKeyDto_noPermission() {
        assertThrows(
            MCRAccessKeyValidationException.class,
            () -> {
                final MCRAccessKeyValidatorImpl validator = new MCRAccessKeyValidatorImpl();
                final MCRAccessKeyDto accessKeyDto = new MCRAccessKeyDto();
                accessKeyDto.setReference(TEST_REFERENCE);
                accessKeyDto.setSecret(TEST_SECRET);
                validator.validateAccessKeyDto(accessKeyDto);
            });
    }

    @Test
    public void testValidateAccessKeyDto_noValue() {
        assertThrows(
            MCRAccessKeyValidationException.class,
            () -> {
                final MCRAccessKeyValidatorImpl validator = new MCRAccessKeyValidatorImpl();
                final MCRAccessKeyDto accessKeyDto = new MCRAccessKeyDto();
                accessKeyDto.setPermission(TEST_PERMISSION);
                accessKeyDto.setReference(TEST_REFERENCE);
                validator.validateAccessKeyDto(accessKeyDto);
            });
    }

    @Test
    public void testValidateAccessKeyPartialUpdateDto() {
        final MCRAccessKeyValidatorImpl validator = new MCRAccessKeyValidatorImpl();
        final MCRAccessKeyPartialUpdateDto accessKeyDto = new MCRAccessKeyPartialUpdateDto();
        accessKeyDto.setSecret(new MCRNullable<>(TEST_SECRET));
        accessKeyDto.setReference(new MCRNullable<>(TEST_REFERENCE));
        accessKeyDto.setPermission(new MCRNullable<>(TEST_PERMISSION));
        validator.validateAccessKeyPartialUpdateDto(accessKeyDto);
    }

    @Test
    public void testValidateAccessKeyPartialUpdateDto_valueNull() {
        assertThrows(
            MCRAccessKeyValidationException.class,
            () -> {
                final MCRAccessKeyValidatorImpl validator = new MCRAccessKeyValidatorImpl();
                final MCRAccessKeyPartialUpdateDto accessKeyDto = new MCRAccessKeyPartialUpdateDto();
                accessKeyDto.setSecret(new MCRNullable<>(null));
                accessKeyDto.setReference(new MCRNullable<>(TEST_REFERENCE));
                accessKeyDto.setPermission(new MCRNullable<>(TEST_PERMISSION));
                validator.validateAccessKeyPartialUpdateDto(accessKeyDto);
            });
    }

    @Test
    public void testValidateAccessKeyPartialUpdateDto_permissionNull() {
        assertThrows(
            MCRAccessKeyValidationException.class,
            () -> {
                final MCRAccessKeyValidatorImpl validator = new MCRAccessKeyValidatorImpl();
                final MCRAccessKeyPartialUpdateDto accessKeyDto = new MCRAccessKeyPartialUpdateDto();
                accessKeyDto.setSecret(new MCRNullable<>(TEST_SECRET));
                accessKeyDto.setReference(new MCRNullable<>(TEST_REFERENCE));
                accessKeyDto.setPermission(new MCRNullable<>(null));
                validator.validateAccessKeyPartialUpdateDto(accessKeyDto);
            });
    }

    @Test
    public void testValidateAccessKeyPartialUpdateDto_referenceNull() {
        assertThrows(
            MCRAccessKeyValidationException.class,
            () -> {
                final MCRAccessKeyValidatorImpl validator = new MCRAccessKeyValidatorImpl();
                final MCRAccessKeyPartialUpdateDto accessKeyDto = new MCRAccessKeyPartialUpdateDto();
                accessKeyDto.setSecret(new MCRNullable<>(TEST_SECRET));
                accessKeyDto.setReference(new MCRNullable<>(null));
                accessKeyDto.setPermission(new MCRNullable<>(TEST_PERMISSION));
                validator.validateAccessKeyPartialUpdateDto(accessKeyDto);
            });
    }

    @Test
    public void testValidatePermission_valid() {
        MCRAccessKeyValidatorImpl.validatePermission("read");
    }

    @Test
    public void testValidatePermission_blank() {
        assertThrows(
            MCRAccessKeyValidationException.class,
            () -> MCRAccessKeyValidatorImpl.validatePermission(""));
    }

    @Test
    public void testValidateSecret_valid() {
        MCRAccessKeyValidatorImpl.validateSecret(TEST_SECRET);
    }

    @Test
    public void testValidateSecret_blank() {
        assertThrows(
            MCRAccessKeyValidationException.class,
            () -> MCRAccessKeyValidatorImpl.validateSecret(""));
    }

    @Test
    public void testValidateReference_valid() {
        MCRAccessKeyValidatorImpl.validateReference(TEST_REFERENCE);
    }

    @Test
    public void testValidateReference_blank() {
        assertThrows(
            MCRAccessKeyValidationException.class,
            () -> MCRAccessKeyValidatorImpl.validateReference(""));
    }

}
