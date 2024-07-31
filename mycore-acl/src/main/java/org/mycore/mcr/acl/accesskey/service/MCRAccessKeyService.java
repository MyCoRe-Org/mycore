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

import java.util.List;
import java.util.UUID;

import org.mycore.access.MCRAccessException;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyCollisionException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyValidationException;

/**
 * Service interface for managing AccessKeys.
 */
public interface MCRAccessKeyService {

    /**
     * Returns all access keys.
     *
     * @return A list of all access keys.
     */
    List<MCRAccessKeyDto> getAllAccessKeys();

    /**
     * Retrieves a list of access keys associated with the specified reference.
     *
     * @param reference the reference for which access keys are to be retrieved
     * @return a list of access key DTO associated with the specified reference
     */
    List<MCRAccessKeyDto> getAccessKeysByReference(String reference);

    /**
     * Retrieves a list of access keys of a specific permission associated with the specified reference.
     *
     * @param reference the reference for which access keys are to be retrieved
     * @param permission the permission of access keys to be retrieved
     * @return a list of access key DTO of the specified permission associated with the specified reference
     */
    List<MCRAccessKeyDto> getAccessKeysByReferenceAndPermission(String reference, String permission);

    /**
     * Retrieves an access key by its id.
     *
     * @param id reference the id of the access key
     * @return the access key DTO corresponding to the specified reference and value
     * @throws MCRAccessKeyNotFoundException if the access key does not exist
     */
    MCRAccessKeyDto getAccessKeyById(UUID id);

    /**
     * Retrieves an access key by its reference associated with the value.
     *
     * @param reference the reference for which access keys are to be retrieved
     * @param value the value of the access key to be retrieved
     * @return the access key DTO corresponding to the specified reference and value
     * @throws MCRAccessKeyNotFoundException if the access key does not exist
     */
    MCRAccessKeyDto getAccessKeyByReferenceAndValue(String reference, String value) throws MCRAccessException;

    /**
     * Creates a new access key.
     *
     * @param accessKeyDto the DTO containing the details of the access key to be created
     * @return the created access key DTO
     * @throws MCRAccessKeyValidationException if the DTO is invalid
     * @throws MCRAccessKeyCollisionException if there is already and access key with the value
     */
    MCRAccessKeyDto createAccessKey(MCRAccessKeyDto accessKeyDto) throws MCRAccessException;

    /**
     * Import existing access key.
     *
     * @param accessKeyDto the access keys to be imported
     * @return the imported access key DTO
     * @throws MCRAccessKeyValidationException if the DTO is invalid
     * @throws MCRAccessKeyCollisionException if there is already and access key with the value
     */
    MCRAccessKeyDto importAccessKey(MCRAccessKeyDto accessKeyDto) throws MCRAccessException;

    /**
     * Updates an access key based on the provided reference and value.
     *
     * @param id reference the id of the access key to be updated
     * @param accessKeyDto the DTO containing the updated details of the access key
     * @return the updated access key DTO
     * @throws MCRAccessKeyValidationException if the DTO is invalid
     * @throws MCRAccessKeyNotFoundException if the access key does not exist
     * @throws MCRAccessKeyCollisionException if the access key already exists
     */
    MCRAccessKeyDto updateAccessKeyById(UUID id, MCRAccessKeyDto accessKeyDto) throws MCRAccessException;

    /**
     * Partially updates an existing access key by id.
     *
     * @param id reference the id of the access key to be updated
     * @param accessKeyDto the DTO representing the access key to be updated
     * @return the updated access key
     * @throws MCRAccessKeyValidationException if the DTO is invalid
     * @throws MCRAccessKeyNotFoundException if the access key does not exist
     * @throws MCRAccessKeyCollisionException if the access key already exists
     */
    MCRAccessKeyDto partialUpdateAccessKeyById(UUID id, MCRAccessKeyPartialUpdateDto accessKeyDto)
        throws MCRAccessException;

    /**
     * Deletes an access key by id.
     *
     * @param id reference the id of the access key to be deleted
     * @throws MCRAccessKeyNotFoundException if the access key does not exist
     */
    void deleteAccessKeyById(UUID id) throws MCRAccessException;

    /**
     * Deletes all access keys that match the specified reference.
     *
     * @param reference the reference for which all matching access keys should be deleted
     * @return true if at least one access key was deleted
     */
    boolean deleteAccessKeysByReference(String reference);

    /**
     * Deletes all access keys that match the specified reference and permission type.
     *
     * @param reference the reference for which all matching access keys should be deleted
     * @param permission the permission type to filter by
     * @return true if at least one access key was successfully deleted
     */
    boolean deleteAccessKeysByReferenceAndPermission(String reference, String permission);

    /**
     * Deletes all AccessKeys.
     */
    void deleteAllAccessKeys();

    /**
     * Checks if an access key with the specified reference and encoded value exists.
     *
     * @param reference the reference to search for
     * @param value the encoded value to search for
     * @return true if an access key with the specified reference and value exists
     */
    boolean existsAccessKeyWithReferenceAndEncodedValue(String reference, String encodedValue);

}
