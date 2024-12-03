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
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRTestCase;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;
import org.mycore.mcr.acl.accesskey.dto.util.MCRNullable;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyCollisionException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.mcr.acl.accesskey.mapper.MCRAccessKeyMapper;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;
import org.mycore.mcr.acl.accesskey.persistence.MCRAccessKeyRepository;
import org.mycore.mcr.acl.accesskey.service.processor.MCRAccessKeyHashSecretProcessor;
import org.mycore.mcr.acl.accesskey.service.processor.MCRAccessKeySecretProcessor;
import org.mycore.mcr.acl.accesskey.validation.MCRAccessKeyValidatorImpl;

public class MCRAccessKeyServiceImplTest extends MCRTestCase {

    private MCRAccessKeyRepository accessKeyRepositoryMock;

    private MCRAccessKeySecretProcessor accessKeySecretProcessor;

    private MCRAccessKeyServiceImpl accessKeyService;

    private static final UUID TEST_ID_READ = UUID.randomUUID();

    private static final UUID TEST_ID_WRITE = UUID.randomUUID();

    private static final String TEST_REFERENCE_READ = "testReference1";

    private static final String TEST_REFERENCE_WRITE = "testReference2";

    private static final String TEST_PERMISSION_READ = "read";

    private static final String TEST_PERMISSION_WRITE = "writedb";

    private static final String TEST_SECRET_READ = "readSecret";

    private static final String TEST_SECRET_WRITE = "writeSecret";

    private static String testEncodedSecretRead;

