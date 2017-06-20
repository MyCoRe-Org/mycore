package org.mycore.pi.frontend;

import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIRegistrationService;
import org.mycore.pi.MCRPIRegistrationServiceManager;
import org.mycore.pi.MCRPersistentIdentifier;

public class MCRIdentifierXSLUtils {

    public static boolean hasIdentifierCreated(String service, String id, String additional) {
        MCRPIRegistrationService<MCRPersistentIdentifier> registrationService = MCRPIRegistrationServiceManager
            .getInstance().getRegistrationService(service);
        return registrationService.isCreated(MCRObjectID.getInstance(id), additional);
    }

    public static boolean hasIdentifierRegistered(String service, String id, String additional) {
        MCRPIRegistrationService<MCRPersistentIdentifier> registrationService = MCRPIRegistrationServiceManager
            .getInstance().getRegistrationService(service);
        return registrationService.isRegistered(MCRObjectID.getInstance(id), additional);
    }

}
