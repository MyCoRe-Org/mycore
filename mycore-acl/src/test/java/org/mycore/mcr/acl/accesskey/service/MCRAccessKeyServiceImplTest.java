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

package org.mycore.mcr.acl.accesskey.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mycore.common.MCRTestCase;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;
import org.mycore.mcr.acl.accesskey.dto.util.MCRNullable;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyCollisionException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.mcr.acl.accesskey.mapper.MCRAccessKeyMapper;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;
import org.mycore.mcr.acl.accesskey.persistence.MCRAccessKeyRepository;

public class MCRAccessKeyServiceImplTest extends MCRTestCase {

    private MCRAccessKeyRepository accessKeyRepositoryMock;

    private MCRAccessKeyServiceImpl accessKeyService;

    private static final UUID TEST_ID_READ = UUID.randomUUID();

    private static final UUID TEST_ID_WRITE = UUID.randomUUID();

    private static final String TEST_REFERENCE_READ = "testReference1";

    private static final String TEST_REFERENCE_WRITE = "testReference2";

    private static final String TEST_PERMISSION_READ = "read";

    private static final String TEST_PERMISSION_WRITE = "write";

    private static final String TEST_VALUE_READ = "readValue";

    private static final String TEST_VALUE_WRITE = "writeValue";

    private static String testEncodedValueRead;

