package org.mycore.pi;


import java.util.stream.Stream;

import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;

public class MCRLocalPersistentIdentifierResolver extends MCRPersistentIdentifierResolver<MCRPersistentIdentifier> {
    public MCRLocalPersistentIdentifierResolver() {
        super("Local-Resolver");
    }

    @Override
    public Stream<String> resolve(MCRPersistentIdentifier identifier) throws MCRIdentifierUnresolvableException {
        return MCRPersistentIdentifierManager.getInstance().getInfo(identifier.asString()).stream().map(info -> {
            return MCRFrontendUtil.getBaseURL() + "receive/" + info.getMycoreID();
        });
    }
}
