package org.mycore.pi.doi;

import java.util.Optional;
import java.util.UUID;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.MCRPersistentIdentifierGenerator;

public class MCRUUIDDOIGenerator implements MCRPersistentIdentifierGenerator<MCRDigitalObjectIdentifier> {


    private final MCRDOIParser mcrdoiParser;

    private String prefix = MCRConfiguration.instance().getString("MCR.DOI.Prefix");

    public MCRUUIDDOIGenerator() {
        mcrdoiParser = new MCRDOIParser();
    }

    @Override
    public MCRDigitalObjectIdentifier generate(MCRObjectID mcrID, String additional) {
        Optional<MCRPersistentIdentifier> parse = mcrdoiParser.parse(prefix + "/" + UUID.randomUUID());
        MCRPersistentIdentifier doi = parse.get();
        return (MCRDigitalObjectIdentifier) doi;
    }
}
