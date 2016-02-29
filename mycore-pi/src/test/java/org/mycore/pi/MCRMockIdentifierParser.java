package org.mycore.pi;

import java.util.Optional;

public class MCRMockIdentifierParser implements MCRPersistentIdentifierParser {
    public static final String MOCK_SCHEME = "MOCK:";

    @Override
    public Optional<MCRPersistentIdentifier> parse(String identifier) {
        if(!identifier.startsWith(MOCK_SCHEME)){
            return Optional.empty();
        }

        return Optional.of(new MCRMockIdentifier(identifier));
    }
}
