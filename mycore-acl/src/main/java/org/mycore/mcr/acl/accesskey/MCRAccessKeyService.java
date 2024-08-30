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
     * Retrieves a list of access keys associated with the specified permission.
     *
     * @param permission the permission for which access keys are to be retrieved
     * @return a list of access key DTO associated with the specified permission
     */
    List<MCRAccessKeyDto> getAccessKeysByPermission(String permission);

    /**
     * Retrieves a list of access keys of a specific permission associated with the specified reference.
     *
     * @param reference the reference for which access keys are to be retrieved
     * @param permission the permission of access keys to be retrieved
     * @return a list of access key DTO of the specified permission associated with the specified reference
     */
    List<MCRAccessKeyDto> getAccessKeysByReferenceAndPermission(String reference, String permission);

    /**
     * Retrieves an access key by id.
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
     * @return the access key DTO corresponding to the specified reference and value or null
     */
    MCRAccessKeyDto getAccessKeyByReferenceAndValue(String reference, String value);

    /**
     * Creates a new access key.
     *
     * @param accessKeyDto the DTO containing the details of the access key to be created
     * @return the created access key DTO
     * @throws MCRAccessKeyValidationException if the DTO is invalid
     * @throws MCRAccessKeyCollisionException if there is already and access key with the value
     */
    MCRAccessKeyDto createAccessKey(MCRAccessKeyDto accessKeyDto);

    /**
     * Imports existing access key.
     *
     * @param accessKeyDto the access keys to be imported
     * @return the imported access key DTO
     * @throws MCRAccessKeyValidationException if the DTO is invalid
     * @throws MCRAccessKeyCollisionException if there is already and access key with the value
     */
    MCRAccessKeyDto importAccessKey(MCRAccessKeyDto accessKeyDto);

    /**
     * Updates an access key by id.
     *
     * @param id reference the id of the access key to be updated
     * @param accessKeyDto the DTO containing the updated details of the access key
     * @return the updated access key DTO
     * @throws MCRAccessKeyValidationException if the DTO is invalid
     * @throws MCRAccessKeyNotFoundException if the access key does not exist
     * @throws MCRAccessKeyCollisionException if the access key already exists
     */
    MCRAccessKeyDto updateAccessKeyById(UUID id, MCRAccessKeyDto accessKeyDto);

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
    MCRAccessKeyDto partialUpdateAccessKeyById(UUID id, MCRAccessKeyPartialUpdateDto accessKeyDto);

    /**
     * Deletes an access key by id.
     *
     * @param id the id of the access key to be deleted
     * @throws MCRAccessKeyNotFoundException if the access key does not exist
     */
    void deleteAccessKeyById(UUID id);

    /**
     * Deletes all access keys that match the specified reference and current user is allowed to.
     *
     * @param reference the reference for which all matching access keys should be deleted
     * @return true if at least one access key was deleted
     */
    boolean deleteAccessKeysByReference(String reference);

    /**
     * Deletes all access keys that match the specified reference and permission type and current user is allowed to.
     *
     * @param reference the reference for which all matching access keys should be deleted
     * @param permission the permission type to filter by
     * @return true if at least one access key was successfully deleted
     */
    boolean deleteAccessKeysByReferenceAndPermission(String reference, String permission);

    /**
     * Deletes all access keys current user is allowed to.
     */
    void deleteAllAccessKeys();

    /**
     * Checks if current user has given permission by access key defined by reference and value.
     *
     * @param reference the reference to search for
     * @param rawValue the value to search for
     * @param permission the permission
     * @return true if current user has permission to reference via access key
     */
    boolean checkAccess(String reference, String rawValue, String permission);

    /**
     * Returns the value processed value.
     *
     * @param reference the reference
     * @param rawValue the raw value
     * @return the processed value
     */
    String getValue(String reference, String rawValue);

}
