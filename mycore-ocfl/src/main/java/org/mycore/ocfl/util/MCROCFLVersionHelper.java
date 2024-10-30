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

package org.mycore.ocfl.util;

import java.util.Map;

import org.mycore.common.MCRPersistenceException;

/**
 * Helper class for handling version-related operations within the OCFL repository.
 * <p>
 * This class provides utilities for mapping versioning messages (e.g., "Created", "Updated", "Deleted")
 * to their corresponding version types in the OCFL metadata.
 */
public class MCROCFLVersionHelper {

    /**
     * Message indicating that an object was created.
     */
    public static final String MESSAGE_CREATED = "Created";

    /**
     * Message indicating that an object was updated.
     */
    public static final String MESSAGE_UPDATED = "Updated";

    /**
     * Message indicating that an object was deleted.
     */
    public static final String MESSAGE_DELETED = "Deleted";

    /**
     * Mapping between versioning messages and their corresponding version types.
     * <p>
     * The mapping is as follows:
     * <ul>
     *   <li>"Created" -> {@link MCROCFLMetadataVersion#CREATED}</li>
     *   <li>"Updated" -> {@link MCROCFLMetadataVersion#UPDATED}</li>
     *   <li>"Deleted" -> {@link MCROCFLMetadataVersion#DELETED}</li>
     * </ul>
     */
    public static final Map<String, Character> MESSAGE_TYPE_MAPPING = Map.ofEntries(
        Map.entry(MESSAGE_CREATED, MCROCFLMetadataVersion.CREATED),
        Map.entry(MESSAGE_UPDATED, MCROCFLMetadataVersion.UPDATED),
        Map.entry(MESSAGE_DELETED, MCROCFLMetadataVersion.DELETED));

    /**
     * Converts a versioning message (e.g., "Created", "Updated", "Deleted") to its corresponding version type.
     *
     * @param message the versioning message to convert.
     * @return the corresponding version type as a character.
     * @throws MCRPersistenceException if the message cannot be mapped to a valid version type.
     */
    public static char convertMessageToType(String message) throws MCRPersistenceException {
        if (!MESSAGE_TYPE_MAPPING.containsKey(message)) {
            throw new MCRPersistenceException("Cannot identify version type from message '" + message + "'");
        }
        return MESSAGE_TYPE_MAPPING.get(message);
    }

}
