package org.mycore.pi;

import java.util.stream.Stream;

public class MCRMockResolver extends MCRPersistentIdentifierResolver<MCRMockIdentifier> {

    public static final String NAME = "MOCK-Resolver";

    public MCRMockResolver() {
        super(NAME);
    }

    @Override
    public Stream<String> resolve(MCRMockIdentifier identifier) {
        return Stream.of(identifier.asString());
    }
}
