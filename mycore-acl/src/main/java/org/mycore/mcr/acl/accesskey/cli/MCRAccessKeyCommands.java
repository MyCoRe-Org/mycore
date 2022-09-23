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
import org.mycore.common.MCRException;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyManager;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyTransformer;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyUtils;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;

@MCRCommandGroup(
    name = "Access keys")
public class MCRAccessKeyCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    @MCRCommand(syntax = "clear all access keys",
        help = "Clears all access keys")
    public static void clearAccessKeys() {
        MCRAccessKeyManager.clearAccessKeys();
        LOGGER.info("Cleared all access keys.");
    }

    @MCRCommand(syntax = "clear all access keys for {0}",
        help = "Clears all access keys for MCRObject/Derivate {0}")
    public static void clearAccessKeys(final String objectIdString) throws MCRException {
        final MCRObjectID objectId = MCRObjectID.getInstance(objectIdString);
        if (!MCRMetadataManager.exists(objectId)) {
            throw new MCRException("MCRObject/Derivate doesn't exists.");
        }
        MCRAccessKeyManager.clearAccessKeys(objectId);
        LOGGER.info("Cleared all access keys of {}.", objectIdString);
    }

    @MCRCommand(syntax = "create access key for {0} from file {1}",
        help = "Creates an access key {0} for MCRObject/Derivate from file {1} in json format")
    public static void createAccessKey(final String objectIdString, final String pathString)
        throws IOException, MCRException {
        final MCRObjectID objectId = MCRObjectID.getInstance(objectIdString);
        if (!MCRMetadataManager.exists(objectId)) {
            throw new MCRException("MCRObject/Derivate doesn't exists.");
        }
        final MCRAccessKey accessKey = readAccessKeyFromFile(pathString);
        MCRAccessKeyManager.createAccessKey(objectId, accessKey);
        LOGGER.info("Created access key for {}.", objectIdString);
    }

    @MCRCommand(syntax = "update access key for {0} with secret {1} from file {2}",
        help = "Updates an access key for MCRObject/Derivate {0}"
            + " with (hashed) secret {1} from file {2} in json format")
    public static void updateAccessKey(final String objectIdString, final String secret, final String pathString)
        throws IOException, MCRException {
        final MCRObjectID objectId = MCRObjectID.getInstance(objectIdString);
        if (!MCRMetadataManager.exists(objectId)) {
            throw new MCRException("MCRObject/Derivate doesn't exists.");
        }
        final MCRAccessKey accessKey = readAccessKeyFromFile(pathString);
        MCRAccessKeyManager.updateAccessKey(objectId, secret, accessKey);
        LOGGER.info("Updated access key ({}) for {}.", secret, objectIdString);
    }

    @MCRCommand(syntax = "delete access key for {0} with secret {1}",
        help = "Deletes an access key for MCRObject/Derivate {0} with (hashed) secret {1}")
    public static void removeAccessKey(final String objectIdString, final String secret) throws MCRException {
        final MCRObjectID objectId = MCRObjectID.getInstance(objectIdString);
        if (!MCRMetadataManager.exists(objectId)) {
            throw new MCRException("MCRObject/Derivate doesn't exists.");
        }
        MCRAccessKeyManager.removeAccessKey(objectId, secret);
        LOGGER.info("Deleted access key ({}) for {}.", secret, objectIdString);
    }

    @MCRCommand(syntax = "import access keys for {0} from file {1}",
        help = "Imports access keys for MCRObject/Derivate {0} from file {1} in json array format")
    public static void importAccessKeysFromFile(final String objectIdString, final String pathString)
        throws IOException, MCRException {
        final MCRObjectID objectId = MCRObjectID.getInstance(objectIdString);
        if (!MCRMetadataManager.exists(objectId)) {
            throw new MCRException("MCRObject/Derivate doesn't exists.");
        }
        final Path path = Path.of(pathString);
        final String accessKeysJson = Files.readString(path, UTF_8);
        final List<MCRAccessKey> accessKeys = MCRAccessKeyTransformer.accessKeysFromJson(accessKeysJson);
        MCRAccessKeyManager.addAccessKeys(objectId, accessKeys);
        LOGGER.info("Imported access keys for {} from file {}.", objectIdString, pathString);
    }

    @MCRCommand(syntax = "export access keys for {0} to file {1}",
        help = "Exports access keys for MCRObject/Derivate {0} to file {1} in json array format")
    public static void exportAccessKeysToFile(final String objectIdString, final String pathString)
        throws IOException, MCRException {
        final MCRObjectID objectId = MCRObjectID.getInstance(objectIdString);
        if (!MCRMetadataManager.exists(objectId)) {
            throw new MCRException("MCRObject/Derivate doesn't exists.");
        }
        final Path path = Path.of(pathString);
        final List<MCRAccessKey> accessKeys = MCRAccessKeyManager.listAccessKeys(objectId);
        final String accessKeysJson = MCRAccessKeyTransformer.jsonFromAccessKeys(accessKeys);
        Files.writeString(path, accessKeysJson, UTF_8);
        LOGGER.info("Exported access keys for {} to file {}.", objectIdString, pathString);
    }

    @MCRCommand(syntax = "clean up access key user attributes",
        help = "Cleans all access key secret attributes of users if the corresponding key does not exist.")
    public static void cleanUp() {
        MCRAccessKeyUtils.cleanUpUserAttributes();
        LOGGER.info("Cleaned up access keys.");
    }

    @MCRCommand(syntax = "hash access key secret {0} for {1}",
        help = "Hashes secret {0} for MCRObject/Derivate {1}")
    public static void hashSecret(final String secret, final String objectIdString) throws MCRException {
        final MCRObjectID objectId = MCRObjectID.getInstance(objectIdString);
        final String result = MCRAccessKeyManager.hashSecret(secret, objectId);
        LOGGER.info("Hashed secret for {}: '{}'.", objectIdString, result);
    }

    private static MCRAccessKey readAccessKeyFromFile(final String pathString) throws IOException, MCRException {
        final Path path = Path.of(pathString);
        final String accessKeyJson = Files.readString(path, UTF_8);
        return MCRAccessKeyTransformer.accessKeyFromJson(accessKeyJson);
    }
}
