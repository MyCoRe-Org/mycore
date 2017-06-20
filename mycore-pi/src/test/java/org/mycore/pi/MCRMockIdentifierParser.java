package org.mycore.pi;

import java.util.Optional;

public class MCRMockIdentifierParser implements MCRPersistentIdentifierParser {

    @Override
    public Optional<MCRPersistentIdentifier> parse(String identifier) {
        if (!identifier.startsWith(MCRMockIdentifier.MOCK_SCHEME)) {
            return Optional.empty();
        }

        return Optional.of(new MCRMockIdentifier(identifier));
    }
}
