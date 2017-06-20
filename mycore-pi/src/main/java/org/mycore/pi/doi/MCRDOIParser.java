package org.mycore.pi.doi;

import java.util.Optional;

import org.mycore.pi.MCRPersistentIdentifierParser;

public class MCRDOIParser implements MCRPersistentIdentifierParser<MCRDigitalObjectIdentifier> {

    public static final String DIRECTORY_INDICATOR = "10.";

    @Override
    public Optional<MCRDigitalObjectIdentifier> parse(String doi) {
        if (!doi.startsWith(DIRECTORY_INDICATOR)) {
            return Optional.empty();
        }

        String[] doiParts = doi.split("/", 2);

        if (doiParts.length != 2) {
            return Optional.empty();
        }

        String prefix = doiParts[0];
        String suffix = doiParts[1];

        if (suffix.length() == 0) {
            return Optional.empty();
        }

        return Optional.of(new MCRDigitalObjectIdentifier(prefix, suffix));
    }
}
