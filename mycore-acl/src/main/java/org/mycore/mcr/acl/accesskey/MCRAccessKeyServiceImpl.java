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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessCacheHelper;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionHelper;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyCollisionException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.mcr.acl.accesskey.mapper.MCRAccessKeyMapper;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;
import org.mycore.mcr.acl.accesskey.persistence.MCRAccessKeyRepository;
import org.mycore.mcr.acl.accesskey.validation.MCRAccessKeyValidator;
import org.mycore.mcr.acl.accesskey.value.MCRAccessKeyValueProcessor;
import org.mycore.util.concurrent.MCRTransactionableCallable;

/**
 * Implementation of the {@link MCRAccessKeyService} interface for managing access keys.
 *
 * <p>
 * This service provides various methods to manage access keys in the MyCoRe system,
 * including creating, updating, deleting, and querying access keys by different criteria.
 * </p>
 */
public class MCRAccessKeyServiceImpl implements MCRAccessKeyService {

    private static final Logger LOGGER = LogManager.getLogger();

    private final MCRAccessKeyRepository accessKeyRepository;

    private final MCRAccessKeyValidator accessKeyValidator;

    private final MCRAccessKeyValueProcessor valueProcessor;

    /**
     * Constructs a new {@link MCRAccessKeyServiceImpl} instance.
     *
     * @param accessKeyRepository the access key repository
     * @param accessKeyValidator the access key validator
     * @param valueProcessor the value processor
     */
    public MCRAccessKeyServiceImpl(MCRAccessKeyRepository accessKeyRepository, MCRAccessKeyValidator accessKeyValidator,
        MCRAccessKeyValueProcessor valueProcessor) {
        this.accessKeyRepository = accessKeyRepository;
        this.accessKeyValidator = accessKeyValidator;
        this.valueProcessor = valueProcessor;
    }

    @Override
    public List<MCRAccessKeyDto> listAllAccessKeys() {
        return accessKeyRepository.findAll().stream().map(a -> fixAccessKeyUuidIfRequired(a))
            .map(MCRAccessKeyMapper::toDto).toList();
    }

    @Override
    public List<MCRAccessKeyDto> findAccessKeysByReference(String reference) {
        return accessKeyRepository.findByReference(reference).stream().map(a -> fixAccessKeyUuidIfRequired(a))
            .map(MCRAccessKeyMapper::toDto).toList();
    }

    @Override
    public List<MCRAccessKeyDto> findAccessKeysByPermission(String permission) {
        return accessKeyRepository.findByPermission(permission).stream().map(a -> fixAccessKeyUuidIfRequired(a))
            .map(MCRAccessKeyMapper::toDto).toList();
    }

    @Override
    public List<MCRAccessKeyDto> findAccessKeysByReferenceAndPermission(String reference, String permission) {
        return accessKeyRepository.findByReferenceAndPermission(reference, permission).stream()
            .map(a -> fixAccessKeyUuidIfRequired(a)).map(MCRAccessKeyMapper::toDto).toList();
    }

    @Override
    public MCRAccessKeyDto findAccessKey(UUID id) {
        return accessKeyRepository.findByUuid(id).map(a -> fixAccessKeyUuidIfRequired(a)).map(MCRAccessKeyMapper::toDto)
            .orElseThrow(() -> new MCRAccessKeyNotFoundException("Access key with given reference does not exist"));
    }

    @Override
    public MCRAccessKeyDto findAccessKeyByReferenceAndValue(String reference, String value) {
        return accessKeyRepository.findByReferenceAndValue(reference, value).map(a -> fixAccessKeyUuidIfRequired(a))
            .map(MCRAccessKeyMapper::toDto).orElse(null);
    }