    private static String testEncodedValueWrite;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        accessKeyRepositoryMock = Mockito.mock(MCRAccessKeyRepository.class);
        Mockito.when(accessKeyRepositoryMock.existsByReferenceAndValue(TEST_REFERENCE_READ, testEncodedValueWrite))
            .thenReturn(false);
        accessKeyService = new MCRAccessKeyServiceImpl(accessKeyRepositoryMock);
        testEncodedValueRead = MCRAccessKeyServiceImpl.getEncodedValue(TEST_REFERENCE_READ, TEST_VALUE_READ);
        testEncodedValueWrite = MCRAccessKeyServiceImpl.getEncodedValue(TEST_REFERENCE_WRITE, TEST_VALUE_WRITE);
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.ACL.AccessKey.Secret.Storage.Mode", "plain");
        return testProperties;
    }

    @Test
    public void testGetAllAccessKeys() {
        final List<MCRAccessKey> accessKeys = new ArrayList<>();
        accessKeys.add(getReadAccessKey());
        accessKeys.add(getWriteAccessKey());
        Mockito.when(accessKeyRepositoryMock.findAll()).thenReturn(accessKeys);
        final List<MCRAccessKeyDto> resultAccessKeys = accessKeyService.getAllAccessKeys();
        assertNotNull(resultAccessKeys);
        assertEquals(2, resultAccessKeys.size());
        final MCRAccessKeyDto accessKeyDto = resultAccessKeys.getFirst();
        compareReadAccessKey(accessKeyDto);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findAll();
    }

    @Test
    public void testGetAllAccessKeys_fix() {
        final List<MCRAccessKey> accessKeys = new ArrayList<>();
        final MCRAccessKey accessKeyNoUuuid = new MCRAccessKey();
        accessKeys.add(accessKeyNoUuuid);
        Mockito.when(accessKeyRepositoryMock.findAll()).thenReturn(accessKeys);
        Mockito.when(accessKeyRepositoryMock.save(accessKeyNoUuuid)).thenAnswer(new Answer<MCRAccessKey>() {
            @Override
            public MCRAccessKey answer(InvocationOnMock invocation) throws Throwable {
                final MCRAccessKey accessKey = (MCRAccessKey) invocation.getArguments()[0];
                accessKey.setUuid(UUID.randomUUID());
                return accessKey;
            }
        });
        final List<MCRAccessKeyDto> accessKeyDtos = accessKeyService.getAllAccessKeys();
        assertNotNull(accessKeyDtos);
        assertEquals(1, accessKeyDtos.size());
        final MCRAccessKeyDto accessKeyDto = accessKeyDtos.getFirst();
        assertNotNull(accessKeyDto.getId());
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findAll();
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).save(Mockito.any());
    }

    @Test
    public void testGetAllAccessKeys_empty() {
        Mockito.when(accessKeyRepositoryMock.findAll()).thenReturn(new ArrayList<>());
        final List<MCRAccessKeyDto> resultAccessKeys = accessKeyService.getAllAccessKeys();
        assertNotNull(resultAccessKeys);
        assertEquals(0, resultAccessKeys.size());
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findAll();
    }

    @Test
    public void testGetAccessKeysByReference() {
        final List<MCRAccessKey> accessKeys = new ArrayList<>();
        accessKeys.add(getReadAccessKey());
        Mockito.when(accessKeyRepositoryMock.findByReference(TEST_REFERENCE_READ)).thenReturn(accessKeys);
        final List<MCRAccessKeyDto> resultAccessKeys = accessKeyService.getAccessKeysByReference(TEST_REFERENCE_READ);
        assertNotNull(resultAccessKeys);
        assertEquals(1, resultAccessKeys.size());
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByReference(TEST_REFERENCE_READ);
    }

    @Test
    public void testGetAccessKeysByReference_empty() {
        Mockito.when(accessKeyRepositoryMock.findByReference(TEST_REFERENCE_READ)).thenReturn(new ArrayList<>());
        final List<MCRAccessKeyDto> resultAccessKeys = accessKeyService.getAccessKeysByReference(TEST_REFERENCE_READ);
        assertNotNull(resultAccessKeys);
        assertEquals(0, resultAccessKeys.size());
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByReference(TEST_REFERENCE_READ);
    }

    @Test
    public void testGetAccessKeysByReferenceAndPermission() {
        final List<MCRAccessKey> accessKeys = new ArrayList<>();
        accessKeys.add(getReadAccessKey());
        Mockito.when(accessKeyRepositoryMock.findByReferenceAndPermission(TEST_REFERENCE_READ, TEST_PERMISSION_READ))
            .thenReturn(accessKeys);
        final List<MCRAccessKeyDto> resultAccessKeys
            = accessKeyService.getAccessKeysByReferenceAndPermission(TEST_REFERENCE_READ, TEST_PERMISSION_READ);
        assertNotNull(resultAccessKeys);
        assertEquals(1, resultAccessKeys.size());
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByReferenceAndPermission(TEST_REFERENCE_READ,
            TEST_PERMISSION_READ);
    }

    @Test
    public void testGetAccessKeysByReferenceAndPermission_empty() {
        Mockito.when(accessKeyRepositoryMock.findByReferenceAndPermission(TEST_REFERENCE_READ, TEST_PERMISSION_READ))
            .thenReturn(new ArrayList<>());
        final List<MCRAccessKeyDto> resultAccessKeys
            = accessKeyService.getAccessKeysByReferenceAndPermission(TEST_REFERENCE_READ, TEST_PERMISSION_READ);
        assertNotNull(resultAccessKeys);
        assertEquals(0, resultAccessKeys.size());
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByReferenceAndPermission(TEST_REFERENCE_READ,
            TEST_PERMISSION_READ);
    }

    @Test
    public void testGetAccessKeyById() {
        Mockito.when(accessKeyRepositoryMock.findByUuid(TEST_ID_READ)).thenReturn(Optional.of(getReadAccessKey()));
        final MCRAccessKeyDto accessKeyDto = accessKeyService.getAccessKeyById(TEST_ID_READ);
        assertNotNull(accessKeyDto);
        compareReadAccessKey(accessKeyDto);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByUuid(TEST_ID_READ);
    }

    public void testGetAccessKeyById_notExists() {
        Mockito.when(accessKeyRepositoryMock.findByUuid(Mockito.any())).thenReturn(Optional.empty());
        assertThrows(MCRAccessKeyNotFoundException.class, () -> accessKeyService.getAccessKeyById(TEST_ID_READ));
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByUuid(Mockito.any());
    }

    @Test
    public void testGetAccessKeyByReferenceAndValue() {
        Mockito.when(accessKeyRepositoryMock.findByReferenceAndValue(TEST_REFERENCE_READ, testEncodedValueRead))
            .thenReturn(Optional.of(getReadAccessKey()));
        final MCRAccessKeyDto accessKeyDto
            = accessKeyService.getAccessKeyByReferenceAndValue(TEST_REFERENCE_READ, TEST_VALUE_READ);
        assertNotNull(accessKeyDto);
        compareReadAccessKey(accessKeyDto);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByReferenceAndValue(TEST_REFERENCE_READ,
            testEncodedValueRead);
    }

    public void testGetAccessKeyByReferenceAndValue_notExists() {
        Mockito.when(accessKeyRepositoryMock.findByReferenceAndValue(TEST_REFERENCE_READ, testEncodedValueRead))
            .thenReturn(Optional.empty());
        assertThrows(MCRAccessKeyNotFoundException.class,
            () -> accessKeyService.getAccessKeyByReferenceAndValue(TEST_REFERENCE_READ, TEST_VALUE_READ));
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByReferenceAndValue(TEST_REFERENCE_READ,
            testEncodedValueRead);
    }

    @Test
    public void testCreateAccessKey() {
        final MCRAccessKey accessKey = new MCRAccessKey(TEST_REFERENCE_READ, TEST_PERMISSION_READ, TEST_VALUE_READ);
        final MCRAccessKeyDto createAccessKeyDto = MCRAccessKeyMapper.toDto(accessKey);
        accessKey.setSecret(testEncodedValueRead);
        accessKey.setUuid(TEST_ID_READ);
        Mockito.when(accessKeyRepositoryMock.save(accessKey)).thenReturn(accessKey);
        Mockito.when(accessKeyRepositoryMock.existsByReferenceAndValue(TEST_REFERENCE_READ, testEncodedValueRead))
            .thenReturn(false);
        final MCRAccessKeyServiceImpl service = new MCRAccessKeyServiceImpl(accessKeyRepositoryMock);
        final MCRAccessKeyDto createdAccessKey = service.createAccessKey(createAccessKeyDto);
        assertNotNull(createdAccessKey);
        assertEquals(TEST_ID_READ, createdAccessKey.getId());
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).save(accessKey);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).existsByReferenceAndValue(TEST_REFERENCE_READ,
            testEncodedValueRead);
    }

    public void testCreateAccessKey_collision() {
        Mockito.when(accessKeyRepositoryMock.existsByReferenceAndValue(TEST_REFERENCE_READ, testEncodedValueRead))
            .thenReturn(true);
        assertThrows(MCRAccessKeyCollisionException.class,
            () -> accessKeyService.createAccessKey(MCRAccessKeyMapper.toDto(getReadAccessKey())));
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).existsByReferenceAndValue(TEST_REFERENCE_READ,
            testEncodedValueRead);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(0)).save(Mockito.any());
    }

    @Test
    public void testImportAccessKey() {
        final MCRAccessKeyDto importAccessKeyDto = MCRAccessKeyMapper.toDto(getReadAccessKey());
        importAccessKeyDto.setCreated(new Date());
        importAccessKeyDto.setCreatedBy("bla");
        final MCRAccessKey importAccessKey = MCRAccessKeyMapper.toEntity(importAccessKeyDto);
        Mockito
            .when(accessKeyRepositoryMock.existsByReferenceAndValue(TEST_REFERENCE_READ, TEST_VALUE_READ))
            .thenReturn(false);
        Mockito.when(accessKeyRepositoryMock.save(importAccessKey)).thenReturn(importAccessKey);
        final MCRAccessKeyDto importedAccessKeyDto = accessKeyService.importAccessKey(importAccessKeyDto);
        assertEquals(importAccessKeyDto, importedAccessKeyDto);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).existsByReferenceAndValue(TEST_REFERENCE_READ,
            TEST_VALUE_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).save(importAccessKey);
    }

    public void testImportAccessKey_collision() {
        Mockito.when(accessKeyRepositoryMock.existsByReferenceAndValue(TEST_REFERENCE_READ, testEncodedValueRead))
            .thenReturn(true);
        assertThrows(MCRAccessKeyCollisionException.class,
            () -> accessKeyService.importAccessKey(MCRAccessKeyMapper.toDto(getReadAccessKey())));
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).existsByReferenceAndValue(TEST_REFERENCE_READ,
            testEncodedValueRead);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(0)).save(Mockito.any());
    }

    @Test
    public void testUpdateAccessKeyById() {
        final String newValue = "newValue";
        final String encodedNewValue = MCRAccessKeyServiceImpl.getEncodedValue(TEST_REFERENCE_READ, newValue);
        final MCRAccessKey outdatedAccessKey = getReadAccessKey();
        final MCRAccessKeyDto updateAccessKeyDto = MCRAccessKeyMapper.toDto(outdatedAccessKey);
        updateAccessKeyDto.setValue(newValue);
        final MCRAccessKey updateAccessKey = MCRAccessKeyMapper.toEntity(updateAccessKeyDto);
        updateAccessKey.setSecret(encodedNewValue);
        Mockito.when(accessKeyRepositoryMock.findByUuid(TEST_ID_READ)).thenReturn(Optional.of(getReadAccessKey()));
        Mockito.when(accessKeyRepositoryMock.existsByReferenceAndValue(TEST_REFERENCE_READ, encodedNewValue))
            .thenReturn(false);
        Mockito.when(accessKeyRepositoryMock.save(updateAccessKey)).thenReturn(updateAccessKey);
        final MCRAccessKeyDto updatedAccessKeyDto
            = accessKeyService.updateAccessKeyById(TEST_ID_READ, updateAccessKeyDto);
        assertEquals(MCRAccessKeyMapper.toDto(updateAccessKey), updatedAccessKeyDto);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByUuid(TEST_ID_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).existsByReferenceAndValue(TEST_REFERENCE_READ,
            encodedNewValue);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).save(updateAccessKey);
    }

    @Test
    public void testUpdateAccessKeyById_collision() {
        final String newValue = "newValue";
        final MCRAccessKey outdatedAccessKey = getReadAccessKey();
        final MCRAccessKeyDto updatedAccessKeyDto = MCRAccessKeyMapper.toDto(outdatedAccessKey);
        updatedAccessKeyDto.setValue(newValue);
        final String encodedNewValue = MCRAccessKeyServiceImpl.getEncodedValue(TEST_REFERENCE_READ, newValue);
        Mockito.when(accessKeyRepositoryMock.findByUuid(TEST_ID_READ)).thenReturn(Optional.of(outdatedAccessKey));
        Mockito.when(accessKeyRepositoryMock.existsByReferenceAndValue(TEST_REFERENCE_READ, encodedNewValue))
            .thenReturn(true);
        assertThrows(MCRAccessKeyCollisionException.class,
            () -> accessKeyService.updateAccessKeyById(TEST_ID_READ, updatedAccessKeyDto));
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByUuid(TEST_ID_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).existsByReferenceAndValue(TEST_REFERENCE_READ,
            encodedNewValue);
    }

    @Test
    public void testUpdateAccessKeyById_notExists() {
        final MCRAccessKey updatedAccessKey = getReadAccessKey();
        Mockito.when(accessKeyRepositoryMock.findByUuid(TEST_ID_READ)).thenReturn(Optional.empty());
        assertThrows(MCRAccessKeyNotFoundException.class, () -> accessKeyService.updateAccessKeyById(TEST_ID_READ,
            MCRAccessKeyMapper.toDto(updatedAccessKey)));
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByUuid(TEST_ID_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(0)).existsByReferenceAndValue(Mockito.any(),
            Mockito.any());
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(0)).save(Mockito.any());
    }

    @Test
    public void testPartialUpdateAccessKeyById() {
        final String newValue = "newValue";
        final String encodedNewValue = MCRAccessKeyServiceImpl.getEncodedValue(TEST_REFERENCE_READ, newValue);
        final MCRAccessKeyPartialUpdateDto partialUpdateDto = new MCRAccessKeyPartialUpdateDto();
        partialUpdateDto.setValue(new MCRNullable<>(newValue));
        partialUpdateDto.setExpiration(new MCRNullable<>(null));
        final MCRAccessKey outdatedAccessKey = getReadAccessKey();
        outdatedAccessKey.setExpiration(new Date());
        final MCRAccessKey updatedAccessKey = getReadAccessKey();
        updatedAccessKey.setSecret(encodedNewValue);
        updatedAccessKey.setExpiration(null);
        Mockito.when(accessKeyRepositoryMock.findByUuid(TEST_ID_READ)).thenReturn(Optional.of(outdatedAccessKey));
        Mockito.when(accessKeyRepositoryMock.existsByReferenceAndValue(TEST_REFERENCE_READ, encodedNewValue))
            .thenReturn(false);
        Mockito.when(accessKeyRepositoryMock.save(updatedAccessKey)).thenReturn(updatedAccessKey);
        final MCRAccessKeyDto updatedAccessKeyDto
            = accessKeyService.partialUpdateAccessKeyById(TEST_ID_READ, partialUpdateDto);
        assertNull(updatedAccessKeyDto.getExpiration());
        assertEquals(encodedNewValue, updatedAccessKeyDto.getValue());
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByUuid(TEST_ID_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).save(updatedAccessKey);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).existsByReferenceAndValue(TEST_REFERENCE_READ,
            encodedNewValue);
    }

    @Test
    public void testPartialUpdateAccessKeyById_notExists() {
        Mockito.when(accessKeyRepositoryMock.findByUuid(TEST_ID_READ)).thenReturn(Optional.empty());
        assertThrows(MCRAccessKeyNotFoundException.class,
            () -> accessKeyService.partialUpdateAccessKeyById(TEST_ID_READ, null));
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByUuid(TEST_ID_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(0)).save(Mockito.any());
    }

    @Test
    public void testDeleteAccessKeyById() {
        Mockito.when(accessKeyRepositoryMock.findByUuid(TEST_ID_READ)).thenReturn(Optional.of(getReadAccessKey()));
        accessKeyService.deleteAccessKeyById(TEST_ID_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByUuid(TEST_ID_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).delete(getReadAccessKey());
    }

    public void testDeleteAccessKeyById_notExists() {
        Mockito.when(accessKeyRepositoryMock.findByUuid(TEST_ID_READ)).thenReturn(Optional.empty());
        assertThrows(MCRAccessKeyNotFoundException.class, () -> accessKeyService.deleteAccessKeyById(TEST_ID_READ));
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByUuid(TEST_ID_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(0)).delete(Mockito.any());
    }

    @Test
    public void testDeleteAccessKeysByReference() {
        Mockito.when(accessKeyRepositoryMock.deleteByReference(TEST_REFERENCE_READ)).thenReturn(1l);
        final boolean deleted = accessKeyService.deleteAccessKeysByReference(TEST_REFERENCE_READ);
        assertTrue(deleted);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).deleteByReference(TEST_REFERENCE_READ);
    }

    @Test
    public void testDeleteAccessKeysByReference_noMatch() {
        Mockito.when(accessKeyRepositoryMock.deleteByReference(TEST_REFERENCE_READ)).thenReturn(0l);
        final boolean deleted = accessKeyService.deleteAccessKeysByReference(TEST_REFERENCE_READ);
        assertFalse(deleted);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).deleteByReference(TEST_REFERENCE_READ);
    }

    @Test
    public void testDeleteAccessKeysByReferenceAndPermission() {
        Mockito.when(accessKeyRepositoryMock.deleteByReferenceAndPermission(TEST_REFERENCE_READ, TEST_PERMISSION_READ))
            .thenReturn(1l);
        final boolean deleted
            = accessKeyService.deleteAccessKeysByReferenceAndPermission(TEST_REFERENCE_READ, TEST_PERMISSION_READ);
        assertTrue(deleted);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).deleteByReferenceAndPermission(TEST_REFERENCE_READ,
            TEST_PERMISSION_READ);
    }

    @Test
    public void testDeleteAccessKeysByReferenceAndPermission_noMatch() {
        Mockito.when(accessKeyRepositoryMock.deleteByReferenceAndPermission(TEST_REFERENCE_READ, TEST_PERMISSION_READ))
            .thenReturn(0l);
        final boolean deleted
            = accessKeyService.deleteAccessKeysByReferenceAndPermission(TEST_REFERENCE_READ, TEST_PERMISSION_READ);
        assertFalse(deleted);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).deleteByReferenceAndPermission(TEST_REFERENCE_READ,
            TEST_PERMISSION_READ);
    }

    @Test
    public void testDeleteAllAccessKeys() {
        accessKeyService.deleteAllAccessKeys();
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).deleteAll();
    }

    @Test
    public void testDeleteAllAccessKeys_noExists() {
        accessKeyService.deleteAllAccessKeys();
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).deleteAll();
    }

    @Test
    public void testExistsAccessKeyWithReferenceAndValue() {
        Mockito.when(accessKeyRepositoryMock.existsByReferenceAndValue(TEST_REFERENCE_READ, testEncodedValueRead))
            .thenReturn(true);
        final boolean check
            = accessKeyService.existsAccessKeyWithReferenceAndEncodedValue(TEST_REFERENCE_READ, TEST_VALUE_READ);
        assertTrue(check);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).existsByReferenceAndValue(TEST_REFERENCE_READ,
            testEncodedValueRead);
    }

    @Test
    public void testExistsAccessKeyWithReferenceAndValue_notExists() {
        final boolean check
            = accessKeyService.existsAccessKeyWithReferenceAndEncodedValue(TEST_REFERENCE_READ, TEST_VALUE_WRITE);
        assertFalse(check);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).existsByReferenceAndValue(TEST_REFERENCE_READ,
            testEncodedValueWrite);
    }

    private void compareReadAccessKey(MCRAccessKeyDto accessKeyDto) {
        assertEquals(TEST_ID_READ, accessKeyDto.getId());
        assertEquals(TEST_REFERENCE_READ, accessKeyDto.getReference());
        assertEquals(TEST_PERMISSION_READ, accessKeyDto.getPermission());
        assertEquals(TEST_VALUE_READ, accessKeyDto.getValue());
    }

    private MCRAccessKey getReadAccessKey() {
        final MCRAccessKey accessKey = new MCRAccessKey(TEST_REFERENCE_READ, TEST_PERMISSION_READ, TEST_VALUE_READ);
        accessKey.setUuid(TEST_ID_READ);
        return accessKey;
    }

    private MCRAccessKey getWriteAccessKey() {
        final MCRAccessKey accessKey = new MCRAccessKey(TEST_REFERENCE_WRITE, TEST_PERMISSION_WRITE, TEST_VALUE_WRITE);
        accessKey.setUuid(TEST_ID_WRITE);
        return accessKey;
    }
}
