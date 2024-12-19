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

package org.mycore.mcr.acl.accesskey.persistence;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;

/**
 * Repository interface for managing {@link MCRAccessKey} entities.
 */
public interface MCRAccessKeyRepository {

    /**
     * Finds and returns all {@link MCRAccessKey} entities.
     *
     * @return a collection of all access keys
     */
    Collection<MCRAccessKey> findAll();

    /**
     * Finds and returns all {@link MCRAccessKey} entities that match the specified reference.
     *
     * @param reference the reference to filter by
     * @return a collection of access keys that match the specified reference
     */
    Collection<MCRAccessKey> findByReference(String reference);

    /**
     * Finds and returns all {@link MCRAccessKey} entities that match the specified type.
     *
     * @param type the type to filter by
     * @return a collection of access keys that match the specified type
     */
    Collection<MCRAccessKey> findByType(String type);

    /**
     * Finds and returns all {@link MCRAccessKey} entities that match the specified reference and type.
     *
     * @param reference the reference to filter by
     * @param type the type to filter by
     * @return a collection of access keys that match the specified reference and type
     */
    Collection<MCRAccessKey> findByReferenceAndType(String reference, String type);

    /**
     * Finds and returns an {@link MCRAccessKey} by id.
     *
     * @param id the id
     * @return an {@link Optional} containing the access key
     */
    Optional<MCRAccessKey> findByUuid(UUID id);

    /**
     * Finds and returns an {@link MCRAccessKey} entity that matches the specified reference and secret.
     *
     * @param reference the reference to filter by
     * @param secret the secret to filter by
     * @return an {@link Optional} containing the access key
     */
    Optional<MCRAccessKey> findByReferenceAndSecret(String reference, String secret);

    /**
     * Saves the given {@link MCRAccessKey} entity.
     * If an AccessKey with the same reference and secret already exists, it will be updated.
     *
     * @param accessKey the access key object to be saved
     * @return the saved access key entity
     */
    MCRAccessKey save(MCRAccessKey accessKey);

    /**
     * Deletes the specified {@link MCRAccessKey} entity.
     *
     * @param accessKey The access key object to be deleted
     */
    void delete(MCRAccessKey accessKey);

    /**
     * Checks if an {@link MCRAccessKey} with the specified reference and secret exists.
     *
     * @param reference the reference to check
     * @param secret the secret to check
     * @return true if an access key with the specified reference and secret exists
     */
    boolean existsByReferenceAndSecret(String reference, String secret);

    /**
     * Detaches the specified {@link MCRAccessKey} entity from the persistence context, if it is managed.
     *
     * @param accessKey The access key object to be detached
     */
    void detach(MCRAccessKey accessKey);

    /**
     * Flushes all changes made to the persistence context to the underlying database.
     */
    void flush();
}
