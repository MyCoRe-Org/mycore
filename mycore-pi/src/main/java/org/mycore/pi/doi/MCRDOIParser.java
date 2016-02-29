package org.mycore.pi.doi;


import java.util.Optional;

import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.MCRPersistentIdentifierParser;

public class MCRDOIParser implements MCRPersistentIdentifierParser {

    // The Prefix of every doi Prefix
    public static final String PREFIX_PREFIX = "10.";

    @Override
    public Optional<MCRPersistentIdentifier> parse(String doi) {
        if (!doi.startsWith(PREFIX_PREFIX)) {
            return Optional.empty();
        }

        String[] doiParts = doi.split("/", 2);

        if (doiParts.length != 2) {
            return Optional.empty();
        }

        String prefix = doiParts[0];
        String suffix = doiParts[1];

        // the part after 10.
        String prefixSuffix = prefix.substring(PREFIX_PREFIX.length());

        try {
            Integer.parseInt(prefixSuffix, 10);
        } catch (NumberFormatException nfe) {
            return Optional.empty();
        }

        if (suffix.length() == 0) {
            return Optional.empty();
        }

        return Optional.of(new MCRDigitalObjectIdentifier(prefix, suffix));
    }
}
