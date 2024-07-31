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

package org.mycore.mcr.acl.accesskey.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;

public class MCRAccessKeyRepositoryImplTest extends MCRJPATestCase {

    private static final UUID TEST_ID_READ = UUID.randomUUID();

    private static final UUID TEST_ID_WRITE = UUID.randomUUID();

    private static final String TEST_REFERENCE_READ = "testReference1";

    private static final String TEST_REFERENCE_WRITE = "testReference2";

    private static final String TEST_PERMISSION_READ = "read";

    private static final String TEST_PERMISSION_WRITE = "write";

    private static final String TEST_VALUE_READ = "readValue";

    private static final String TEST_VALUE_WRITE = "writeValue";

    @Test
    public void testFindAll() {
        insertAccessKey(TEST_ID_READ, getReadAccessKey());
        insertAccessKey(TEST_ID_WRITE, getWriteAccessKey());
        endTransaction();
        final Collection<MCRAccessKey> accessKeys = new MCRAccessKeyRepositoryImpl().findAll();
        assertEquals(2, accessKeys.size());
    }

    @Test
    public void testFindAll_empty() {
        final Collection<MCRAccessKey> accessKeys = new MCRAccessKeyRepositoryImpl().findAll();
        assertNotNull(accessKeys);
        assertEquals(0, accessKeys.size());
    }

    @Test
    public void testFindByReference() {
        insertAccessKey(TEST_ID_READ, getReadAccessKey());
        insertAccessKey(TEST_ID_WRITE, getWriteAccessKey());
        endTransaction();
        final Collection<MCRAccessKey> accessKeys = new MCRAccessKeyRepositoryImpl()
            .findByReference(TEST_REFERENCE_READ);
        assertEquals(1, accessKeys.size());
        final MCRAccessKey accessKey = accessKeys.iterator().next();
        assertEquals(TEST_ID_READ, accessKey.getUuid());
        assertEquals(TEST_REFERENCE_READ, accessKey.getReference());
        assertEquals(TEST_PERMISSION_READ, accessKey.getPermission());
        assertEquals(TEST_VALUE_READ, accessKey.getValue());
    }

    @Test
    public void testFindByReference_empty() {
        final Collection<MCRAccessKey> accessKeys = new MCRAccessKeyRepositoryImpl()
            .findByReference(TEST_REFERENCE_READ);
        assertNotNull(accessKeys);
        assertEquals(0, accessKeys.size());
    }

    @Test
    public void testFindByReferenceAndPermission() {
        insertAccessKey(TEST_ID_READ, getReadAccessKey());
        insertAccessKey(TEST_ID_WRITE, getWriteAccessKey());
        endTransaction();
        final Collection<MCRAccessKey> accessKeys = new MCRAccessKeyRepositoryImpl()
            .findByReferenceAndPermission(TEST_REFERENCE_READ, TEST_PERMISSION_READ);
        assertEquals(1, accessKeys.size());
        final MCRAccessKey accessKey = accessKeys.iterator().next();
        assertEquals(TEST_ID_READ, accessKey.getUuid());
        assertEquals(TEST_REFERENCE_READ, accessKey.getReference());
        assertEquals(TEST_PERMISSION_READ, accessKey.getPermission());
        assertEquals(TEST_VALUE_READ, accessKey.getValue());
    }

    @Test
    public void testFindByReferenceAndPermission_empty() {
        final Collection<MCRAccessKey> accessKeys = new MCRAccessKeyRepositoryImpl()
            .findByReferenceAndPermission(TEST_REFERENCE_READ, TEST_PERMISSION_READ);
        assertNotNull(accessKeys);
        assertEquals(0, accessKeys.size());
    }

    @Test
    public void testFindByUuid() {
        insertAccessKey(TEST_ID_READ, getReadAccessKey());
        insertAccessKey(TEST_ID_WRITE, getWriteAccessKey());
        endTransaction();
        final Optional<MCRAccessKey> optAccessKey = new MCRAccessKeyRepositoryImpl().findByUuid(TEST_ID_READ);
        assertNotNull(optAccessKey);
        assertTrue(optAccessKey.isPresent());
        assertEquals(TEST_ID_READ, optAccessKey.get().getUuid());
        assertEquals(TEST_REFERENCE_READ, optAccessKey.get().getReference());
        assertEquals(TEST_PERMISSION_READ, optAccessKey.get().getPermission());
        assertEquals(TEST_VALUE_READ, optAccessKey.get().getValue());
    }

    @Test
    public void testFindByUuid_noMatch() {
        insertAccessKey(TEST_ID_READ, getReadAccessKey());
        endTransaction();
        final Optional<MCRAccessKey> optAccessKey = new MCRAccessKeyRepositoryImpl().findByUuid(TEST_ID_WRITE);
        assertNotNull(optAccessKey);
        assertTrue(optAccessKey.isEmpty());
    }

