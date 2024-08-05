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

package org.mycore.mcr.acl.accesskey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRJPATestCase;
import org.mycore.mcr.acl.accesskey.access.MCRAccessKeyAccessService;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;
import org.mycore.mcr.acl.accesskey.dto.util.MCRNullable;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyCollisionException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.mcr.acl.accesskey.mapper.MCRAccessKeyMapper;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;
import org.mycore.mcr.acl.accesskey.persistence.MCRAccessKeyRepository;
import org.mycore.mcr.acl.accesskey.value.MCRAccessKeyHashValueProcessor;
import org.mycore.mcr.acl.accesskey.value.MCRAccessKeyValueProcessor;

public class MCRAccessKeyServiceImplTest extends MCRJPATestCase {

    private MCRAccessKeyRepository accessKeyRepositoryMock;

    private MCRAccessKeyValueProcessor accessKeyValueProcessor;

    private MCRAccessKeyAccessService accessKeyAccessService;

    private MCRAccessKeyServiceImpl accessKeyService;

    private static final UUID TEST_ID_READ = UUID.randomUUID();

    private static final UUID TEST_ID_WRITE = UUID.randomUUID();

    private static final String TEST_REFERENCE_READ = "testReference1";

    private static final String TEST_REFERENCE_WRITE = "testReference2";

    private static final String TEST_PERMISSION_READ = "read";

    private static final String TEST_PERMISSION_WRITE = "writedb";

    private static final String TEST_VALUE_READ = "readValue";

    private static final String TEST_VALUE_WRITE = "writeValue";

    private static String testEncodedValueRead;

