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

package org.mycore.mcr.acl.accesskey.cli;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.mcr.acl.accesskey.mapper.MCRAccessKeyJsonMapper;
import org.mycore.mcr.acl.accesskey.service.MCRAccessKeyServiceFactory;

/**
 * Command class for managing access keys in the system.
 */
@MCRCommandGroup(name = "Access keys")
public class MCRAccessKeyCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Removes all access keys.
     *
     * @see org.mycore.mcr.acl.accesskey.service.MCRAccessKeyServiceImpl#removeAllAccessKeys()
     */
    @MCRCommand(syntax = "clear all access keys", help = "Clears all access keys")
    public static void removeAllAccessKeys() {
        MCRAccessKeyServiceFactory.getAccessKeyService().removeAllAccessKeys();
        LOGGER.info("Cleared all access keys.");
    }

    /**
     * Removes access keys associated with a specified reference.
     *
     * @param reference the reference for which to delete access keys
     */
    @MCRCommand(syntax = "clear all access keys for {0}", help = "Clears all access keys for reference {0}")
    public static void removeAccessKeysByReference(String reference) {
        MCRAccessKeyServiceFactory.getAccessKeyService().removeAccessKeysByReference(reference);
        LOGGER.info("Cleared all access keys of {}.", reference);
    }

    /**
     * Creates an access key for a specified reference from a file.
     *
     * @param reference the reference for which the access key is created
     * @param path the file path from which the access key data is read
     * @throws IOException if an error occurs while reading the file
     * @throws MCRAccessKeyException if an error occurs while creating access key
     */
    @MCRCommand(syntax = "create access key for {0} from file {1}",
        help = "Creates an access key {0} for reference from file {1} in JSON format")
    public static void createAccessKey(String reference, String path) throws IOException {
        final MCRAccessKeyDto accessKeyDto = readAccessKeyFromFile(path);
        accessKeyDto.setReference(reference);
        MCRAccessKeyServiceFactory.getAccessKeyService().addAccessKey(accessKeyDto);
        LOGGER.info("Created access key for {}.", reference);
    }

    /**
     * Updates an existing access key for a specified reference with data from a file.
     *
     * @param reference the reference for which to update the access key
     * @param secret the secret of the access key to update
     * @param path the file path from which the new access key data is read
     * @throws IOException if an error occurs while reading the file
     * @throws MCRAccessKeyException if an error occurs while updating access key
     */
    @MCRCommand(syntax = "update access key for {0} with secret {1} from file {2}",
        help = "Updates an access key for reference {0} with (hashed) secret {1} from file {2} in JSON format")
    public static void updateAccessKey(String reference, String secret, String path) throws IOException {
        final MCRAccessKeyDto accessKeyDto = readAccessKeyFromFile(path);
        final MCRAccessKeyDto outdatedAccessKeyDto
            = MCRAccessKeyServiceFactory.getAccessKeyService().findAccessKeyByReferenceAndSecret(reference, secret);
        if (outdatedAccessKeyDto == null) {
            throw new MCRAccessKeyNotFoundException("Access key does not exist");
        }
        MCRAccessKeyServiceFactory.getAccessKeyService().updateAccessKey(outdatedAccessKeyDto.getId(), accessKeyDto);
        LOGGER.info("Updated access key ({}) for {}.", secret, reference);
    }

    /**
     * Removes a specific access key identified by its reference and secret.
     *
     * @param reference the reference associated with the access key
     * @param secret the secret of the access key to delete
     * @throws MCRAccessKeyNotFoundException if the specified access key is not found
     */
    @MCRCommand(syntax = "delete access key for {0} with secret {1}",
        help = "Deletes an access key for reference {0} with (processed) secret {1}")
    public static void removeAccessKey(String reference, String secret) {
        final MCRAccessKeyDto outdatedAccessKeyDto
            = MCRAccessKeyServiceFactory.getAccessKeyService().findAccessKeyByReferenceAndSecret(reference, secret);
        if (outdatedAccessKeyDto == null) {
            throw new MCRAccessKeyNotFoundException("Access key does not exist");
        }
        MCRAccessKeyServiceFactory.getAccessKeyService().removeAccessKey(outdatedAccessKeyDto.getId());
        LOGGER.info("Deleted access key ({}) for {}.", secret, reference);
    }

    /**
     * Imports access keys for a specified reference from a file.
     *
     * @param reference the reference for which to import access keys
     * @param path the file path from which the access keys are read
     * @throws IOException if an error occurs while reading the file
     * @throws MCRAccessKeyException if an error occurs while importing access key
     */
    @MCRCommand(syntax = "import access keys for {0} from file {1}",
        help = "Imports access keys for reference {0} from file {1} in JSON array format")
    public static void importAccessKeysFromFile(String reference, String path) throws IOException {
        final String json = Files.readString(Path.of(path), UTF_8);
        final List<MCRAccessKeyDto> accessKeyDtos
            = MCRAccessKeyJsonMapper.jsonToAccessKeyDtos(json).stream().peek(a -> a.setReference(reference)).toList();
        for (MCRAccessKeyDto accessKeyDto : accessKeyDtos) {
            MCRAccessKeyServiceFactory.getAccessKeyService().importAccessKey(accessKeyDto);
        }
        LOGGER.info("Imported access keys for {} from file {}.", reference, path);
    }

    /**
     * Exports access keys for a specified reference to a JSON file.
     *
     * @param reference the reference for which to export access keys
     * @param path the file path where the access keys will be written
     * @throws IOException if an error occurs while writing to the file
     * @throws MCRAccessKeyException if an error occurs while exporting access keys
     */
    @MCRCommand(syntax = "export access keys for {0} to file {1}",
        help = "Exports access keys for reference {0} to file {1} in JSON array format")
    public static void exportAccessKeysToFile(String reference, String path) throws IOException {
        final List<MCRAccessKeyDto> accessKeyDtos
            = MCRAccessKeyServiceFactory.getAccessKeyService().findAccessKeysByReference(reference);
        final String json = MCRAccessKeyJsonMapper.accessKeyDtosToJson(accessKeyDtos);
        Files.writeString(Path.of(path), json, UTF_8);
        LOGGER.info("Exported access keys for {} to file {}.", reference, path);
    }

    /**
     * Cleans up user attributes related to access keys.
     */
    @MCRCommand(syntax = "clean up access key user attributes",
        help = "Cleans all access key secret attributes of users if the corresponding key does not exist.")
    public static void cleanUp() {
        MCRAccessKeyServiceFactory.getAccessKeyUserService().cleanUpUserAttributes();
        LOGGER.info("Cleaned up access keys.");
    }

    /**
     * Returns processed access key secret for reference to command output.
     *
     * @param secret the secret
     * @param reference the reference
     */
    @MCRCommand(syntax = "hash access key secret {0} for {1}", help = "Hashes secret {0} for reference {1}")
    public static void processSecret(String secret, String reference) {
        final String result = MCRAccessKeyServiceFactory.getAccessKeyService().processSecret(reference, secret);
        LOGGER.info("Processed secret for {}: '{}'.", reference, result);
    }

    private static MCRAccessKeyDto readAccessKeyFromFile(String path) throws IOException {
        final String json = Files.readString(Path.of(path), UTF_8);
        return MCRAccessKeyJsonMapper.jsonToAccessKeyDto(json);
    }
}
