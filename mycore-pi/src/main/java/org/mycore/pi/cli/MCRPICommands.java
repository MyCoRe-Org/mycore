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

package org.mycore.pi.cli;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRException;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.pi.MCRPIManager;
import org.mycore.pi.MCRPIMetadataService;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.MCRPIService;
import org.mycore.pi.MCRPIServiceManager;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.MCRPersistentIdentifierEventHandler;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.pi.urn.MCRDNBURN;

@MCRCommandGroup(name = "PI Commands")
public class MCRPICommands {

    private static final Logger LOGGER = LogManager.getLogger();

    @MCRCommand(syntax = "add pi flags to all objects",
        help = "Should only be used if you used mycore-pi pre 2016 lts!",
        order = 10)
    public static List<String> addFlagsToObjects() {
        return MCRPIManager.getInstance().getList().stream()
            .filter(registrationInfo -> {
                String mycoreID = registrationInfo.getMycoreID();
                MCRObjectID objectID = MCRObjectID.getInstance(mycoreID);
                MCRBase base = MCRMetadataManager.retrieve(objectID);
                return !MCRPIService.hasFlag(base, registrationInfo.getAdditional(), registrationInfo);
            })
            .map(MCRPIRegistrationInfo::getMycoreID)
            .distinct()
            .map(id -> "add pi flags to object " + id)
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "add pi flags to object {0}", help = "see add pi flags to all objects!", order = 20)
    public static void addFlagToObject(String mycoreIDString) {
        MCRObjectID objectID = MCRObjectID.getInstance(mycoreIDString);
        MCRBase base = MCRMetadataManager.retrieve(objectID);
        final List<MCRPIRegistrationInfo> pi = MCRPIManager.getInstance().getRegistered(base);

        final boolean addedAFlag = pi.stream().filter(registrationInfo -> {
            if (!MCRPIService.hasFlag(base, registrationInfo.getAdditional(), registrationInfo)) {
                LOGGER.info("Add PI-Flag to " + mycoreIDString);
                MCRPIService.addFlagToObject(base, (MCRPI) registrationInfo);
                return true;
            }
            return false;
        }).count() > 0;

        if (addedAFlag) {
            try {
                MCRMetadataManager.update(base);
            } catch (MCRAccessException e) {
                throw new MCRException(e);
            }
        }
    }

