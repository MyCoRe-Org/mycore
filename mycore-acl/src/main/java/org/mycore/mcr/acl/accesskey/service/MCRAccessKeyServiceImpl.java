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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionHelper;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.crypt.MCRCipher;
import org.mycore.crypt.MCRCipherManager;
import org.mycore.crypt.MCRCryptKeyFileNotFoundException;
import org.mycore.crypt.MCRCryptKeyNoPermissionException;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyConstants;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyCollisionException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.mcr.acl.accesskey.mapper.MCRAccessKeyMapper;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;
import org.mycore.mcr.acl.accesskey.persistence.MCRAccessKeyRepository;
import org.mycore.mcr.acl.accesskey.persistence.MCRAccessKeyRepositoryImpl;
import org.mycore.mcr.acl.accesskey.validation.MCRAccessKeyValidatorImpl;
import org.mycore.util.concurrent.MCRTransactionableCallable;

/**
 * Implementation of the {@link MCRAccessKeyService} interface for managing access keys.
 */
public class MCRAccessKeyServiceImpl implements MCRAccessKeyService {

    private static final String SECRET_STORAGE_MODE_PROP_PREFX = "MCR.ACL.AccessKey.Secret.Storage.Mode";

    private static final String SECRET_STORAGE_MODE
        = MCRConfiguration2.getStringOrThrow(SECRET_STORAGE_MODE_PROP_PREFX);

    private static final int HASHING_ITERATIONS
        = MCRConfiguration2.getInt(SECRET_STORAGE_MODE_PROP_PREFX + ".Hash.Iterations").orElse(1000);

    private final MCRAccessKeyRepository accessKeyRepository;

    public static String getEncodedValue(String reference, String value) {
        switch (SECRET_STORAGE_MODE) {
            case "plain" -> {
                return value;
            }
            case "crypt" -> {
                try {
                    final MCRCipher cipher = MCRCipherManager.getCipher("accesskey");
                    return cipher.encrypt(reference + value);
                } catch (MCRCryptKeyFileNotFoundException | MCRCryptKeyNoPermissionException e) {
                    throw new MCRException(e);
                }
            }
            case "hash" -> {
                try {
                    return MCRUtils.asSHA256String(HASHING_ITERATIONS, reference.getBytes(UTF_8), value);
                } catch (NoSuchAlgorithmException e) {
                    throw new MCRException("Cannot hash secret.", e);
                }
            }
            default -> throw new MCRException("Please configure a valid storage mode for secret.");
        }
    }

    /**
     * Constructs a new {@link MCRAccessKeyObjectIdService} instance.
     */
    protected MCRAccessKeyServiceImpl(MCRAccessKeyRepository accessKeyRepository) {
        this.accessKeyRepository = accessKeyRepository;
    }

