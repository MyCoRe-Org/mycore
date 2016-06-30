package org.mycore.pi.cli;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRException;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.pi.MCRPIRegistrationService;
import org.mycore.pi.MCRPersistentIdentifierManager;
import org.mycore.pi.backend.MCRPI;

@MCRCommandGroup(name = "PI Commands")
public class MCRPICommands {

    private static final Logger LOGGER = LogManager.getLogger();

    @MCRCommand(syntax = "add PI Flags to objects", help = "Should only be used if you used mycore-pi pre 2016 lts!")
    public static void addFlagsToObjects() {
        MCRPersistentIdentifierManager.getList().forEach(registrationInfo -> {
            if (registrationInfo.getMcrRevision() <= 35726) {
                String mycoreID = registrationInfo.getMycoreID();
                MCRObjectID objectID = MCRObjectID.getInstance(mycoreID);
                MCRBase base = MCRMetadataManager.retrieve(objectID);
                LOGGER.info("Add PI-Flag to " + mycoreID);
                MCRPIRegistrationService.addFlagToObject(base, (MCRPI) registrationInfo);
                try {
                    MCRMetadataManager.update(base);
                } catch (IOException|MCRAccessException|MCRActiveLinkException e) {
                    throw new MCRException(e);
                }
            }
        });
    }


}
