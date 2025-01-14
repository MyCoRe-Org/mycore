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

    private static final String TEST_TYPE_READ = "read";

    private static final String TEST_TYPE_WRITE = "write";

    private static final String TEST_SECRET_READ = "readSecret";

    private static final String TEST_SECRET_WRITE = "writeSecret";

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
        final Collection<MCRAccessKey> accessKeys
            = new MCRAccessKeyRepositoryImpl().findByReference(TEST_REFERENCE_READ);
        assertEquals(1, accessKeys.size());
        final MCRAccessKey accessKey = accessKeys.iterator().next();
        assertEquals(TEST_ID_READ, accessKey.getUuid());
        assertEquals(TEST_REFERENCE_READ, accessKey.getReference());
        assertEquals(TEST_TYPE_READ, accessKey.getType());
        assertEquals(TEST_SECRET_READ, accessKey.getSecret());
    }

    @Test
    public void testFindByReference_empty() {
        final Collection<MCRAccessKey> accessKeys
            = new MCRAccessKeyRepositoryImpl().findByReference(TEST_REFERENCE_READ);
        assertNotNull(accessKeys);
        assertEquals(0, accessKeys.size());
    }

    @Test
    public void testFindByReferenceAndType() {
        insertAccessKey(TEST_ID_READ, getReadAccessKey());
        insertAccessKey(TEST_ID_WRITE, getWriteAccessKey());
        endTransaction();
        final Collection<MCRAccessKey> accessKeys
            = new MCRAccessKeyRepositoryImpl().findByReferenceAndType(TEST_REFERENCE_READ, TEST_TYPE_READ);
        assertEquals(1, accessKeys.size());
        final MCRAccessKey accessKey = accessKeys.iterator().next();
        assertEquals(TEST_ID_READ, accessKey.getUuid());
        assertEquals(TEST_REFERENCE_READ, accessKey.getReference());
        assertEquals(TEST_TYPE_READ, accessKey.getType());
        assertEquals(TEST_SECRET_READ, accessKey.getSecret());
    }

    @Test
    public void testFindByReferenceAndType_empty() {
        final Collection<MCRAccessKey> accessKeys
            = new MCRAccessKeyRepositoryImpl().findByReferenceAndType(TEST_REFERENCE_READ, TEST_TYPE_READ);
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
        assertEquals(TEST_TYPE_READ, optAccessKey.get().getType());
        assertEquals(TEST_SECRET_READ, optAccessKey.get().getSecret());
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
    public void testFindByReferenceAndSecret() {
        insertAccessKey(TEST_ID_READ, getReadAccessKey());
        insertAccessKey(TEST_ID_WRITE, getWriteAccessKey());
        endTransaction();
        final Optional<MCRAccessKey> optAccessKey
            = new MCRAccessKeyRepositoryImpl().findByReferenceAndSecret(TEST_REFERENCE_READ, TEST_SECRET_READ);
        assertNotNull(optAccessKey);
        assertTrue(optAccessKey.isPresent());
        assertEquals(TEST_ID_READ, optAccessKey.get().getUuid());
        assertEquals(TEST_REFERENCE_READ, optAccessKey.get().getReference());
        assertEquals(TEST_TYPE_READ, optAccessKey.get().getType());
        assertEquals(TEST_SECRET_READ, optAccessKey.get().getSecret());
    }

    @Test
    public void testFindByReferenceAndSecret_noMatch() {
        final Optional<MCRAccessKey> optAccessKey
            = new MCRAccessKeyRepositoryImpl().findByReferenceAndSecret(TEST_REFERENCE_READ, TEST_SECRET_READ);
        assertNotNull(optAccessKey);
        assertTrue(optAccessKey.isEmpty());
    }

    @Test
    public void testSave_create() {
        final MCRAccessKey accessKey = new MCRAccessKey();
        accessKey.setReference(TEST_REFERENCE_READ);
        accessKey.setType(TEST_TYPE_READ);
        accessKey.setSecret(TEST_SECRET_READ);
        accessKey.setIsActive(true);
        final MCRAccessKey createdAccessKey = new MCRAccessKeyRepositoryImpl().save(accessKey);
        endTransaction();
        assertNotNull(createdAccessKey);
        assertNotNull(createdAccessKey.getUuid());
        assertEquals(TEST_REFERENCE_READ, createdAccessKey.getReference());
        assertEquals(TEST_TYPE_READ, createdAccessKey.getType());
        assertEquals(TEST_SECRET_READ, createdAccessKey.getSecret());
    }

    @Test
    public void testSave_update() {
        insertAccessKey(TEST_ID_READ, getReadAccessKey());
        startNewTransaction();
        final MCRAccessKey accessKey = new MCRAccessKey();
        accessKey.setUuid(TEST_ID_READ);
        accessKey.setReference(TEST_REFERENCE_READ);
        accessKey.setType(TEST_TYPE_WRITE);
        accessKey.setSecret(TEST_SECRET_READ);
        accessKey.setIsActive(true);
        final MCRAccessKey createdAccessKey = new MCRAccessKeyRepositoryImpl().save(accessKey);
        endTransaction();
        assertNotNull(createdAccessKey);
        assertEquals(TEST_ID_READ, createdAccessKey.getUuid());
        assertEquals(TEST_REFERENCE_READ, createdAccessKey.getReference());
        assertEquals(TEST_TYPE_WRITE, createdAccessKey.getType());
        assertEquals(TEST_SECRET_READ, createdAccessKey.getSecret());
    }

    @Test
    public void testExistsByReferenceAndSecret() {
        insertAccessKey(TEST_ID_READ, getReadAccessKey());
        final boolean check
            = new MCRAccessKeyRepositoryImpl().existsByReferenceAndSecret(TEST_REFERENCE_READ, TEST_SECRET_READ);
        assertTrue(check);
    }

    @Test
    public void testExistsByReferenceAndSecret_noMatch() {
        insertAccessKey(TEST_ID_READ, getReadAccessKey());
        final boolean check
            = new MCRAccessKeyRepositoryImpl().existsByReferenceAndSecret(TEST_REFERENCE_READ + "1", TEST_SECRET_READ);
        assertFalse(check);
    }

    private MCRAccessKey getReadAccessKey() {
        return new MCRAccessKey(TEST_REFERENCE_READ, TEST_TYPE_READ, TEST_SECRET_READ);
    }

    private MCRAccessKey getWriteAccessKey() {
        return new MCRAccessKey(TEST_REFERENCE_WRITE, TEST_TYPE_WRITE, TEST_SECRET_WRITE);
    }

    private void insertAccessKey(UUID id, MCRAccessKey accessKey) {
        MCRJPATestCase.executeUpdate(
            "INSERT INTO \"junit\".\"MCRAccessKey\" (\"uuid\",\"type\",\"object_id\","
                + "\"secret\",\"isActive\") VALUES ('" + id + "','" + accessKey.getType() + "','"
                + accessKey.getReference() + "','" + accessKey.getSecret() + "',true)");
    }
}