    private static String testEncodedValueWrite;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        accessKeyRepositoryMock = Mockito.mock(MCRAccessKeyRepository.class);
        accessKeyAccessService = Mockito.mock(MCRAccessKeyAccessService.class);
        accessKeyValueProcessor = new MCRAccessKeyHashValueProcessor(1);
        Mockito.when(accessKeyRepositoryMock.existsByReferenceAndValue(TEST_REFERENCE_READ, testEncodedValueWrite))
            .thenReturn(false);
        Mockito.when(accessKeyAccessService.checkManagePermission(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(true);
        accessKeyService = new MCRAccessKeyServiceImpl(accessKeyRepositoryMock, accessKeyAccessService);
        accessKeyService.setValueProcessor(accessKeyValueProcessor);
        testEncodedValueRead = accessKeyValueProcessor.getValue(TEST_REFERENCE_READ, TEST_VALUE_READ);
        testEncodedValueWrite = accessKeyValueProcessor.getValue(TEST_REFERENCE_WRITE, TEST_VALUE_WRITE);
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
        accessKeyNoUuuid.setPermission(TEST_PERMISSION_READ);
        accessKeyNoUuuid.setReference(TEST_REFERENCE_READ);
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
        final List<MCRAccessKeyDto> resultAccessKeys = accessKeyService
            .getAccessKeysByReferenceAndPermission(TEST_REFERENCE_READ, TEST_PERMISSION_READ);
        assertNotNull(resultAccessKeys);
        assertEquals(1, resultAccessKeys.size());
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByReferenceAndPermission(TEST_REFERENCE_READ,
            TEST_PERMISSION_READ);
    }

    @Test
    public void testGetAccessKeysByReferenceAndPermission_empty() {
        Mockito.when(accessKeyRepositoryMock.findByReferenceAndPermission(TEST_REFERENCE_READ, TEST_PERMISSION_READ))
            .thenReturn(new ArrayList<>());
        final List<MCRAccessKeyDto> resultAccessKeys = accessKeyService
            .getAccessKeysByReferenceAndPermission(TEST_REFERENCE_READ, TEST_PERMISSION_READ);
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
        final MCRAccessKeyDto accessKeyDto = accessKeyService.getAccessKeyByReferenceAndValue(TEST_REFERENCE_READ,
            testEncodedValueRead);
        assertNotNull(accessKeyDto);
        compareReadAccessKey(accessKeyDto);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByReferenceAndValue(TEST_REFERENCE_READ,
            testEncodedValueRead);
    }

    public void testGetAccessKeyByReferenceAndRawValue_notExists() {
        Mockito.when(accessKeyRepositoryMock.findByReferenceAndValue(TEST_REFERENCE_READ, TEST_VALUE_READ))
            .thenReturn(Optional.empty());
        assertThrows(MCRAccessKeyNotFoundException.class,
            () -> accessKeyService.getAccessKeyByReferenceAndValue(TEST_REFERENCE_READ, TEST_VALUE_READ));
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByReferenceAndValue(TEST_REFERENCE_READ,
            testEncodedValueRead);
    }

    @Test
    public void testCreateAccessKey() throws MCRAccessException {
        final MCRAccessKey accessKey = new MCRAccessKey(TEST_REFERENCE_READ, TEST_PERMISSION_READ, TEST_VALUE_READ);
        final MCRAccessKeyDto createAccessKeyDto = MCRAccessKeyMapper.toDto(accessKey);
        accessKey.setSecret(testEncodedValueRead);
        accessKey.setUuid(TEST_ID_READ);
        Mockito.when(accessKeyRepositoryMock.save(accessKey)).thenReturn(accessKey);
        Mockito.when(accessKeyRepositoryMock.existsByReferenceAndValue(TEST_REFERENCE_READ, testEncodedValueRead))
            .thenReturn(false);
        final MCRAccessKeyDto createdAccessKey = accessKeyService.createAccessKey(createAccessKeyDto);
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
    public void testImportAccessKey() throws MCRAccessException {
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
    public void testUpdateAccessKeyById() throws MCRAccessException {
        final String newValue = "newValue";
        final String encodedNewValue = accessKeyValueProcessor.getValue(TEST_REFERENCE_READ, newValue);
        final MCRAccessKey outdatedAccessKey = getReadAccessKey();
        final MCRAccessKeyDto updateAccessKeyDto = MCRAccessKeyMapper.toDto(outdatedAccessKey);
        updateAccessKeyDto.setValue(newValue);
        final MCRAccessKey updateAccessKey = MCRAccessKeyMapper.toEntity(updateAccessKeyDto);
        updateAccessKey.setSecret(encodedNewValue);
        Mockito.when(accessKeyRepositoryMock.findByUuid(TEST_ID_READ)).thenReturn(Optional.of(getReadAccessKey()));
        Mockito.when(accessKeyRepositoryMock.existsByReferenceAndValue(TEST_REFERENCE_READ, encodedNewValue))
            .thenReturn(false);
        Mockito.when(accessKeyRepositoryMock.save(updateAccessKey)).thenReturn(updateAccessKey);
        final MCRAccessKeyDto updatedAccessKeyDto = accessKeyService.updateAccessKeyById(TEST_ID_READ,
            updateAccessKeyDto);
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
        final String encodedNewValue = accessKeyValueProcessor.getValue(TEST_REFERENCE_READ, newValue);
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
    public void testPartialUpdateAccessKeyById() throws MCRAccessException {
        final String newValue = "newValue";
        final String encodedNewValue = accessKeyValueProcessor.getValue(TEST_REFERENCE_READ, newValue);
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
        final MCRAccessKeyDto updatedAccessKeyDto = accessKeyService.partialUpdateAccessKeyById(TEST_ID_READ,
            partialUpdateDto);
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
    public void testDeleteAccessKeyById() throws MCRAccessException {
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
        final List<MCRAccessKey> accessKeys = new ArrayList<>();
        accessKeys.add(getReadAccessKey());
        Mockito.when(accessKeyRepositoryMock.findByReference(TEST_REFERENCE_READ))
            .thenReturn(accessKeys);
        final boolean deleted = accessKeyService.deleteAccessKeysByReference(TEST_REFERENCE_READ);
        assertTrue(deleted);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByReference(TEST_REFERENCE_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).delete(getReadAccessKey());
    }

    @Test
    public void testDeleteAccessKeysByReference_noMatch() {
        Mockito.when(accessKeyRepositoryMock.findByReference(TEST_REFERENCE_READ)).thenReturn(new ArrayList<>());
        final boolean deleted = accessKeyService.deleteAccessKeysByReference(TEST_REFERENCE_READ);
        assertFalse(deleted);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByReference(TEST_REFERENCE_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(0)).delete(Mockito.any(MCRAccessKey.class));
    }

    @Test
    public void testDeleteAccessKeysByReferenceAndPermission() {
        final List<MCRAccessKey> accessKeys = new ArrayList<>();
        accessKeys.add(getReadAccessKey());
        Mockito.when(accessKeyRepositoryMock.findByReferenceAndPermission(TEST_REFERENCE_READ, TEST_PERMISSION_READ))
            .thenReturn(accessKeys);
        final boolean deleted = accessKeyService.deleteAccessKeysByReferenceAndPermission(TEST_REFERENCE_READ,
            TEST_PERMISSION_READ);
        assertTrue(deleted);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByReferenceAndPermission(TEST_REFERENCE_READ,
            TEST_PERMISSION_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).delete(getReadAccessKey());
    }

    @Test
    public void testDeleteAccessKeysByReferenceAndPermission_noMatch() {
        Mockito.when(accessKeyRepositoryMock.findByReferenceAndPermission(TEST_REFERENCE_READ, TEST_PERMISSION_READ))
            .thenReturn(new ArrayList<>());
        final boolean deleted = accessKeyService.deleteAccessKeysByReferenceAndPermission(TEST_REFERENCE_READ,
            TEST_PERMISSION_READ);
        assertFalse(deleted);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByReferenceAndPermission(TEST_REFERENCE_READ,
            TEST_PERMISSION_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(0)).delete(Mockito.any(MCRAccessKey.class));
    }

    @Test
    public void testDeleteAllAccessKeys() {
        final List<MCRAccessKey> accessKeys = new ArrayList<>();
        accessKeys.add(getReadAccessKey());
        Mockito.when(accessKeyRepositoryMock.findAll()).thenReturn(accessKeys);
        accessKeyService.deleteAllAccessKeys();
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findAll();
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).delete(getReadAccessKey());
    }

    @Test
    public void testDeleteAllAccessKeys_noExists() {
        Mockito.when(accessKeyRepositoryMock.findAll()).thenReturn(new ArrayList<>());
        accessKeyService.deleteAllAccessKeys();
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findAll();
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(0)).delete(Mockito.any(MCRAccessKey.class));
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
