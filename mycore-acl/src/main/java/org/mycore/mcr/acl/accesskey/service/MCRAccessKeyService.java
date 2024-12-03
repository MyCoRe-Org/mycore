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

import java.util.List;
import java.util.UUID;

import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyPartialUpdateDto;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyCollisionException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyValidationException;

/**
 * Service interface for managing access keys.
 */
public interface MCRAccessKeyService {

    /**
     * Lists all available access keys.
     *
     * @return a list of all available access keys
     */
    List<MCRAccessKeyDto> listAllAccessKeys();

    /**
     * Finds all access keys for a given reference.
     *
     * @param reference the reference to search by
     * @return a list of access keys as DTOs
     */
    List<MCRAccessKeyDto> findAccessKeysByReference(String reference);

    /**
     * Finds all access keys that grant a specific permission.
     *
     * @param permission the permission to search by
     * @return a list of access keys as DTOs
     */
    List<MCRAccessKeyDto> findAccessKeysByPermission(String permission);

    /**
     * Finds access keys by reference and permission.
     *
     * @param reference the reference
     * @param permission the permission
     * @return a list of matching access keys as DTOs
     */
    List<MCRAccessKeyDto> findAccessKeysByReferenceAndPermission(String reference, String permission);

    /**
     * Finds an access key by its ID.
     *
     * @param id the ID of the access key
     * @return the corresponding access key as a DTO
     * @throws MCRAccessKeyNotFoundException if no access key is found with the given ID
     */
    MCRAccessKeyDto findAccessKey(UUID id);

    /**
     * Finds an access key by its reference and secret.
     *
     * @param reference the reference of the access key
     * @param secret the secret of the access key
     * @return the corresponding access key as a DTO or null if not found
     */
    MCRAccessKeyDto findAccessKeyByReferenceAndSecret(String reference, String secret);

    /**
     * Adds a new access key.
     *
     * @param accessKeyDto the access key data to add
     * @return the added access key as a DTO
     * @throws MCRAccessKeyValidationException if the access key data is invalid
     * @throws MCRAccessKeyCollisionException if a key with the same reference and secret already exists
     */
    MCRAccessKeyDto addAccessKey(MCRAccessKeyDto accessKeyDto);

    /**
     * Imports an access key.
     *
     * @param accessKeyDto the access key data to import
     * @return the imported access key as a DTO
     * @throws MCRAccessKeyValidationException if the access key data is invalid
     * @throws MCRAccessKeyCollisionException if a key with the same reference and secret already exists
     */
    MCRAccessKeyDto importAccessKey(MCRAccessKeyDto accessKeyDto);

    /**
     * Updates an existing access key by its ID.
     *
     * @param id the ID of the access key to update
     * @param accessKeyDto the new access key data
     * @return the updated access key as a DTO
     * @throws MCRAccessKeyValidationException if the updated data is invalid
     * @throws MCRAccessKeyNotFoundException if no access key is found with the given ID
     * @throws MCRAccessKeyCollisionException if the updated secret conflicts with another access key
     */
    MCRAccessKeyDto updateAccessKey(UUID id, MCRAccessKeyDto accessKeyDto);

    /**
     * Partially updates an existing access key by its ID.
     *
     * @param id the ID of the access key to update
     * @param accessKeyDto the new access key data for partial update
     * @return the updated access key as a DTO
     * @throws MCRAccessKeyValidationException if the partial update data is invalid
     * @throws MCRAccessKeyNotFoundException if no access key is found with the given ID
     * @throws MCRAccessKeyCollisionException if the updated secret conflicts with another access key
     */
    MCRAccessKeyDto partialUpdateAccessKey(UUID id, MCRAccessKeyPartialUpdateDto accessKeyDto);

    /**
     * Removes an access key by its ID.
     *
     * @param id the ID of the access key to delete
     * @throws MCRAccessKeyNotFoundException if no access key is found with the given ID
     */
    void removeAccessKey(UUID id);

    /**
     * Removes all access keys associated with a given reference.
     *
     * @param reference the reference whose access keys will be deleted
     * @return true if any access keys were deleted, false otherwise
     */
    boolean removeAccessKeysByReference(String reference);

    /**
     * Removes all access keys associated with a given reference and permission.
     *
     * @param reference the reference
     * @param permission the permission
     * @return true if any access keys were deleted, false otherwise
     */
    boolean removeAccessKeysByReferenceAndPermission(String reference, String permission);

    /**
     * Removes all access keys.
     */
    void removeAllAccessKeys();

    /**
     * Processes the raw secret for a specific reference to create a valid access key secret.
     *
     * @param reference the reference of the access key
     * @param secret the raw secret to process
     * @return the processed secret
     */
    String processSecret(String reference, String secret);

}