    private static String testEncodedSecretWrite;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        accessKeyRepositoryMock = Mockito.mock(MCRAccessKeyRepository.class);
        accessKeySecretProcessor = new MCRAccessKeyHashSecretProcessor(1);
        Mockito.when(accessKeyRepositoryMock.existsByReferenceAndSecret(TEST_REFERENCE_READ, testEncodedSecretWrite))
            .thenReturn(false);
        accessKeyService = new MCRAccessKeyServiceImpl(accessKeyRepositoryMock, new MCRAccessKeyValidatorImpl(),
            accessKeySecretProcessor);
        testEncodedSecretRead = accessKeySecretProcessor.processSecret(TEST_REFERENCE_READ, TEST_SECRET_READ);
        testEncodedSecretWrite = accessKeySecretProcessor.processSecret(TEST_REFERENCE_WRITE, TEST_SECRET_WRITE);
    }

    @Test
    public void testGetAllAccessKeys() {
        final List<MCRAccessKey> accessKeys = new ArrayList<>();
        accessKeys.add(getReadAccessKey());
        accessKeys.add(getWriteAccessKey());
        Mockito.when(accessKeyRepositoryMock.findAll()).thenReturn(accessKeys);
        final List<MCRAccessKeyDto> resultAccessKeys = accessKeyService.listAllAccessKeys();
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
        accessKeyNoUuuid.setType(TEST_PERMISSION_READ);
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
        final List<MCRAccessKeyDto> accessKeyDtos = accessKeyService.listAllAccessKeys();
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
        final List<MCRAccessKeyDto> resultAccessKeys = accessKeyService.listAllAccessKeys();
        assertNotNull(resultAccessKeys);
        assertEquals(0, resultAccessKeys.size());
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findAll();
    }

    @Test
    public void testGetAccessKeysByReference() {
        final List<MCRAccessKey> accessKeys = new ArrayList<>();
        accessKeys.add(getReadAccessKey());
        Mockito.when(accessKeyRepositoryMock.findByReference(TEST_REFERENCE_READ)).thenReturn(accessKeys);
        final List<MCRAccessKeyDto> resultAccessKeys = accessKeyService.findAccessKeysByReference(TEST_REFERENCE_READ);
        assertNotNull(resultAccessKeys);
        assertEquals(1, resultAccessKeys.size());
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByReference(TEST_REFERENCE_READ);
    }

    @Test
    public void testGetAccessKeysByReference_empty() {
        Mockito.when(accessKeyRepositoryMock.findByReference(TEST_REFERENCE_READ)).thenReturn(new ArrayList<>());
        final List<MCRAccessKeyDto> resultAccessKeys = accessKeyService.findAccessKeysByReference(TEST_REFERENCE_READ);
        assertNotNull(resultAccessKeys);
        assertEquals(0, resultAccessKeys.size());
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByReference(TEST_REFERENCE_READ);
    }

    @Test
    public void testGetAccessKeysByReferenceAndPermission() {
        final List<MCRAccessKey> accessKeys = new ArrayList<>();
        accessKeys.add(getReadAccessKey());
        Mockito.when(accessKeyRepositoryMock.findByReferenceAndType(TEST_REFERENCE_READ, TEST_PERMISSION_READ))
            .thenReturn(accessKeys);
        final List<MCRAccessKeyDto> resultAccessKeys
            = accessKeyService.findAccessKeysByReferenceAndPermission(TEST_REFERENCE_READ, TEST_PERMISSION_READ);
        assertNotNull(resultAccessKeys);
        assertEquals(1, resultAccessKeys.size());
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByReferenceAndType(TEST_REFERENCE_READ,
            TEST_PERMISSION_READ);
    }

    @Test
    public void testGetAccessKeysByReferenceAndPermission_empty() {
        Mockito.when(accessKeyRepositoryMock.findByReferenceAndType(TEST_REFERENCE_READ, TEST_PERMISSION_READ))
            .thenReturn(new ArrayList<>());
        final List<MCRAccessKeyDto> resultAccessKeys
            = accessKeyService.findAccessKeysByReferenceAndPermission(TEST_REFERENCE_READ, TEST_PERMISSION_READ);
        assertNotNull(resultAccessKeys);
        assertEquals(0, resultAccessKeys.size());
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByReferenceAndType(TEST_REFERENCE_READ,
            TEST_PERMISSION_READ);
    }

    @Test
    public void testGetAccessKeyById() {
        Mockito.when(accessKeyRepositoryMock.findByUuid(TEST_ID_READ)).thenReturn(Optional.of(getReadAccessKey()));
        final MCRAccessKeyDto accessKeyDto = accessKeyService.findAccessKey(TEST_ID_READ);
        assertNotNull(accessKeyDto);
        compareReadAccessKey(accessKeyDto);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByUuid(TEST_ID_READ);
    }

    public void testGetAccessKeyById_notExists() {
        Mockito.when(accessKeyRepositoryMock.findByUuid(Mockito.any())).thenReturn(Optional.empty());
        assertThrows(MCRAccessKeyNotFoundException.class, () -> accessKeyService.findAccessKey(TEST_ID_READ));
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByUuid(Mockito.any());
    }

    @Test
    public void testGetAccessKeyByReferenceAndSecret() {
        Mockito.when(accessKeyRepositoryMock.findByReferenceAndSecret(TEST_REFERENCE_READ, testEncodedSecretRead))
            .thenReturn(Optional.of(getReadAccessKey()));
        final MCRAccessKeyDto accessKeyDto
            = accessKeyService.findAccessKeyByReferenceAndSecret(TEST_REFERENCE_READ, testEncodedSecretRead);
        assertNotNull(accessKeyDto);
        compareReadAccessKey(accessKeyDto);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByReferenceAndSecret(TEST_REFERENCE_READ,
            testEncodedSecretRead);
    }

    public void testGetAccessKeyByReferenceAndSecret_notExists() {
        Mockito.when(accessKeyRepositoryMock.findByReferenceAndSecret(TEST_REFERENCE_READ, TEST_SECRET_READ))
            .thenReturn(Optional.empty());
        assertThrows(MCRAccessKeyNotFoundException.class,
            () -> accessKeyService.findAccessKeyByReferenceAndSecret(TEST_REFERENCE_READ, TEST_SECRET_READ));
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByReferenceAndSecret(TEST_REFERENCE_READ,
            testEncodedSecretRead);
    }

    @Test
    public void testCreateAccessKey() throws MCRAccessException {
        final MCRAccessKey accessKey = new MCRAccessKey(TEST_REFERENCE_READ, TEST_PERMISSION_READ, TEST_SECRET_READ);
        final MCRAccessKeyDto createAccessKeyDto = MCRAccessKeyMapper.toDto(accessKey);
        Mockito.when(accessKeyRepositoryMock.save(Mockito.any(MCRAccessKey.class))).thenAnswer(invocation -> {
            MCRAccessKey savedAccessKey = invocation.getArgument(0);
            savedAccessKey.setId(1);
            savedAccessKey.setUuid(TEST_ID_READ);
            return savedAccessKey;
        });
        Mockito.when(accessKeyRepositoryMock.existsByReferenceAndSecret(TEST_REFERENCE_READ, testEncodedSecretRead))
            .thenReturn(false);
        final MCRAccessKeyDto createdAccessKey = accessKeyService.addAccessKey(createAccessKeyDto);
        assertNotNull(createdAccessKey);
        assertNotNull(createdAccessKey.getCreated());
        assertNotNull(createdAccessKey.getCreatedBy());
        assertEquals(TEST_ID_READ, createdAccessKey.getId());
        assertEquals(testEncodedSecretRead, createdAccessKey.getSecret());
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).save(Mockito.any(MCRAccessKey.class));
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).existsByReferenceAndSecret(TEST_REFERENCE_READ,
            testEncodedSecretRead);
    }

    public void testCreateAccessKey_collision() {
        Mockito.when(accessKeyRepositoryMock.existsByReferenceAndSecret(TEST_REFERENCE_READ, testEncodedSecretRead))
            .thenReturn(true);
        assertThrows(MCRAccessKeyCollisionException.class,
            () -> accessKeyService.addAccessKey(MCRAccessKeyMapper.toDto(getReadAccessKey())));
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).existsByReferenceAndSecret(TEST_REFERENCE_READ,
            testEncodedSecretRead);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(0)).save(Mockito.any());
    }

    @Test
    public void testImportAccessKey() throws MCRAccessException {
        final MCRAccessKeyDto importAccessKeyDto = MCRAccessKeyMapper.toDto(getReadAccessKey());
        importAccessKeyDto.setCreated(new Date());
        importAccessKeyDto.setCreatedBy("bla");
        Mockito.when(accessKeyRepositoryMock.existsByReferenceAndSecret(TEST_REFERENCE_READ, TEST_SECRET_READ))
            .thenReturn(false);
        Mockito.when(accessKeyRepositoryMock.save(Mockito.any(MCRAccessKey.class))).thenAnswer(invocation -> {
            MCRAccessKey savedAccessKey = invocation.getArgument(0);
            savedAccessKey.setId(1);
            savedAccessKey.setUuid(TEST_ID_READ);
            return savedAccessKey;
        });
        final MCRAccessKeyDto importedAccessKeyDto = accessKeyService.importAccessKey(importAccessKeyDto);
        assertNotNull(importedAccessKeyDto);
        assertEquals(importAccessKeyDto.getSecret(), importedAccessKeyDto.getSecret());
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).existsByReferenceAndSecret(TEST_REFERENCE_READ,
            TEST_SECRET_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).save(Mockito.any(MCRAccessKey.class));
    }

    public void testImportAccessKey_collision() {
        Mockito.when(accessKeyRepositoryMock.existsByReferenceAndSecret(TEST_REFERENCE_READ, testEncodedSecretRead))
            .thenReturn(true);
        assertThrows(MCRAccessKeyCollisionException.class,
            () -> accessKeyService.importAccessKey(MCRAccessKeyMapper.toDto(getReadAccessKey())));
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).existsByReferenceAndSecret(TEST_REFERENCE_READ,
            testEncodedSecretRead);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(0)).save(Mockito.any());
    }

    @Test
    public void testUpdateAccessKeyById() throws MCRAccessException {
        final String newSecret = "newValue";
        final String processedNewSecret = accessKeySecretProcessor.processSecret(TEST_REFERENCE_READ, newSecret);
        final MCRAccessKey outdatedAccessKey = getReadAccessKey();
        outdatedAccessKey.setId(1);
        final MCRAccessKeyDto updateAccessKeyDto = MCRAccessKeyMapper.toDto(outdatedAccessKey);
        updateAccessKeyDto.setSecret(newSecret);
        final MCRAccessKey updateAccessKey = MCRAccessKeyMapper.toEntity(updateAccessKeyDto);
        updateAccessKey.setSecret(processedNewSecret);
        updateAccessKey.setId(1);
        Mockito.when(accessKeyRepositoryMock.findByUuid(TEST_ID_READ)).thenReturn(Optional.of(outdatedAccessKey));
        Mockito.when(accessKeyRepositoryMock.existsByReferenceAndSecret(TEST_REFERENCE_READ, processedNewSecret))
            .thenReturn(false);
        Mockito.when(accessKeyRepositoryMock.save(updateAccessKey)).thenReturn(updateAccessKey);
        final MCRAccessKeyDto updatedAccessKeyDto = accessKeyService.updateAccessKey(TEST_ID_READ, updateAccessKeyDto);
        assertEquals(MCRAccessKeyMapper.toDto(updateAccessKey), updatedAccessKeyDto);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByUuid(TEST_ID_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).existsByReferenceAndSecret(TEST_REFERENCE_READ,
            processedNewSecret);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).save(updateAccessKey);
    }

    @Test
    public void testUpdateAccessKeyById_collision() {
        final String newSecret = "newValue";
        final MCRAccessKey outdatedAccessKey = getReadAccessKey();
        final MCRAccessKeyDto updatedAccessKeyDto = MCRAccessKeyMapper.toDto(outdatedAccessKey);
        updatedAccessKeyDto.setSecret(newSecret);
        final String processedNewSecret = accessKeySecretProcessor.processSecret(TEST_REFERENCE_READ, newSecret);
        Mockito.when(accessKeyRepositoryMock.findByUuid(TEST_ID_READ)).thenReturn(Optional.of(outdatedAccessKey));
        Mockito.when(accessKeyRepositoryMock.existsByReferenceAndSecret(TEST_REFERENCE_READ, processedNewSecret))
            .thenReturn(true);
        assertThrows(MCRAccessKeyCollisionException.class,
            () -> accessKeyService.updateAccessKey(TEST_ID_READ, updatedAccessKeyDto));
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByUuid(TEST_ID_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).existsByReferenceAndSecret(TEST_REFERENCE_READ,
            processedNewSecret);
    }

    @Test
    public void testUpdateAccessKeyById_notExists() {
        final MCRAccessKey updatedAccessKey = getReadAccessKey();
        Mockito.when(accessKeyRepositoryMock.findByUuid(TEST_ID_READ)).thenReturn(Optional.empty());
        assertThrows(MCRAccessKeyNotFoundException.class, () -> accessKeyService.updateAccessKey(TEST_ID_READ,
            MCRAccessKeyMapper.toDto(updatedAccessKey)));
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByUuid(TEST_ID_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(0)).existsByReferenceAndSecret(Mockito.any(),
            Mockito.any());
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(0)).save(Mockito.any());
    }

    @Test
    public void testPartialUpdateAccessKeyById() throws MCRAccessException {
        final String newSecret = "newValue";
        final String processedNewSecret = accessKeySecretProcessor.processSecret(TEST_REFERENCE_READ, newSecret);
        final MCRAccessKeyPartialUpdateDto partialUpdateDto = new MCRAccessKeyPartialUpdateDto();
        partialUpdateDto.setSecret(new MCRNullable<>(newSecret));
        partialUpdateDto.setExpiration(new MCRNullable<>(null));
        final MCRAccessKey outdatedAccessKey = getReadAccessKey();
        outdatedAccessKey.setExpiration(new Date());
        final MCRAccessKey updatedAccessKey = getReadAccessKey();
        updatedAccessKey.setSecret(processedNewSecret);
        updatedAccessKey.setExpiration(null);
        Mockito.when(accessKeyRepositoryMock.findByUuid(TEST_ID_READ)).thenReturn(Optional.of(outdatedAccessKey));
        Mockito.when(accessKeyRepositoryMock.existsByReferenceAndSecret(TEST_REFERENCE_READ, processedNewSecret))
            .thenReturn(false);
        Mockito.when(accessKeyRepositoryMock.save(updatedAccessKey)).thenReturn(updatedAccessKey);
        final MCRAccessKeyDto updatedAccessKeyDto
            = accessKeyService.partialUpdateAccessKey(TEST_ID_READ, partialUpdateDto);
        assertNull(updatedAccessKeyDto.getExpiration());
        assertEquals(processedNewSecret, updatedAccessKeyDto.getSecret());
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByUuid(TEST_ID_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).save(updatedAccessKey);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).existsByReferenceAndSecret(TEST_REFERENCE_READ,
            processedNewSecret);
    }

    @Test
    public void testPartialUpdateAccessKeyById_notExists() {
        Mockito.when(accessKeyRepositoryMock.findByUuid(TEST_ID_READ)).thenReturn(Optional.empty());
        assertThrows(MCRAccessKeyNotFoundException.class,
            () -> accessKeyService.partialUpdateAccessKey(TEST_ID_READ, null));
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByUuid(TEST_ID_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(0)).save(Mockito.any());
    }

    @Test
    public void testDeleteAccessKeyById() throws MCRAccessException {
        Mockito.when(accessKeyRepositoryMock.findByUuid(TEST_ID_READ)).thenReturn(Optional.of(getReadAccessKey()));
        accessKeyService.removeAccessKey(TEST_ID_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByUuid(TEST_ID_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).delete(getReadAccessKey());
    }

    public void testDeleteAccessKeyById_notExists() {
        Mockito.when(accessKeyRepositoryMock.findByUuid(TEST_ID_READ)).thenReturn(Optional.empty());
        assertThrows(MCRAccessKeyNotFoundException.class, () -> accessKeyService.removeAccessKey(TEST_ID_READ));
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByUuid(TEST_ID_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(0)).delete(Mockito.any());
    }

    @Test
    public void testDeleteAccessKeysByReference() {
        final List<MCRAccessKey> accessKeys = new ArrayList<>();
        accessKeys.add(getReadAccessKey());
        Mockito.when(accessKeyRepositoryMock.findByReference(TEST_REFERENCE_READ)).thenReturn(accessKeys);
        final boolean deleted = accessKeyService.removeAccessKeysByReference(TEST_REFERENCE_READ);
        assertTrue(deleted);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByReference(TEST_REFERENCE_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).delete(getReadAccessKey());
    }

    @Test
    public void testDeleteAccessKeysByReference_noMatch() {
        Mockito.when(accessKeyRepositoryMock.findByReference(TEST_REFERENCE_READ)).thenReturn(new ArrayList<>());
        final boolean deleted = accessKeyService.removeAccessKeysByReference(TEST_REFERENCE_READ);
        assertFalse(deleted);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByReference(TEST_REFERENCE_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(0)).delete(Mockito.any(MCRAccessKey.class));
    }

    @Test
    public void testDeleteAccessKeysByReferenceAndPermission() {
        final List<MCRAccessKey> accessKeys = new ArrayList<>();
        accessKeys.add(getReadAccessKey());
        Mockito.when(accessKeyRepositoryMock.findByReferenceAndType(TEST_REFERENCE_READ, TEST_PERMISSION_READ))
            .thenReturn(accessKeys);
        final boolean deleted
            = accessKeyService.removeAccessKeysByReferenceAndPermission(TEST_REFERENCE_READ, TEST_PERMISSION_READ);
        assertTrue(deleted);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByReferenceAndType(TEST_REFERENCE_READ,
            TEST_PERMISSION_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).delete(getReadAccessKey());
    }

    @Test
    public void testDeleteAccessKeysByReferenceAndPermission_noMatch() {
        Mockito.when(accessKeyRepositoryMock.findByReferenceAndType(TEST_REFERENCE_READ, TEST_PERMISSION_READ))
            .thenReturn(new ArrayList<>());
        final boolean deleted
            = accessKeyService.removeAccessKeysByReferenceAndPermission(TEST_REFERENCE_READ, TEST_PERMISSION_READ);
        assertFalse(deleted);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findByReferenceAndType(TEST_REFERENCE_READ,
            TEST_PERMISSION_READ);
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(0)).delete(Mockito.any(MCRAccessKey.class));
    }

    @Test
    public void testDeleteAllAccessKeys() {
        final List<MCRAccessKey> accessKeys = new ArrayList<>();
        accessKeys.add(getReadAccessKey());
        Mockito.when(accessKeyRepositoryMock.findAll()).thenReturn(accessKeys);
        accessKeyService.removeAllAccessKeys();
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findAll();
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).delete(getReadAccessKey());
    }

    @Test
    public void testDeleteAllAccessKeys_noExists() {
        Mockito.when(accessKeyRepositoryMock.findAll()).thenReturn(new ArrayList<>());
        accessKeyService.removeAllAccessKeys();
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(1)).findAll();
        Mockito.verify(accessKeyRepositoryMock, Mockito.times(0)).delete(Mockito.any(MCRAccessKey.class));
    }

    private void compareReadAccessKey(MCRAccessKeyDto accessKeyDto) {
        assertEquals(TEST_ID_READ, accessKeyDto.getId());
        assertEquals(TEST_REFERENCE_READ, accessKeyDto.getReference());
        assertEquals(TEST_PERMISSION_READ, accessKeyDto.getPermission());
        assertEquals(TEST_SECRET_READ, accessKeyDto.getSecret());
    }

    private MCRAccessKey getReadAccessKey() {
        final MCRAccessKey accessKey = new MCRAccessKey(TEST_REFERENCE_READ, TEST_PERMISSION_READ, TEST_SECRET_READ);
        accessKey.setId(1);
        accessKey.setUuid(TEST_ID_READ);
        return accessKey;
    }

    private MCRAccessKey getWriteAccessKey() {
        final MCRAccessKey accessKey = new MCRAccessKey(TEST_REFERENCE_WRITE, TEST_PERMISSION_WRITE, TEST_SECRET_WRITE);
        accessKey.setId(2);
        accessKey.setUuid(TEST_ID_WRITE);
        return accessKey;
    }
}