    @Override
    public MCRAccessKeyDto addAccessKey(MCRAccessKeyDto accessKeyDto) {
        accessKeyValidator.validateAccessKeyDto(accessKeyDto);
        final String encodedValue = valueProcessor.getValue(accessKeyDto.getReference(), accessKeyDto.getValue());
        if (accessKeyRepository.existsByReferenceAndValue(accessKeyDto.getReference(), encodedValue)) {
            throw new MCRAccessKeyCollisionException("Given access key value collides with another access key");
        }
        final MCRAccessKey accessKey = MCRAccessKeyMapper.toEntity(accessKeyDto);
        accessKey.setValue(encodedValue);
        if (accessKeyDto.getActive() == null) {
            accessKey.setIsActive(true);
        }
        accessKey.setCreated(new Date());
        accessKey.setCreatedBy(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID());
        final MCRAccessKey savedAccessKey = accessKeyRepository.save(accessKey);
        MCRAccessCacheHelper.clearAllPermissionCaches(savedAccessKey.getReference());
        return MCRAccessKeyMapper.toDto(savedAccessKey);
    }

    @Override
    public MCRAccessKeyDto importAccessKey(MCRAccessKeyDto accessKeyDto) {
        accessKeyValidator.validateAccessKeyDto(accessKeyDto);
        if (accessKeyRepository.existsByReferenceAndValue(accessKeyDto.getReference(), accessKeyDto.getValue())) {
            throw new MCRAccessKeyCollisionException("Given access key value collides with another access key");
        }
        final MCRAccessKey accessKey = MCRAccessKeyMapper.toEntity(accessKeyDto);
        final MCRAccessKey savedAccessKey = accessKeyRepository.save(accessKey);
        MCRAccessCacheHelper.clearAllPermissionCaches(savedAccessKey.getReference());
        return MCRAccessKeyMapper.toDto(savedAccessKey);
    }

    @Override
    public MCRAccessKeyDto updateAccessKey(UUID id, MCRAccessKeyDto accessKeyDto) {
        final MCRAccessKey accessKey = accessKeyRepository.findByUuid(id)
            .orElseThrow(() -> new MCRAccessKeyNotFoundException("access key with given reference does not exist"));
        accessKeyValidator.validateAccessKeyDto(accessKeyDto);
        MCRAccessCacheHelper.clearAllPermissionCaches(accessKey.getReference());
        if (!Objects.equals(accessKey.getPermission(), accessKeyDto.getPermission())) {
            accessKey.setPermission(accessKeyDto.getPermission());
        }
        final String value = valueProcessor.getValue(accessKeyDto.getReference(), accessKeyDto.getValue());
        if (!Objects.equals(accessKey.getValue(), value)) {
            if (accessKeyRepository.existsByReferenceAndValue(accessKeyDto.getReference(), value)) {
                throw new MCRAccessKeyCollisionException("Given access key value collides with another access key");
            }
            accessKey.setValue(value);
        }
        accessKey.setReference(accessKeyDto.getReference());
        accessKey.setComment(accessKeyDto.getComment());
        accessKey.setLastModified(new Date());
        accessKey.setLastModifiedBy(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID());
        accessKey.setExpiration(accessKeyDto.getExpiration());
        accessKey.setIsActive(accessKeyDto.getActive());
        final MCRAccessKey savedAccessKey = accessKeyRepository.save(accessKey);
        MCRAccessCacheHelper.clearAllPermissionCaches(savedAccessKey.getReference());
        return MCRAccessKeyMapper.toDto(savedAccessKey);
    }

