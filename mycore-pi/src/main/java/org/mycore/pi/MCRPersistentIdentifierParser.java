package org.mycore.pi;

import java.util.Optional;

public interface MCRPersistentIdentifierParser<T extends MCRPersistentIdentifier> {
    Optional<T> parse(String identifier);
}
