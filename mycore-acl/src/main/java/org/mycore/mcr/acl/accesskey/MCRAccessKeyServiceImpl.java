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

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessCacheHelper;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionHelper;
import org.mycore.mcr.acl.accesskey.access.MCRAccessKeyAccessService;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyManagementPermissionsDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyCollisionException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.mcr.acl.accesskey.mapper.MCRAccessKeyMapper;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;
import org.mycore.mcr.acl.accesskey.persistence.MCRAccessKeyRepository;
import org.mycore.mcr.acl.accesskey.validation.MCRAccessKeyValidatorImpl;
import org.mycore.mcr.acl.accesskey.value.MCRAccessKeyPlainValueProcessor;
import org.mycore.mcr.acl.accesskey.value.MCRAccessKeyValueProcessor;
import org.mycore.util.concurrent.MCRTransactionableCallable;

/**
 * Implementation of the {@link MCRAccessKeyService} interface for managing access keys.
 */
public class MCRAccessKeyServiceImpl implements MCRAccessKeyService {

    private static final Logger LOGGER = LogManager.getLogger();

    private final MCRAccessKeyRepository accessKeyRepository;

    private final MCRAccessKeyAccessService accessService;

    private MCRAccessKeyValueProcessor valueProcessor = new MCRAccessKeyPlainValueProcessor();

    /**
     * Constructs a new {@link MCRAccessKeyServiceImpl} instance.
     *
     * @param accessKeyRepository the access key repository
     * @param accessService the access service
     */
    public MCRAccessKeyServiceImpl(MCRAccessKeyRepository accessKeyRepository,
        MCRAccessKeyAccessService accessService) {
        this.accessKeyRepository = accessKeyRepository;
        this.accessService = accessService;
    }

    private MCRAccessKey fixAccessKeyUuidIfRequired(MCRAccessKey accessKey) {
        if (accessKey.getUuid() == null) {
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
            return fixedAccessKey;
        }
        return accessKey;
    }

    @Override
    public List<MCRAccessKeyDto> getAllAccessKeys() {
        return accessKeyRepository.findAll().stream().filter(a -> checkManagePermission(a))
            .map(a -> fixAccessKeyUuidIfRequired(a)).map(MCRAccessKeyMapper::toDto).toList();
    }

    @Override
    public List<MCRAccessKeyDto> getAccessKeysByReference(String reference) {
        return accessKeyRepository.findByReference(reference).stream().filter(a -> checkManagePermission(a))
            .map(a -> fixAccessKeyUuidIfRequired(a)).map(MCRAccessKeyMapper::toDto).toList();
    }

    @Override
    public List<MCRAccessKeyDto> getAccessKeysByPermission(String permission) {
        return accessKeyRepository.findByPermission(permission).stream().filter(a -> checkManagePermission(a))
            .map(a -> fixAccessKeyUuidIfRequired(a)).map(MCRAccessKeyMapper::toDto).toList();
    }

    @Override
    public List<MCRAccessKeyDto> getAccessKeysByReferenceAndPermission(String reference, String permission) {
        return accessKeyRepository.findByReferenceAndPermission(reference, permission).stream()
            .filter(a -> checkManagePermission(a)).map(a -> fixAccessKeyUuidIfRequired(a))
            .map(MCRAccessKeyMapper::toDto).toList();
    }

    @Override
    public MCRAccessKeyDto getAccessKeyById(UUID id) {
        return accessKeyRepository.findByUuid(id).filter(a -> checkManagePermission(a))
            .map(a -> fixAccessKeyUuidIfRequired(a)).map(MCRAccessKeyMapper::toDto)
            .orElseThrow(() -> new MCRAccessKeyNotFoundException("Access key with given reference does not exist"));
    }

    @Override
    public MCRAccessKeyDto getAccessKeyByReferenceAndValue(String reference, String value) {
        return accessKeyRepository.findByReferenceAndValue(reference, value)
            .filter(a -> checkManagePermission(a)).map(a -> fixAccessKeyUuidIfRequired(a))
            .map(MCRAccessKeyMapper::toDto).orElse(null);
    }