    @Test
    public void testFindByReferenceAndValue() {
        insertAccessKey(TEST_ID_READ, getReadAccessKey());
        insertAccessKey(TEST_ID_WRITE, getWriteAccessKey());
        endTransaction();
        final Optional<MCRAccessKey> optAccessKey = new MCRAccessKeyRepositoryImpl()
            .findByReferenceAndValue(TEST_REFERENCE_READ, TEST_VALUE_READ);
        assertNotNull(optAccessKey);
        assertTrue(optAccessKey.isPresent());
        assertEquals(TEST_ID_READ, optAccessKey.get().getUuid());
        assertEquals(TEST_REFERENCE_READ, optAccessKey.get().getReference());
        assertEquals(TEST_PERMISSION_READ, optAccessKey.get().getPermission());
        assertEquals(TEST_VALUE_READ, optAccessKey.get().getValue());
    }

    @Test
    public void testFindByReferenceAndValue_noMatch() {
        final Optional<MCRAccessKey> optAccessKey = new MCRAccessKeyRepositoryImpl()
            .findByReferenceAndValue(TEST_REFERENCE_READ, TEST_VALUE_READ);
        assertNotNull(optAccessKey);
        assertTrue(optAccessKey.isEmpty());
    }

    @Test
    public void testSave_create() {
        final MCRAccessKey accessKey = new MCRAccessKey();
        accessKey.setReference(TEST_REFERENCE_READ);
        accessKey.setPermission(TEST_PERMISSION_READ);
        accessKey.setValue(TEST_VALUE_READ);
        accessKey.setIsActive(true);
        final MCRAccessKey createdAccessKey = new MCRAccessKeyRepositoryImpl().save(accessKey);
        endTransaction();
        assertNotNull(createdAccessKey);
        assertNotNull(createdAccessKey.getUuid());
        assertEquals(TEST_REFERENCE_READ, createdAccessKey.getReference());
        assertEquals(TEST_PERMISSION_READ, createdAccessKey.getPermission());
        assertEquals(TEST_VALUE_READ, createdAccessKey.getValue());
    }

    @Test
    public void testSave_update() {
        insertAccessKey(TEST_ID_READ, getReadAccessKey());
        startNewTransaction();
        final MCRAccessKey accessKey = new MCRAccessKey();
        accessKey.setUuid(TEST_ID_READ);
        accessKey.setReference(TEST_REFERENCE_READ);
        accessKey.setPermission(TEST_PERMISSION_WRITE);
        accessKey.setValue(TEST_VALUE_READ);
        accessKey.setIsActive(true);
        final MCRAccessKey createdAccessKey = new MCRAccessKeyRepositoryImpl().save(accessKey);
        endTransaction();
        assertNotNull(createdAccessKey);
        assertEquals(TEST_ID_READ, createdAccessKey.getUuid());
        assertEquals(TEST_REFERENCE_READ, createdAccessKey.getReference());
        assertEquals(TEST_PERMISSION_WRITE, createdAccessKey.getPermission());
        assertEquals(TEST_VALUE_READ, createdAccessKey.getValue());
    }

    @Test
    public void testExistsByReferenceAndValue() {
        insertAccessKey(TEST_ID_READ, getReadAccessKey());
        final boolean check = new MCRAccessKeyRepositoryImpl().existsByReferenceAndValue(TEST_REFERENCE_READ,
            TEST_VALUE_READ);
        assertTrue(check);
    }

    @Test
    public void testExistsByReferenceAndValue_noMatch() {
        insertAccessKey(TEST_ID_READ, getReadAccessKey());
        final boolean check = new MCRAccessKeyRepositoryImpl().existsByReferenceAndValue(TEST_REFERENCE_READ + "1",
            TEST_VALUE_READ);
        assertFalse(check);
    }

    private MCRAccessKey getReadAccessKey() {
        return new MCRAccessKey(TEST_REFERENCE_READ, TEST_PERMISSION_READ, TEST_VALUE_READ);
    }

    private MCRAccessKey getWriteAccessKey() {
        return new MCRAccessKey(TEST_REFERENCE_WRITE, TEST_PERMISSION_WRITE, TEST_VALUE_WRITE);
    }

    private void insertAccessKey(UUID id, MCRAccessKey accessKey) {
        MCRJPATestCase.executeUpdate(
            "INSERT INTO \"junit\".\"MCRAccessKey\" (\"uuid\",\"type\",\"objectId\","
                + "\"secret\",\"isActive\") VALUES ('" + id + "','" + accessKey.getPermission() + "','"
                + accessKey.getReference() + "','" + accessKey.getValue() + "',true)");
    }
}
