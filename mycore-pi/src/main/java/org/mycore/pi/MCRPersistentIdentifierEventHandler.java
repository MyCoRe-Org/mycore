package org.mycore.pi;

import java.util.List;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRCoreVersion;
import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

import com.google.gson.Gson;

public class MCRPersistentIdentifierEventHandler extends MCREventHandlerBase {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        /* Add PIs to DB if they are not there */
        MCRPersistentIdentifierManager.getInstance().getRegistered(obj).forEach(pi -> {
            MCRPersistentIdentifierManager.getInstance().delete(pi.getMycoreID(), pi.getAdditional(), pi.getType(),
                pi.getService());
        });

        Gson gson = new Gson();
        obj.getService().getFlags(MCRPIRegistrationService.PI_FLAG).stream()
            .map(piFlag -> gson.fromJson(piFlag, MCRPI.class))
            .filter(entry -> !MCRPersistentIdentifierManager.getInstance().exist(entry))
            .forEach(entry -> {
                //TODO: disabled for MCR-1393
                //                    entry.setMcrRevision(MCRCoreVersion.getRevision());
                entry.setMcrVersion(MCRCoreVersion.getVersion());
                entry.setMycoreID(obj.getId().toString());
                LOGGER.info(
                    "Add PI : " + entry.getIdentifier() + " with service " + entry.getService() + " to database!");
                MCRHIBConnection.instance().getSession().save(entry);
            });

    }

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        detectServices(obj, (service, registrationInfo) -> {
            try {
                service.onUpdate(getIdentifier(registrationInfo), obj, registrationInfo.getAdditional());
            } catch (MCRPersistentIdentifierException e) {
                throw new MCRException(e);
            }
        });
    }

    @Override
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        detectServices(obj, (service, registrationInfo) -> {
            try {
                service.onDelete(getIdentifier(registrationInfo), obj, registrationInfo.getAdditional());
            } catch (MCRPersistentIdentifierException e) {
                throw new MCRException(e);
            }
        });
    }

    private void detectServices(MCRObject obj, BiConsumer<MCRPIRegistrationService, MCRPIRegistrationInfo> r) {
        MCRPIRegistrationServiceManager serviceManager = MCRPIRegistrationServiceManager.getInstance();

        List<MCRPIRegistrationInfo> registered = MCRPersistentIdentifierManager.getInstance().getRegistered(obj);
        List<String> serviceList = serviceManager.getServiceIDList();

        for (MCRPIRegistrationInfo pi : registered) {
            String serviceName = pi.getService();
            if (serviceList.contains(serviceName)) {
                getIdentifier(pi);
                MCRPIRegistrationService<MCRPersistentIdentifier> registrationService = serviceManager
                    .getRegistrationService(serviceName);
                r.accept(registrationService, pi);
            } else {
                LOGGER
                    .warn(() -> "The service " + serviceName + " was removed from properties, so the update function!");
            }
        }
    }

    private MCRPersistentIdentifier getIdentifier(MCRPIRegistrationInfo pi) {
        MCRPersistentIdentifierManager identifierManager = MCRPersistentIdentifierManager.getInstance();
        MCRPersistentIdentifierParser<?> parser = identifierManager.getParserForType(pi.getType());

        return parser.parse(pi.getIdentifier())
            .orElseThrow(() -> new MCRException("Cannot parse a previous inserted identifier"));
    }

}
