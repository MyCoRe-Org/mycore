package org.mycore.pi.cli;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.mycore.access.MCRAccessException;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRException;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.pi.MCRPIRegistrationService;
import org.mycore.pi.MCRPIRegistrationServiceManager;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.MCRPersistentIdentifierManager;
import org.mycore.pi.MCRPersistentIdentifierMetadataManager;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.pi.urn.MCRDNBURN;

@MCRCommandGroup(name = "PI Commands")
public class MCRPICommands {

    private static final Logger LOGGER = LogManager.getLogger();

    @MCRCommand(syntax = "add PI Flags to objects", help = "Should only be used if you used mycore-pi pre 2016 lts!")
    public static void addFlagsToObjects() {
        MCRPersistentIdentifierManager.getInstance().getList().forEach(registrationInfo -> {
            if (registrationInfo.getMcrRevision() <= 35726) {
                String mycoreID = registrationInfo.getMycoreID();
                MCRObjectID objectID = MCRObjectID.getInstance(mycoreID);
                MCRBase base = MCRMetadataManager.retrieve(objectID);
                LOGGER.info("Add PI-Flag to " + mycoreID);
                MCRPIRegistrationService.addFlagToObject(base, (MCRPI) registrationInfo);
                try {
                    MCRMetadataManager.update(base);
                } catch (IOException | MCRAccessException | MCRActiveLinkException e) {
                    throw new MCRException(e);
                }
            }
        });
    }

    @MCRCommand(syntax = "migrate urn granular to service id {0}", help = "Used to migrate urn granular to MyCoRe-PI. " +
            "{0} should be your granular service id.")
    public static void migrateURNGranularToServiceID(String serviceID) {
        Session session = MCRHIBConnection.instance().getSession();
        MCRXMLMetadataManager.instance().listIDsOfType("derivate").stream().forEach(derivateID -> {
            MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(derivateID));

            String urn = derivate.getDerivate().getURN();
            if (urn != null) {
                LOGGER.info("Found URN in :" + derivateID);
                MCRPI derivatePI = new MCRPI(urn, MCRDNBURN.TYPE, derivateID, "", serviceID, new Date());
                if (MCRPersistentIdentifierManager.getInstance().exist(derivatePI)) {
                    LOGGER.warn("PI-Entry for " + urn + " already exist!");
                } else {
                    session.save(derivatePI);
                    derivate.getUrnMap().forEach((file, fileURN) -> {
                        MCRPI filePI = new MCRPI(fileURN, MCRDNBURN.TYPE, derivateID, file, serviceID, new Date());
                        if (MCRPersistentIdentifierManager.getInstance().exist(filePI)) {
                            LOGGER.warn("PI-Entry for " + fileURN + " already exist!");
                        } else {
                            session.save(fileURN);
                        }
                    });
                }
            }
        });
    }

    @MCRCommand(syntax = "try to control {0} with service {1} with additional {2}", help = "This command tries to" +
            " read a pi from the object {0} with the MetadataManager from the specified service {1}." +
            " If the service configuration is right then the pi is under control of MyCoRe.")
    public static void controlObjectWithServiceAndAdditional(String objectIDString, String serviceID, final String additional)
        throws MCRAccessException, MCRActiveLinkException, IOException {
        String trimAdditional = additional.trim();
        MCRPIRegistrationService<MCRPersistentIdentifier> service = MCRPIRegistrationServiceManager
            .getInstance().getRegistrationService(serviceID);

        MCRPersistentIdentifierMetadataManager<MCRPersistentIdentifier> metadataManager = service.getMetadataManager();

        MCRObjectID objectID = MCRObjectID.getInstance(objectIDString);
        MCRBase mcrBase = MCRMetadataManager.retrieve(objectID);
        Optional<MCRPersistentIdentifier> identifier;

        try {
            identifier = metadataManager.getIdentifier(mcrBase, trimAdditional);
        } catch (MCRPersistentIdentifierException e) {
            LOGGER.info("Could not detect any identifier with service " + serviceID, e);
            return;
        }

        if (!identifier.isPresent()) {
            LOGGER.info("Could not detect any identifier with service " + serviceID);
            return;
        }

        MCRPersistentIdentifier persistentIdentifier = identifier.get();
        if (service.isRegistered(objectID, trimAdditional)) {
            LOGGER.info("Already present in Database: " + serviceID);
            return;
        }
        MCRPI mcrpi = service.insertIdentifierToDatabase(mcrBase, trimAdditional, persistentIdentifier);
        MCRPIRegistrationService.addFlagToObject(mcrBase, mcrpi);
        MCRMetadataManager.update(mcrBase);
        LOGGER.info("{}:{} is now under control of {}", objectID, trimAdditional, serviceID);
    }

}
