package org.mycore.pi.urn;

import static org.mycore.pi.urn.MCRDNBURN.URN_NID;
import static org.mycore.pi.urn.MCRUniformResourceName.PREFIX;

import java.util.Optional;

import org.mycore.pi.MCRPersistentIdentifierParser;

public class MCRDNBURNParser implements MCRPersistentIdentifierParser<MCRDNBURN> {

    @Override
    public Optional<MCRDNBURN> parse(String identifier) {
        String prefix = PREFIX + URN_NID;
        if (identifier.startsWith(prefix)) {
            int lastColon = identifier.lastIndexOf(":") + 1;
            int checkSumStart = identifier.length() - 1;

            String namespace = identifier.substring(prefix.length(), lastColon);
            String nsss = identifier.substring(lastColon, checkSumStart);

            return Optional.of(new MCRDNBURN(namespace, nsss));
        }

        return Optional.empty();
    }
}
