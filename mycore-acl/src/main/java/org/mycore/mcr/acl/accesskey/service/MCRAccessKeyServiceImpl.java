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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessCacheHelper;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionManager;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyCollisionException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.mcr.acl.accesskey.mapper.MCRAccessKeyMapper;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;
import org.mycore.mcr.acl.accesskey.persistence.MCRAccessKeyRepository;
import org.mycore.mcr.acl.accesskey.service.processor.MCRAccessKeySecretProcessor;
import org.mycore.mcr.acl.accesskey.validation.MCRAccessKeyValidator;
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

    private final MCRAccessKeySecretProcessor secretProcessor;

    /**
     * Constructs a new {@link MCRAccessKeyServiceImpl} instance.
     *
     * @param accessKeyRepository the access key repository
     * @param accessKeyValidator the access key validator
     * @param secretProcessor the secret processor
     */
    public MCRAccessKeyServiceImpl(MCRAccessKeyRepository accessKeyRepository, MCRAccessKeyValidator accessKeyValidator,
        MCRAccessKeySecretProcessor secretProcessor) {
        this.accessKeyRepository = accessKeyRepository;
        this.accessKeyValidator = accessKeyValidator;
        this.secretProcessor = secretProcessor;
    }

    @Override
    public List<MCRAccessKeyDto> listAllAccessKeys() {
        return accessKeyRepository.findAll().stream().map(this::fixAccessKeyUuidIfRequired)
            .map(MCRAccessKeyMapper::toDto).toList();
    }

    @Override
    public List<MCRAccessKeyDto> findAccessKeysByReference(String reference) {
        return accessKeyRepository.findByReference(reference).stream().map(this::fixAccessKeyUuidIfRequired)
            .map(MCRAccessKeyMapper::toDto).toList();
    }

    @Override
    public List<MCRAccessKeyDto> findAccessKeysByPermission(String permission) {
        return accessKeyRepository.findByType(permission).stream().map(this::fixAccessKeyUuidIfRequired)
            .map(MCRAccessKeyMapper::toDto).toList();
    }

    @Override
    public List<MCRAccessKeyDto> findAccessKeysByReferenceAndPermission(String reference, String permission) {
        return accessKeyRepository.findByReferenceAndType(reference, permission).stream()
            .map(this::fixAccessKeyUuidIfRequired).map(MCRAccessKeyMapper::toDto).toList();
    }

    @Override
    public MCRAccessKeyDto findAccessKey(UUID id) {
        return accessKeyRepository.findByUuid(id).map(this::fixAccessKeyUuidIfRequired).map(MCRAccessKeyMapper::toDto)
            .orElseThrow(() -> new MCRAccessKeyNotFoundException("Access key with given reference does not exist"));
    }

    @Override
    public MCRAccessKeyDto findAccessKeyByReferenceAndSecret(String reference, String secret) {
        return accessKeyRepository.findByReferenceAndSecret(reference, secret).map(this::fixAccessKeyUuidIfRequired)
            .map(MCRAccessKeyMapper::toDto).orElse(null);
    }

    @Override
    public MCRAccessKeyDto addAccessKey(MCRAccessKeyDto accessKeyDto) {
        accessKeyValidator.validateAccessKeyDto(accessKeyDto);
        final String processedSecret
            = secretProcessor.processSecret(accessKeyDto.getReference(), accessKeyDto.getSecret());
        validateNoSecretConflict(accessKeyDto.getReference(), processedSecret);
        final MCRAccessKey accessKey = MCRAccessKeyMapper.toEntity(accessKeyDto);
        accessKey.setSecret(processedSecret);
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
        validateNoSecretConflict(accessKeyDto.getReference(), accessKeyDto.getSecret());
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
        if (!Objects.equals(accessKey.getType(), accessKeyDto.getPermission())) {
            accessKey.setType(accessKeyDto.getPermission());
        }
        final String secret = secretProcessor.processSecret(accessKeyDto.getReference(), accessKeyDto.getSecret());
        if (!Objects.equals(accessKey.getSecret(), secret)) {
            validateNoSecretConflict(accessKeyDto.getReference(), secret);
            accessKey.setSecret(secret);
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
        accessKeyDto.getPermission().getOptional().ifPresent(accessKey::setType);
        accessKeyDto.getReference().getOptional().ifPresent(accessKey::setReference);
        if (accessKeyDto.getSecret().isPresent()) {
            final String secret
                = secretProcessor.processSecret(accessKey.getReference(), accessKeyDto.getSecret().get());
            if (!Objects.equals(accessKey.getSecret(), secret)) {
                validateNoSecretConflict(accessKey.getReference(), secret);
                accessKey.setSecret(secret);
            }
        }
        accessKeyDto.getActive().getOptional().ifPresent(accessKey::setIsActive);
        if (accessKeyDto.getComment().isPresent()) {
            accessKey.setComment(accessKeyDto.getComment().get());
        }
        if (accessKeyDto.getExpiration().isPresent()) {
            accessKey.setExpiration(accessKeyDto.getExpiration().get());
        }
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
        final Collection<MCRAccessKey> accessKeys = accessKeyRepository.findByReference(reference);
        deleteAccessKeys(accessKeys);
        return !accessKeys.isEmpty();
    }

    @Override
    public boolean removeAccessKeysByReferenceAndPermission(String reference, String permission) {
        final Collection<MCRAccessKey> accessKeys
            = accessKeyRepository.findByReferenceAndType(reference, permission);
        deleteAccessKeys(accessKeys);
        return !accessKeys.isEmpty();
    }

    @Override
    public void removeAllAccessKeys() {
        deleteAccessKeys(accessKeyRepository.findAll());
    }

    @Override
    public String processSecret(String reference, String rawSecret) {
        return secretProcessor.processSecret(reference, rawSecret);
    }

    private void deleteAccessKeys(Collection<MCRAccessKey> accessKeys) {
        accessKeys.forEach(accessKeyRepository::delete);
        accessKeys.forEach(a -> MCRAccessCacheHelper.clearAllPermissionCaches(a.getReference()));
    }

    /**
     * Ensures that an access key has a UUID, generating one if necessary.
     *
     * @param accessKey the access key that may require a UUID fix
     * @return the access key with a valid UUID
     * @throws MCRException if cannot fix UUID
     */
    private MCRAccessKey fixAccessKeyUuidIfRequired(MCRAccessKey accessKey) {
        if (accessKey.getUuid() == null) {
            accessKey.setUuid(UUID.randomUUID());
            MCRAccessKey fixedAccessKey;
            if (!MCRTransactionManager.hasActiveTransactions()) {
                try {
                    fixedAccessKey = new MCRTransactionableCallable<>(() -> {
                        return accessKeyRepository.save(accessKey);
                    }).call();
                } catch (Exception e) {
                    throw new MCRException("Error while fixing uuid", e);
                }
            } else {
                fixedAccessKey = accessKeyRepository.save(accessKey);
                accessKeyRepository.flush();
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Fixed access key with uuid: {}", fixedAccessKey.getUuid());
            }
            return fixedAccessKey;
        }
        return accessKey;
    }

    private void validateNoSecretConflict(String reference, String secret) {
        if (accessKeyRepository.existsByReferenceAndSecret(reference, secret)) {
            throw new MCRAccessKeyCollisionException("Given access key secret collides with another access key");
        }
    }

}
