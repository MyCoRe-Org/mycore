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

package org.mycore.mcr.acl.accesskey.cli;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyServiceFactory;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyException;
import org.mycore.mcr.acl.accesskey.exception.MCRAccessKeyNotFoundException;
import org.mycore.mcr.acl.accesskey.mapper.MCRAccessKeyJsonMapper;

/**
 * Provides command to manage access keys.
 */
@MCRCommandGroup(name = "Access keys")
public class MCRAccessKeyCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Deletes all access keys.
     *
     * @see org.mycore.mcr.acl.accesskey.MCRAccessKeyServiceImpl#deleteAllAccessKeys()
     */
    @MCRCommand(syntax = "clear all access keys", help = "Clears all access keys")
    public static void deleteAllAccessKeys() {
        MCRAccessKeyServiceFactory.getService().deleteAllAccessKeys();
        LOGGER.info("Cleared all access keys.");
    }

    /**
     * Deletes access keys for reference.
     *
     * @param reference the reference
     *
     * @see org.mycore.mcr.acl.accesskey.MCRAccessKeyServiceImpl#deleteAccessKeysByReference(String)
     */
    @MCRCommand(syntax = "clear all access keys for {0}", help = "Clears all access keys for reference {0}")
    public static void deleteAccessKeysByReference(String reference) {
        MCRAccessKeyServiceFactory.getService().deleteAccessKeysByReference(reference);
        LOGGER.info("Cleared all access keys of {}.", reference);
    }

    /**
     * Creates access key for reference from file by path.
     *
     * @param reference the reference
     * @param path the path
     * @throws IOException if an IO error occurs
     * @throws MCRAccessException if current user is not allowed to created access key
     * @throws MCRAccessKeyException if an error occurs while creating access key
     *
     * @see org.mycore.mcr.acl.accesskey.MCRAccessKeyServiceImpl#createAccessKey(MCRAccessKeyDto)
     */
    @MCRCommand(syntax = "create access key for {0} from file {1}",
        help = "Creates an access key {0} for reference from file {1} in JSON format")
    public static void createAccessKey(String reference, String path) throws IOException, MCRAccessException {
        final MCRAccessKeyDto accessKeyDto = readAccessKeyFromFile(path);
        accessKeyDto.setReference(reference);
        MCRAccessKeyServiceFactory.getService().createAccessKey(accessKeyDto);
        LOGGER.info("Created access key for {}.", reference);
    }

    /**
     * Updates access key for reference by value from file by path.
     *
     * @param reference the reference
     * @param value the value
     * @param path the path
     * @throws IOException if an IO error occurs
     * @throws MCRAccessException if current user is not allowed to update access key
     * @throws MCRAccessKeyException if an error occurs while updating access key
     *
     * @see org.mycore.mcr.acl.accesskey.MCRAccessKeyServiceImpl#updateAccessKeyById(java.util.UUID, MCRAccessKeyDto)
     */
    @MCRCommand(syntax = "update access key for {0} with secret {1} from file {2}",
        help = "Updates an access key for reference {0} with (hashed) secret {1} from file {2} in JSON format")
    public static void updateAccessKey(String reference, String value, String path)
        throws IOException, MCRAccessException {
        final MCRAccessKeyDto accessKeyDto = readAccessKeyFromFile(path);
        final MCRAccessKeyDto outdatedAccessKeyDto
            = MCRAccessKeyServiceFactory.getService().getAccessKeyByReferenceAndValue(reference, value);
        if (outdatedAccessKeyDto == null) {
            throw new MCRAccessKeyNotFoundException("Access key does not exist");
        }
        MCRAccessKeyServiceFactory.getService().updateAccessKeyById(outdatedAccessKeyDto.getId(), accessKeyDto);
        LOGGER.info("Updated access key ({}) for {}.", value, reference);
    }

    /**
     * Deletes access key for reference by value.
     *
     * @param reference the reference
     * @param value the value
     * @throws MCRAccessException if current user is not allowed to delete access key
     * @throws MCRAccessKeyNotFoundException if access key not found
     *
     * @see org.mycore.mcr.acl.accesskey.MCRAccessKeyServiceImpl#deleteAccessKeysByReference(String)
     */
    @MCRCommand(syntax = "delete access key for {0} with secret {1}",
        help = "Deletes an access key for reference {0} with (hashed) value {1}")
    public static void deleteAccessKey(String reference, String value) throws MCRAccessException {
        final MCRAccessKeyDto outdatedAccessKeyDto
            = MCRAccessKeyServiceFactory.getService().getAccessKeyByReferenceAndValue(reference, value);
        if (outdatedAccessKeyDto == null) {
            throw new MCRAccessKeyNotFoundException("Access key does not exist");
        }
        MCRAccessKeyServiceFactory.getService().deleteAccessKeyById(outdatedAccessKeyDto.getId());
        LOGGER.info("Deleted access key ({}) for {}.", value, reference);
    }

    /**
     * Imports access key for reference from file by path.
     *
     * @param reference the reference
     * @param path the path
     * @throws IOException
     * @throws MCRAccessException if current user is not allowed to update access key
     * @throws MCRAccessKeyException if an error occurs while importing access key
     *
     * @see org.mycore.mcr.acl.accesskey.MCRAccessKeyServiceImpl#importAccessKey(MCRAccessKeyDto)
     */
    @MCRCommand(syntax = "import access keys for {0} from file {1}",
        help = "Imports access keys for reference {0} from file {1} in JSON array format")
    public static void importAccessKeysFromFile(String reference, String path) throws IOException, MCRAccessException {
        final String json = Files.readString(Path.of(path), UTF_8);
        final List<MCRAccessKeyDto> accessKeyDtos
            = MCRAccessKeyJsonMapper.jsonToAccessKeyDtos(json).stream().peek(a -> a.setReference(reference)).toList();
        for (MCRAccessKeyDto accessKeyDto : accessKeyDtos) {
            MCRAccessKeyServiceFactory.getService().importAccessKey(accessKeyDto);
        }
        LOGGER.info("Imported access keys for {} from file {}.", reference, path);
    }

    /**
     * Exports access key for reference to file by path.
     *
     * @param reference the reference
     * @param path the path
     * @throws MCRAccessKeyException if an error occurs while exporting access keys
     *
     * @see org.mycore.mcr.acl.accesskey.MCRAccessKeyServiceImpl#getAccessKeysByReference(String)
     */
    @MCRCommand(syntax = "export access keys for {0} to file {1}",
        help = "Exports access keys for reference {0} to file {1} in JSON array format")
    public static void exportAccessKeysToFile(String reference, String path) throws IOException {
        final List<MCRAccessKeyDto> accessKeyDtos
            = MCRAccessKeyServiceFactory.getService().getAccessKeysByReference(reference);
        final String json = MCRAccessKeyJsonMapper.accessKeyDtosToJson(accessKeyDtos);
        Files.writeString(Path.of(path), json, UTF_8);
        LOGGER.info("Exported access keys for {} to file {}.", reference, path);
    }

    /**
     * Deletes all access key value attributes of users if corresponding access key does not exist.
     */
    @MCRCommand(syntax = "clean up access key user attributes",
        help = "Cleans all access key secret attributes of users if the corresponding key does not exist.")
    public static void cleanUp() {
        MCRAccessKeyServiceFactory.getUserService().cleanUpUserAttributes();
        LOGGER.info("Cleaned up access keys.");
    }

    /**
     * Returns processed access key value for reference to command output.
     *
     * @param value the value
     * @param reference the reference
     */
    @MCRCommand(syntax = "hash access key secret {0} for {1}", help = "Hashes value {0} for reference {1}")
    public static void hashSecret(String value, String reference) {
        final String result = MCRAccessKeyServiceFactory.getService().getValue(reference, value);
        LOGGER.info("Hashed secret for {}: '{}'.", reference, result);
    }

    private static MCRAccessKeyDto readAccessKeyFromFile(String path) throws IOException {
        final String json = Files.readString(Path.of(path), UTF_8);
        return MCRAccessKeyJsonMapper.jsonToAccessKeyDto(json);
    }
}
