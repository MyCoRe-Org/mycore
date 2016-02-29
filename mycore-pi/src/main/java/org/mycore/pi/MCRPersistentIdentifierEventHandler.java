package org.mycore.pi;


import java.util.List;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRPersistentIdentifierEventHandler extends MCREventHandlerBase {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        detectServices(obj, (service, identifier) -> {
            try {
                service.onUpdate(identifier, obj);
            } catch (MCRPersistentIdentifierException e) {
                throw new MCRException(e);
            }
        });
    }

    @Override
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        detectServices(obj, (service, identifier) -> {
            try {
                service.onDelete(identifier, obj);
            } catch (MCRPersistentIdentifierException e) {
                throw new MCRException(e);
            }
        });
    }

    private void detectServices(MCRObject obj, BiConsumer<MCRPIRegistrationService, MCRPersistentIdentifier> r) {
        MCRPersistentIdentifierManager identifierManager = MCRPersistentIdentifierManager.getInstance();
        MCRPIRegistrationServiceManager serviceManager = MCRPIRegistrationServiceManager.getInstance();

        List<MCRPIRegistrationInfo> registered = MCRPersistentIdentifierManager.getRegistered(obj);
        List<String> serviceList = serviceManager.getServiceList();

        for (MCRPIRegistrationInfo pi : registered) {
            String serviceName = pi.getService();
            if (serviceList.contains(serviceName)) {
                MCRPersistentIdentifierParser parser = identifierManager.getParserForType(pi.getType());
                MCRPersistentIdentifier identifier = parser.parse(pi.getIdentifier()).orElseThrow(() -> new MCRException("Cannot parse a previous inserted identifier"));
                MCRPIRegistrationService<MCRPersistentIdentifier> registrationService = serviceManager.getRegistrationService(serviceName);
                r.accept(registrationService, identifier);
            } else {
                LOGGER.warn(() -> "The service " + serviceName + " was removed from properties, so the update function!");
            }
        }
    }


}
