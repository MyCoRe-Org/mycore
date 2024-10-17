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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;
import org.mycore.mcr.acl.accesskey.dto.util.MCRNullable;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyValidationException;

public class MCRAccessKeyValidatorImplTest {

    private static final String TEST_REFERENCE = "testReference";

    private static final String TEST_PERMISSION = "writedb";

    private static final String TEST_VALUE = "testValue";

    @Test
    public void testValidateAccessKeyDto() {
        final MCRAccessKeyValidatorImpl validator = new MCRAccessKeyValidatorImpl();
        final MCRAccessKeyDto accessKeyDto = new MCRAccessKeyDto();
        accessKeyDto.setPermission(TEST_PERMISSION);
        accessKeyDto.setReference(TEST_REFERENCE);
        accessKeyDto.setValue(TEST_VALUE);
        validator.validateAccessKeyDto(accessKeyDto);
    }

    @Test(expected = MCRAccessKeyValidationException.class)
    public void testValidateAccessKeyDto_noReference() {
        final MCRAccessKeyValidatorImpl validator = new MCRAccessKeyValidatorImpl();
        final MCRAccessKeyDto accessKeyDto = new MCRAccessKeyDto();
        accessKeyDto.setPermission(TEST_PERMISSION);
        accessKeyDto.setValue(TEST_VALUE);
        validator.validateAccessKeyDto(accessKeyDto);
    }

    @Test(expected = MCRAccessKeyValidationException.class)
    public void testValidateAccessKeyDto_noPermission() {
        final MCRAccessKeyValidatorImpl validator = new MCRAccessKeyValidatorImpl();
        final MCRAccessKeyDto accessKeyDto = new MCRAccessKeyDto();
        accessKeyDto.setReference(TEST_REFERENCE);
        accessKeyDto.setValue(TEST_VALUE);
        validator.validateAccessKeyDto(accessKeyDto);
    }

    @Test(expected = MCRAccessKeyValidationException.class)
    public void testValidateAccessKeyDto_noValue() {
        final MCRAccessKeyValidatorImpl validator = new MCRAccessKeyValidatorImpl();
        final MCRAccessKeyDto accessKeyDto = new MCRAccessKeyDto();
        accessKeyDto.setPermission(TEST_PERMISSION);
        accessKeyDto.setReference(TEST_REFERENCE);
        validator.validateAccessKeyDto(accessKeyDto);
    }

    @Test
    public void testValidateAccessKeyPartialUpdateDto() {
        final MCRAccessKeyValidatorImpl validator = new MCRAccessKeyValidatorImpl();
        final MCRAccessKeyPartialUpdateDto accessKeyDto = new MCRAccessKeyPartialUpdateDto();
        accessKeyDto.setValue(new MCRNullable<>(TEST_VALUE));
        accessKeyDto.setReference(new MCRNullable<>(TEST_REFERENCE));
        accessKeyDto.setPermission(new MCRNullable<>(TEST_PERMISSION));
        validator.validateAccessKeyPartialUpdateDto(accessKeyDto);
    }

    @Test(expected = MCRAccessKeyValidationException.class)
    public void testValidateAccessKeyPartialUpdateDto_valueNull() {
        final MCRAccessKeyValidatorImpl validator = new MCRAccessKeyValidatorImpl();
        final MCRAccessKeyPartialUpdateDto accessKeyDto = new MCRAccessKeyPartialUpdateDto();
        accessKeyDto.setValue(new MCRNullable<>(null));
        accessKeyDto.setReference(new MCRNullable<>(TEST_REFERENCE));
        accessKeyDto.setPermission(new MCRNullable<>(TEST_PERMISSION));
        validator.validateAccessKeyPartialUpdateDto(accessKeyDto);
    }

    @Test(expected = MCRAccessKeyValidationException.class)
    public void testValidateAccessKeyPartialUpdateDto_permissionNull() {
        final MCRAccessKeyValidatorImpl validator = new MCRAccessKeyValidatorImpl();
        final MCRAccessKeyPartialUpdateDto accessKeyDto = new MCRAccessKeyPartialUpdateDto();
        accessKeyDto.setValue(new MCRNullable<>(TEST_VALUE));
        accessKeyDto.setReference(new MCRNullable<>(TEST_REFERENCE));
        accessKeyDto.setPermission(new MCRNullable<>(null));
        validator.validateAccessKeyPartialUpdateDto(accessKeyDto);
    }

    @Test(expected = MCRAccessKeyValidationException.class)
    public void testValidateAccessKeyPartialUpdateDto_referenceNull() {
        final MCRAccessKeyValidatorImpl validator = new MCRAccessKeyValidatorImpl();
        final MCRAccessKeyPartialUpdateDto accessKeyDto = new MCRAccessKeyPartialUpdateDto();
        accessKeyDto.setValue(new MCRNullable<>(TEST_VALUE));
        accessKeyDto.setReference(new MCRNullable<>(null));
        accessKeyDto.setPermission(new MCRNullable<>(TEST_PERMISSION));
        validator.validateAccessKeyPartialUpdateDto(accessKeyDto);
    }

    @Test
    public void testCheckPermission() {
        assertTrue(MCRAccessKeyValidatorImpl.checkPermission(TEST_PERMISSION));
        assertTrue(MCRAccessKeyValidatorImpl.checkPermission("read"));
        assertFalse(MCRAccessKeyValidatorImpl.checkPermission("noop"));
    }

    @Test
    public void testCheckValue() {
        assertFalse(MCRAccessKeyValidatorImpl.checkValue(""));
        assertTrue(MCRAccessKeyValidatorImpl.checkValue(TEST_VALUE));
    }

    @Test
    public void testCheckReference() {
        assertFalse(MCRAccessKeyValidatorImpl.checkReference(""));
        assertTrue(MCRAccessKeyValidatorImpl.checkReference(TEST_REFERENCE));
    }

}