    @MCRCommand(syntax = "migrate urn granular to service id {0}",
        help = "Used to migrate urn granular to MyCoRe-PI. "
            +
            "{0} should be your granular service id.",
        order = 30)
    public static void migrateURNGranularToServiceID(String serviceID) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        MCRXMLMetadataManager.instance().listIDsOfType("derivate").forEach(derivateID -> {
            MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(derivateID));

            String urn = derivate.getDerivate().getURN();
            if (urn != null) {
                LOGGER.info("Found URN in :{}", derivateID);
                MCRPI derivatePI = new MCRPI(urn, MCRDNBURN.TYPE, derivateID, "", serviceID, new Date());
                if (MCRPIManager.getInstance().exist(derivatePI)) {
                    LOGGER.warn("PI-Entry for {} already exist!", urn);
                } else {
                    em.persist(derivatePI);
                    derivate.getUrnMap().forEach((file, fileURN) -> {
                        MCRPI filePI = new MCRPI(fileURN, MCRDNBURN.TYPE, derivateID, file, serviceID, new Date());
                        if (MCRPIManager.getInstance().exist(filePI)) {
                            LOGGER.warn("PI-Entry for {} already exist!", fileURN);
                        } else {
                            em.persist(fileURN);
                        }
                    });
                }
            }
        });
    }

    @MCRCommand(syntax = "try to control {0} with pi service {1}",
        help = "This command tries to" +
            " read a pi from the object {0} with the MetadataManager from the specified service {1}." +
            " If the service configuration is right then the pi is under control of MyCoRe.",
        order = 50)
    public static void controlObjectWithService(String objectIDString, String serviceID)
        throws MCRAccessException, MCRActiveLinkException, IOException {
        controlObjectWithServiceAndAdditional(objectIDString, serviceID, null);
    }

    @MCRCommand(syntax = "try to control {0} with pi service {1} with additional {2}",
        help = "This command tries to" +
            " read a pi from the object {0} with the MetadataManager from the specified service {1}." +
            " If the service configuration is right then the pi is under control of MyCoRe.",
        order = 40)
    public static void controlObjectWithServiceAndAdditional(String objectIDString, String serviceID,
        final String additional)
        throws MCRAccessException, MCRActiveLinkException, IOException {

        String trimAdditional = additional != null ? additional.trim() : null;
        MCRPIService<MCRPersistentIdentifier> service = MCRPIServiceManager
            .getInstance().getRegistrationService(serviceID);

        MCRPIMetadataService<MCRPersistentIdentifier> metadataManager = service.getMetadataService();

        MCRObjectID objectID = MCRObjectID.getInstance(objectIDString);
        MCRBase mcrBase = MCRMetadataManager.retrieve(objectID);
        Optional<MCRPersistentIdentifier> identifier;

        try {
            identifier = metadataManager.getIdentifier(mcrBase, trimAdditional);
        } catch (MCRPersistentIdentifierException e) {
            LOGGER.info("Could not detect any identifier with service {}", serviceID, e);
            return;
        }

        if (!identifier.isPresent()) {
            LOGGER.info("Could not detect any identifier with service {}", serviceID);
            return;
        }

        MCRPersistentIdentifier persistentIdentifier = identifier.get();
        if (service.isRegistered(objectID, trimAdditional)) {
            LOGGER.info("Already present in Database: {}", serviceID);
            return;
        }
        MCRPI mcrpi = service.insertIdentifierToDatabase(mcrBase, trimAdditional, persistentIdentifier);
        MCRPIService.addFlagToObject(mcrBase, mcrpi);
        MCRMetadataManager.update(mcrBase);
        LOGGER.info("{}:{} is now under control of {}", objectID, trimAdditional, serviceID);
    }

    @MCRCommand(syntax = "remove control {0} with pi service {1} with additional {2}",
        help = "This commands removes the "
            + "pi control from the object {0}(object id) with the serivce {1}(service id) and the additional {2}",
        order = 60)
    public static void removeControlFromObject(String objectIDString, String serviceID, String additional)
        throws MCRAccessException, MCRActiveLinkException, MCRPersistentIdentifierException {
        MCRObjectID objectID = MCRObjectID.getInstance(objectIDString);
        MCRPI mcrpi = MCRPIManager.getInstance()
            .get(serviceID, objectIDString, additional != null ? additional.trim() : null);
        MCRPIManager.getInstance()
            .delete(mcrpi.getMycoreID(), mcrpi.getAdditional(), mcrpi.getType(), mcrpi.getService());

        MCRBase base = MCRMetadataManager.retrieve(objectID);
        if (MCRPIService.removeFlagFromObject(base, mcrpi) == null) {
            throw new MCRPersistentIdentifierException("Could not delete Flag of object (flag not found)!");
        }
        MCRMetadataManager.update(base);
    }

    @MCRCommand(syntax = "remove control {0} with pi service {1}",
        help = "This commands removes the "
            + "pi control from the object {0}(object id) with the serivce {1}(service id)",
        order = 70)
    public static void removeControlFromObject(String objectIDString, String serviceID)
        throws MCRAccessException, MCRActiveLinkException, MCRPersistentIdentifierException {
        removeControlFromObject(objectIDString, serviceID, null);
    }

    @MCRCommand(syntax = "update all PI of object {0}",
        help = "Triggers the update method of every Object!",
        order = 80)
    public static void updateObject(String objectIDString) {
        MCRObjectID objectID = MCRObjectID.getInstance(objectIDString);
        MCRObject object = MCRMetadataManager.retrieveMCRObject(objectID);
        MCRPersistentIdentifierEventHandler.updateObject(object);
    }

    @MCRCommand(syntax = "create pi with {0} for object {1} with additional ({2})",
        help = "Creates a persistent identifier with the pi service with the id {0} for the object {1}"
            + " with additional ({2}). Does nothing if the object already has a pi from the service {0}.",
        order = 90)
    public static void createPIForObjectIfNotExist(String serviceID, String objectIDString, String additional)
        throws MCRAccessException, ExecutionException, MCRActiveLinkException, MCRPersistentIdentifierException,
        InterruptedException {
        final MCRObjectID objectID = MCRObjectID.getInstance(objectIDString);

        if (!MCRMetadataManager.exists(objectID)) {
            LOGGER.error("Object {} does not exist!", objectID);
            return;
        }

        MCRPIService<MCRPersistentIdentifier> registrationService = MCRPIServiceManager
            .getInstance().getRegistrationService(serviceID);

        if (registrationService.isCreated(objectID, "")) {
            LOGGER.info("Object {} already has a DOI!", objectID);
        }
        final MCRBase object = MCRMetadataManager.retrieve(objectID);
        final MCRPersistentIdentifier doi = registrationService.register(object, additional);
        LOGGER.info("Registered pi with {}: {} for object {}", serviceID, doi.asString(), objectID);
    }

    @MCRCommand(syntax = "create pi with {0} for object {1}",
        help = "Creates a persistent identifier with the pi service with the id {0} for the object {1}."
            + " Does nothing if the object already has a pi from the service {0}.",
        order = 100)
    public static void createPIForObjectIfNotExist(String serviceID, String objectIDString)
        throws MCRAccessException, ExecutionException, MCRActiveLinkException, MCRPersistentIdentifierException,
        InterruptedException {
        createPIForObjectIfNotExist(serviceID, objectIDString, "");
    }
}