    @Override
    public MCRAccessKeyDto createAccessKey(MCRAccessKeyDto accessKeyDto) throws MCRAccessException {
        MCRAccessKeyValidatorImpl.getInstance().validateAccessKeyDto(accessKeyDto);
        validateManagePermission(accessKeyDto);
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
    public MCRAccessKeyDto importAccessKey(MCRAccessKeyDto accessKeyDto) throws MCRAccessException {
        MCRAccessKeyValidatorImpl.getInstance().validateAccessKeyDto(accessKeyDto);
        validateManagePermission(accessKeyDto);
        if (accessKeyRepository.existsByReferenceAndValue(accessKeyDto.getReference(), accessKeyDto.getValue())) {
            throw new MCRAccessKeyCollisionException("Given access key value collides with another access key");
        }
        final MCRAccessKey accessKey = MCRAccessKeyMapper.toEntity(accessKeyDto);
        final MCRAccessKey savedAccessKey = accessKeyRepository.save(accessKey);
        MCRAccessCacheHelper.clearAllPermissionCaches(savedAccessKey.getReference());
        return MCRAccessKeyMapper.toDto(savedAccessKey);
    }

    @Override
    public MCRAccessKeyDto updateAccessKeyById(UUID id, MCRAccessKeyDto accessKeyDto) throws MCRAccessException {
        final MCRAccessKey accessKey = accessKeyRepository.findByUuid(id)
            .orElseThrow(() -> new MCRAccessKeyNotFoundException("access key with given reference does not exist"));
        validateManagePermission(accessKey);
        MCRAccessKeyValidatorImpl.getInstance().validateAccessKeyDto(accessKeyDto);
        validateManagePermission(accessKeyDto);
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
    public MCRAccessKeyDto partialUpdateAccessKeyById(UUID id, MCRAccessKeyPartialUpdateDto accessKeyDto)
        throws MCRAccessException {
        final MCRAccessKey accessKey = accessKeyRepository.findByUuid(id)
            .orElseThrow(() -> new MCRAccessKeyNotFoundException("access key with given reference does not exist"));
        validateManagePermission(accessKey);
        MCRAccessKeyValidatorImpl.getInstance().validateAccessKeyPartialUpdateDto(accessKeyDto);
        MCRAccessCacheHelper.clearAllPermissionCaches(accessKey.getReference());
        accessKeyDto.getPermission().getOptional().ifPresent(accessKey::setPermission);
        accessKeyDto.getReference().getOptional().ifPresent(accessKey::setReference);
        validateManagePermission(accessKey);
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
    public void deleteAccessKeyById(UUID id) throws MCRAccessException {
        final MCRAccessKey accessKey = accessKeyRepository.findByUuid(id)
            .orElseThrow(() -> new MCRAccessKeyNotFoundException("access key with given reference does not exist"));
        validateManagePermission(accessKey);
        accessKeyRepository.delete(accessKey);
        MCRAccessCacheHelper.clearAllPermissionCaches(accessKey.getReference());
    }

    @Override
    public boolean deleteAccessKeysByReference(String reference) {
        final List<MCRAccessKey> accessKeys = accessKeyRepository.findByReference(reference).stream()
            .filter(a -> checkManagePermission(a)).toList();
        deleteAccessKeys(accessKeys);
        return accessKeys.size() > 0;
    }

    @Override
    public boolean deleteAccessKeysByReferenceAndPermission(String reference, String permission) {
        final List<MCRAccessKey> accessKeys = accessKeyRepository.findByReferenceAndPermission(reference, permission)
            .stream().filter(a -> checkManagePermission(a)).toList();
        deleteAccessKeys(accessKeys);
        return accessKeys.size() > 0;
    }

    @Override
    public void deleteAllAccessKeys() {
        final List<MCRAccessKey> accessKeys
            = accessKeyRepository.findAll().stream().filter(a -> checkManagePermission(a)).toList();
        deleteAccessKeys(accessKeys);
    }

    private void deleteAccessKeys(List<MCRAccessKey> accessKeys) {
        accessKeys.forEach(accessKeyRepository::delete);
        accessKeys.forEach(a -> MCRAccessCacheHelper.clearAllPermissionCaches(a.getReference()));
    }

    @Override
    public boolean checkAccess(String reference, String rawValue, String permission) {
        final String sanitizedPermission = sanitizePermission(permission);
        if (!(Objects.equals(MCRAccessManager.PERMISSION_READ, sanitizedPermission)
            || Objects.equals(MCRAccessManager.PERMISSION_WRITE, sanitizedPermission))) {
            LOGGER.warn("Permission {} is not supported by access keys", permission);
            return false;
        }
        final Optional<MCRAccessKey> accessKeyOpt
            = accessKeyRepository.findByReferenceAndValue(reference, valueProcessor.getValue(reference, rawValue));
        if (accessKeyOpt.isEmpty()) {
            return false;
        }
        final MCRAccessKey accessKey = accessKeyOpt.get();
        if (Objects.equals(Boolean.FALSE, accessKey.getIsActive())) {
            return false;
        }
        if (accessKey.getExpiration() != null && accessKey.getExpiration().before(new Date())) {
            return false;
        }
        return ((Objects.equals(MCRAccessManager.PERMISSION_READ, sanitizedPermission)
            && Objects.equals(sanitizedPermission, accessKey.getPermission()))
            || Objects.equals(MCRAccessManager.PERMISSION_WRITE, accessKey.getPermission()));
    }

    private static String sanitizePermission(String permission) {
        if (Objects.equals(MCRAccessManager.PERMISSION_VIEW, permission)
            || Objects.equals(MCRAccessManager.PERMISSION_PREVIEW, permission)) {
            return MCRAccessManager.PERMISSION_READ;
        }
        return permission;
    }

    public void setValueProcessor(MCRAccessKeyValueProcessor valueProcessor) {
        this.valueProcessor = valueProcessor;
    }

    @Override
    public String getValue(String reference, String rawValue) {
        return valueProcessor.getValue(reference, rawValue);
    }

    @Override
    public MCRAccessKeyManagementPermissionsDto getManagementPermissionsByReference(String reference) {
        return new MCRAccessKeyManagementPermissionsDto(
            accessService.checkManagePermission(reference, MCRAccessManager.PERMISSION_READ),
            accessService.checkManagePermission(reference, MCRAccessManager.PERMISSION_WRITE));
    }

    private void validateManagePermission(MCRAccessKeyDto accessKeyDto) throws MCRAccessException {
        accessService.validateManagePermission(accessKeyDto.getReference(), accessKeyDto.getPermission());
    }

    private void validateManagePermission(MCRAccessKey accessKey) throws MCRAccessException {
        accessService.validateManagePermission(accessKey.getReference(), accessKey.getPermission());
    }

    private boolean checkManagePermission(MCRAccessKey accessKey) {
        return accessService.checkManagePermission(accessKey.getReference(), accessKey.getPermission());
    }

}
