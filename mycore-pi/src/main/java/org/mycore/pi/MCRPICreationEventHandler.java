package org.mycore.pi;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRPICreationEventHandler extends MCREventHandlerBase {

    @Override
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        this.handleObjectUpdated(evt, obj);
    }

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        List<MCRPIRegistrationInfo> registered = MCRPIManager.getInstance().getRegistered(obj);

        final List<String> services = registered.stream().map(MCRPIRegistrationInfo::getService)
            .collect(Collectors.toList());

        MCRPIServiceManager.getInstance().getAutoCreationList().stream()
            .filter(Predicate.not(services::contains))
            .filter(s -> s.getCreationPredicate().test(obj))
            .forEach((serviceToRegister) -> {
                try {
                    serviceToRegister.register(obj, "", false);
                } catch (MCRAccessException | MCRActiveLinkException | MCRPersistentIdentifierException
                    | ExecutionException | InterruptedException e) {
                    throw new MCRException("Error while register pi for object " + obj.getId().toString(), e);
                }
            });
    }
}
