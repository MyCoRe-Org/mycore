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

package org.mycore.mcr.acl.accesskey.mapper;

import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MCRAccessKeyMapperTest {

    private static final UUID TEST_ID = UUID.randomUUID();

    private static final String TEST_REFERENCE = "testReference";

    private static final String TEST_VALUE = "testValue";

    private static final String TEST_PERMISSION = "testPermission";

    private static final Date TEST_EXPIRATION = new Date();

    private static final Boolean TEST_ACTIVE = true;

    private static final String TEST_COMMENT = "testComment";

    private static final Date TEST_CREATED = new Date();

    private static final String TEST_CREATED_BY = "testCreatedBy";

    private static final Date TEST_LAST_MODIFIED = new Date();

    private static final String TEST_LAST_MODIFIED_BY = "testLastModifiedBy";

    @Test
    public void testToEntity() {
        final MCRAccessKeyDto accessKeyDto = new MCRAccessKeyDto();
        accessKeyDto.setId(TEST_ID);
        accessKeyDto.setReference(TEST_REFERENCE);
        accessKeyDto.setSecret(TEST_VALUE);
        accessKeyDto.setPermission(TEST_PERMISSION);
        accessKeyDto.setExpiration(TEST_EXPIRATION);
        accessKeyDto.setComment(TEST_COMMENT);
        accessKeyDto.setActive(TEST_ACTIVE);
        accessKeyDto.setCreated(TEST_EXPIRATION);
        accessKeyDto.setCreated(TEST_CREATED);
        accessKeyDto.setCreatedBy(TEST_CREATED_BY);
        accessKeyDto.setLastModified(TEST_LAST_MODIFIED);
        accessKeyDto.setLastModifiedBy(TEST_LAST_MODIFIED_BY);
        final MCRAccessKey accessKey = MCRAccessKeyMapper.toEntity(accessKeyDto);
        assertEquals(TEST_ID, accessKey.getUuid());
        assertEquals(TEST_REFERENCE, accessKey.getReference());
        assertEquals(TEST_VALUE, accessKey.getSecret());
        assertEquals(TEST_PERMISSION, accessKey.getType());
        assertEquals(TEST_EXPIRATION, accessKey.getExpiration());
        assertEquals(TEST_ACTIVE, accessKey.getIsActive());
        assertEquals(TEST_COMMENT, accessKey.getComment());
        assertEquals(TEST_CREATED, accessKey.getCreated());
        assertEquals(TEST_CREATED_BY, accessKey.getCreatedBy());
        assertEquals(TEST_LAST_MODIFIED, accessKey.getLastModified());
        assertEquals(TEST_LAST_MODIFIED_BY, accessKey.getLastModifiedBy());
    }

    @Test
    public void testToEntity_null() {
        final MCRAccessKeyDto accessKeyDto = new MCRAccessKeyDto();
        final MCRAccessKey accessKey = MCRAccessKeyMapper.toEntity(accessKeyDto);
        assertNull(accessKey.getId());
        assertNull(accessKey.getReference());
        assertNull(accessKey.getSecret());
        assertNull(accessKey.getType());
        assertNull(accessKey.getExpiration());
        assertNull(accessKey.getIsActive());
        assertNull(accessKey.getComment());
        assertNull(accessKey.getCreated());
        assertNull(accessKey.getCreatedBy());
        assertNull(accessKey.getLastModified());
        assertNull(accessKey.getLastModifiedBy());
    }

    @Test
    public void testToDto() {
        final MCRAccessKey accessKey = new MCRAccessKey();
        accessKey.setUuid(TEST_ID);
        accessKey.setReference(TEST_REFERENCE);
        accessKey.setSecret(TEST_VALUE);
        accessKey.setType(TEST_PERMISSION);
        accessKey.setExpiration(TEST_EXPIRATION);
        accessKey.setComment(TEST_COMMENT);
        accessKey.setIsActive(TEST_ACTIVE);
        accessKey.setCreated(TEST_EXPIRATION);
        accessKey.setCreated(TEST_CREATED);
        accessKey.setCreatedBy(TEST_CREATED_BY);
        accessKey.setLastModified(TEST_LAST_MODIFIED);
        accessKey.setLastModifiedBy(TEST_LAST_MODIFIED_BY);
        final MCRAccessKeyDto accessKeyDto = MCRAccessKeyMapper.toDto(accessKey);
        assertEquals(TEST_ID, accessKeyDto.getId());
        assertEquals(TEST_REFERENCE, accessKeyDto.getReference());
        assertEquals(TEST_VALUE, accessKeyDto.getSecret());
        assertEquals(TEST_PERMISSION, accessKeyDto.getPermission());
        assertEquals(TEST_EXPIRATION, accessKeyDto.getExpiration());
        assertEquals(TEST_ACTIVE, accessKeyDto.getActive());
        assertEquals(TEST_COMMENT, accessKeyDto.getComment());
        assertEquals(TEST_CREATED, accessKeyDto.getCreated());
        assertEquals(TEST_CREATED_BY, accessKeyDto.getCreatedBy());
        assertEquals(TEST_LAST_MODIFIED, accessKeyDto.getLastModified());
        assertEquals(TEST_LAST_MODIFIED_BY, accessKeyDto.getLastModifiedBy());
    }

    @Test
    public void testToDto_null() {
        final MCRAccessKey accessKey = new MCRAccessKey();
        final MCRAccessKeyDto accessKeyDto = MCRAccessKeyMapper.toDto(accessKey);
        assertNull(accessKeyDto.getId());
        assertNull(accessKeyDto.getReference());
        assertNull(accessKeyDto.getSecret());
        assertNull(accessKeyDto.getPermission());
        assertNull(accessKeyDto.getExpiration());
        assertNull(accessKeyDto.getActive());
        assertNull(accessKeyDto.getComment());
        assertNull(accessKeyDto.getCreated());
        assertNull(accessKeyDto.getCreatedBy());
        assertNull(accessKeyDto.getLastModified());
        assertNull(accessKeyDto.getLastModifiedBy());
    }
}
