package org.mycore.pi;

import java.util.function.Function;
import java.util.stream.Stream;

import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;

public class MCRLocalPersistentIdentifierResolver extends MCRPersistentIdentifierResolver<MCRPersistentIdentifier> {
    private final Function<String, String> toReceiveObjectURL = mcrID -> MCRFrontendUtil.getBaseURL() + "receive/"
        + mcrID;

    public MCRLocalPersistentIdentifierResolver() {
        super("Local-Resolver");
    }

    @Override
    public Stream<String> resolve(MCRPersistentIdentifier identifier) throws MCRIdentifierUnresolvableException {
        return MCRPersistentIdentifierManager.getInstance()
            .getInfo(identifier)
            .stream()
            .map(MCRPIRegistrationInfo::getMycoreID)
            .map(toReceiveObjectURL);
    }
}
