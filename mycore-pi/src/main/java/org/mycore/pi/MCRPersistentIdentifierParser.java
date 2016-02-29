package org.mycore.pi;

import java.util.Optional;

public interface MCRPersistentIdentifierParser {
    Optional<MCRPersistentIdentifier> parse(String identifier);
}
