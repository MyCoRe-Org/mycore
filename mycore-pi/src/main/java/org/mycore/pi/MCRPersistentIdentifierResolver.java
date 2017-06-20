package org.mycore.pi;

import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;

public abstract class MCRPersistentIdentifierResolver<T extends MCRPersistentIdentifier> {

    private static final Logger LOGGER = LogManager.getLogger();

    private final String name;

    public MCRPersistentIdentifierResolver(String name) {
        this.name = name;
    }

    public abstract Stream<String> resolve(T identifier) throws MCRIdentifierUnresolvableException;

    public Stream<String> resolveSuppress(T identifier) {
        try {
            return resolve(identifier);
        } catch (MCRIdentifierUnresolvableException e) {
            LOGGER.info(e);
            return Stream.empty();
        }
    }

    public String getName() {
        return name;
    }
}