    @Override
    public MCRAccessKeyDto partialUpdateAccessKey(UUID id, MCRAccessKeyPartialUpdateDto accessKeyDto) {
        final MCRAccessKey accessKey = accessKeyRepository.findByUuid(id)
            .orElseThrow(() -> new MCRAccessKeyNotFoundException("access key with given reference does not exist"));
        accessKeyValidator.validateAccessKeyPartialUpdateDto(accessKeyDto);
        MCRAccessCacheHelper.clearAllPermissionCaches(accessKey.getReference());
        accessKeyDto.getPermission().getOptional().ifPresent(accessKey::setPermission);
        accessKeyDto.getReference().getOptional().ifPresent(accessKey::setReference);
        if (accessKeyDto.getValue().isPresent()) {
            final String value = valueProcessor.getValue(accessKey.getReference(), accessKeyDto.getValue().get());
            if (!Objects.equals(accessKey.getValue(), value)) {
                if (accessKeyRepository.existsByReferenceAndValue(accessKey.getReference(), value)) {
                    throw new MCRAccessKeyCollisionException("Given access key value collides with another access key");
                }
                accessKey.setValue(value);
            }
        }
        accessKeyDto.getActive().getOptional().ifPresent(accessKey::setIsActive);
        accessKeyDto.getComment().getOptional().ifPresent(accessKey::setComment);
        accessKeyDto.getExpiration().getOptional().ifPresent(accessKey::setExpiration);
        accessKey.setLastModified(new Date());
        accessKey.setLastModifiedBy(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID());
        final MCRAccessKey savedAccessKey = accessKeyRepository.save(accessKey);
        MCRAccessCacheHelper.clearAllPermissionCaches(savedAccessKey.getReference());
        return MCRAccessKeyMapper.toDto(savedAccessKey);
    }

    @Override
    public void removeAccessKey(UUID id) {
        final MCRAccessKey accessKey = accessKeyRepository.findByUuid(id)
            .orElseThrow(() -> new MCRAccessKeyNotFoundException("access key with given reference does not exist"));
        accessKeyRepository.delete(accessKey);
        MCRAccessCacheHelper.clearAllPermissionCaches(accessKey.getReference());
    }

    @Override
    public boolean removeAccessKeysByReference(String reference) {
        final List<MCRAccessKey> accessKeys = new ArrayList<>(accessKeyRepository.findByReference(reference));
        deleteAccessKeys(accessKeys);
        return accessKeys.size() > 0;
    }

    @Override
    public boolean removeAccessKeysByReferenceAndPermission(String reference, String permission) {
        final List<MCRAccessKey> accessKeys = new ArrayList<>(
            accessKeyRepository.findByReferenceAndPermission(reference, permission));
        deleteAccessKeys(accessKeys);
        return accessKeys.size() > 0;
    }

    @Override
    public void removeAllAccessKeys() {
        deleteAccessKeys(new ArrayList<>(accessKeyRepository.findAll()));
    }

    private void deleteAccessKeys(List<MCRAccessKey> accessKeys) {
        accessKeys.forEach(accessKeyRepository::delete);
        accessKeys.forEach(a -> MCRAccessCacheHelper.clearAllPermissionCaches(a.getReference()));
    }

    @Override
    public String processValue(String reference, String rawValue) {
        return valueProcessor.getValue(reference, rawValue);
    }

    /**
     * Ensures that an access key has a UUID, generating one if necessary.
     *
     * @param accessKey the access key that may require a UUID fix
     * @return the access key with a valid UUID
     */
    private MCRAccessKey fixAccessKeyUuidIfRequired(MCRAccessKey accessKey) {
        if (accessKey.getUuid() == null) {
            accessKey.setUuid(UUID.randomUUID());
            MCRAccessKey fixedAccessKey;
            if (!MCRTransactionHelper.isTransactionActive()) {
                try {
                    fixedAccessKey = new MCRTransactionableCallable<>(() -> {
                        return accessKeyRepository.save(accessKey);
                    }).call();
                } catch (Exception e) {
                    throw new MCRAccessKeyException("Error while fixing uuid");
                }
            } else {
                fixedAccessKey = accessKeyRepository.save(accessKey);
                accessKeyRepository.flush();
            }
            LOGGER.info("fixed access key with uuid: {}", fixedAccessKey.getUuid());
            return fixedAccessKey;
        }
        return accessKey;
    }

}