    /**
     * Returns single instance.
     *
     * @return the single instance
     */
    public static MCRAccessKeyServiceImpl getInstance() {
        return InstanceHolder.INSTANCE;
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

    // TODO check permission
    @Override
    public List<MCRAccessKeyDto> getAllAccessKeys() {
        return accessKeyRepository.findAll().stream().map(a -> fixAccessKeyUuidIfRequired(a))
            .map(MCRAccessKeyMapper::toDto).toList();

    }

    // TODO check permission
    @Override
    public List<MCRAccessKeyDto> getAccessKeysByReference(String reference) {
        return accessKeyRepository.findByReference(reference).stream().map(a -> fixAccessKeyUuidIfRequired(a))
            .map(MCRAccessKeyMapper::toDto).toList();
    }

    // TODO check permission
    @Override
    public List<MCRAccessKeyDto> getAccessKeysByReferenceAndPermission(String reference, String permission) {
        return accessKeyRepository.findByReferenceAndPermission(reference, permission).stream()
            .map(a -> fixAccessKeyUuidIfRequired(a)).map(MCRAccessKeyMapper::toDto).toList();
    }

    // TODO check permission
    @Override
    public MCRAccessKeyDto getAccessKeyById(UUID id) {
        return accessKeyRepository.findByUuid(id).map(a -> fixAccessKeyUuidIfRequired(a)).map(MCRAccessKeyMapper::toDto)
            .orElseThrow(() -> new MCRAccessKeyNotFoundException("Access key with given reference does not exist"));
    }

    // TODO check permission
    @Override
    public MCRAccessKeyDto getAccessKeyByReferenceAndValue(String reference, String value) {
        return accessKeyRepository.findByReferenceAndValue(reference, getEncodedValue(reference, value))
            .map(a -> fixAccessKeyUuidIfRequired(a)).map(MCRAccessKeyMapper::toDto)
            .orElseThrow(() -> new MCRAccessKeyNotFoundException("Access key with given reference does not exist"));
    }

    @Override
    public MCRAccessKeyDto createAccessKey(MCRAccessKeyDto accessKeyDto) throws MCRAccessException {
        MCRAccessKeyValidatorImpl.getInstance().validateAccessKeyDto(accessKeyDto);
        validateManagePermission(accessKeyDto);
        final String encodedValue = getEncodedValue(accessKeyDto.getReference(), accessKeyDto.getValue());
        if (accessKeyRepository.existsByReferenceAndValue(accessKeyDto.getReference(), encodedValue)) {
            throw new MCRAccessKeyCollisionException("Given access key value collides with another access key");
        }
        final MCRAccessKey accessKey = MCRAccessKeyMapper.toEntity(accessKeyDto);
        accessKey.setSecret(encodedValue);
        if (accessKeyDto.getActive() == null) {
            accessKey.setIsActive(true);
        }
        accessKey.setCreated(new Date());
        accessKey.setCreatedBy(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID());
        final MCRAccessKey savedAccessKey = accessKeyRepository.save(accessKey);
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
        return MCRAccessKeyMapper.toDto(savedAccessKey);
    }

    @Override
    public MCRAccessKeyDto updateAccessKeyById(UUID id, MCRAccessKeyDto accessKeyDto) throws MCRAccessException {
        final MCRAccessKey accessKey = accessKeyRepository.findByUuid(id)
            .orElseThrow(() -> new MCRAccessKeyNotFoundException("access key with given reference does not exist"));
        validateManagePermission(accessKey);
        MCRAccessKeyValidatorImpl.getInstance().validateAccessKeyDto(accessKeyDto);
        if (!Objects.equals(accessKey.getPermission(), accessKeyDto.getPermission())) {
            validateManagePermission(accessKeyDto);
            accessKey.setPermission(accessKeyDto.getPermission());
        }
        final String encodedValue = getEncodedValue(accessKeyDto.getReference(), accessKeyDto.getValue());
        if (accessKeyRepository.existsByReferenceAndValue(accessKeyDto.getReference(), encodedValue)) {
            throw new MCRAccessKeyCollisionException("Given access key value collides with another access key");
        }
        accessKey.setSecret(encodedValue);
        accessKey.setReference(accessKeyDto.getReference());
        accessKey.setComment(accessKeyDto.getComment());
        accessKey.setLastModified(new Date());
        accessKey.setLastModifiedBy(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID());
        accessKey.setExpiration(accessKeyDto.getExpiration());
        accessKey.setIsActive(accessKeyDto.getActive());
        final MCRAccessKey savedAccessKey = accessKeyRepository.save(accessKey);
        return MCRAccessKeyMapper.toDto(savedAccessKey);
    }

    @Override
    public MCRAccessKeyDto partialUpdateAccessKeyById(UUID id, MCRAccessKeyPartialUpdateDto accessKeyDto)
        throws MCRAccessException {
        final MCRAccessKey accessKey = accessKeyRepository.findByUuid(id)
            .orElseThrow(() -> new MCRAccessKeyNotFoundException("access key with given reference does not exist"));
        validateManagePermission(accessKey);
        MCRAccessKeyValidatorImpl.getInstance().validateAccessKeyPartialUpdateDto(accessKeyDto);
        if (accessKeyDto.getReference().isPresent() || accessKeyDto.getPermission().isPresent()) {
            final String reference
                = accessKeyDto.getReference().isPresent() ? accessKeyDto.getReference().get()
                    : accessKey.getReference();
            final String permission
                = accessKeyDto.getPermission().isPresent() ? accessKeyDto.getPermission().get()
                    : accessKey.getPermission();
            validateManagePermission(reference, permission);
        }
        accessKeyDto.getReference().getOptional().ifPresent(accessKey::setReference);
        if (accessKeyDto.getValue().isPresent()) {
            final String encodedValue = getEncodedValue(accessKey.getReference(), accessKeyDto.getValue().get());
            if (accessKeyRepository.existsByReferenceAndValue(accessKey.getReference(), encodedValue)) {
                throw new MCRAccessKeyCollisionException("Given access key value collides with another access key");
            }
            accessKey.setSecret(encodedValue);
        }
        accessKeyDto.getPermission().getOptional().ifPresent(accessKey::setPermission);
        accessKeyDto.getActive().getOptional().ifPresent(accessKey::setIsActive);
        accessKeyDto.getComment().getOptional().ifPresent(accessKey::setComment);
        accessKeyDto.getExpiration().getOptional().ifPresent(accessKey::setExpiration);
        accessKeyDto.getValue().getOptional().map(v -> (getEncodedValue(accessKey.getReference(), v)))
            .ifPresent(accessKey::setSecret);
        accessKey.setLastModified(new Date());
        accessKey.setLastModifiedBy(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID());
        final MCRAccessKey savedAccessKey = accessKeyRepository.save(accessKey);
        return MCRAccessKeyMapper.toDto(savedAccessKey);
    }

    @Override
    public void deleteAccessKeyById(UUID id) throws MCRAccessException {
        final MCRAccessKey accessKey = accessKeyRepository.findByUuid(id)
            .orElseThrow(() -> new MCRAccessKeyNotFoundException("access key with given reference does not exist"));
        validateManagePermission(accessKey);
        accessKeyRepository.delete(accessKey);
    }

    // TODO check permission
    @Override
    public boolean deleteAccessKeysByReference(String reference) {
        return accessKeyRepository.deleteByReference(reference) > 0;
    }

    // TODO check permission
    @Override
    public boolean deleteAccessKeysByReferenceAndPermission(String reference, String permission) {
        return accessKeyRepository.deleteByReferenceAndPermission(reference, permission) > 0;
    }

    // TODO check permission
    @Override
    public void deleteAllAccessKeys() {
        accessKeyRepository.deleteAll();
    }

    @Override
    public boolean existsAccessKeyWithReferenceAndEncodedValue(String reference, String value) {
        return accessKeyRepository.existsByReferenceAndValue(reference, getEncodedValue(reference, value));
    }

    private void validateManagePermission(MCRAccessKey accessKey) throws MCRAccessException {
        validateManagePermission(accessKey.getReference(), accessKey.getPermission());
    }

    private void validateManagePermission(MCRAccessKeyDto accessKeyDto) throws MCRAccessException {
        validateManagePermission(accessKeyDto.getReference(), accessKeyDto.getPermission());
    }

    private void validateManagePermission(String accessKeyReference, String accessKeyPermission)
        throws MCRAccessException {
        if (Objects.equals(MCRAccessManager.PERMISSION_READ, accessKeyPermission)) {
            if (!MCRAccessManager.checkPermission(accessKeyReference,
                MCRAccessKeyConstants.PERMISSION_MANAGE_READ_ACCESS_KEYS)) {
                throw MCRAccessException
                    .missingPermission("Create a " + accessKeyPermission + " access key",
                        accessKeyReference, MCRAccessKeyConstants.PERMISSION_MANAGE_READ_ACCESS_KEYS);
            }
        }
        if (!MCRAccessManager.checkPermission(accessKeyReference,
            MCRAccessKeyConstants.PERMISSION_MANAGE_WRITE_ACCESS_KEYS)) {
            throw MCRAccessException
                .missingPermission("Create a " + accessKeyPermission + " access key",
                    accessKeyReference, MCRAccessKeyConstants.PERMISSION_MANAGE_WRITE_ACCESS_KEYS);
        }
    }

    private static class InstanceHolder {
        static final MCRAccessKeyServiceImpl INSTANCE = new MCRAccessKeyServiceImpl(new MCRAccessKeyRepositoryImpl());
    }

}
