package org.mycore.pi.doi;

import java.util.Optional;
import java.util.UUID;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.MCRPersistentIdentifierGenerator;

public class MCRUUIDDOIGenerator extends MCRPersistentIdentifierGenerator<MCRDigitalObjectIdentifier> {

    private final MCRDOIParser mcrdoiParser;

    private String prefix = MCRConfiguration.instance().getString("MCR.DOI.Prefix");

    public MCRUUIDDOIGenerator(String generatorString) {
        super(generatorString);
        mcrdoiParser = new MCRDOIParser();
    }

    @Override
    public MCRDigitalObjectIdentifier generate(MCRObjectID mcrID, String additional) {
        Optional<MCRDigitalObjectIdentifier> parse = mcrdoiParser.parse(prefix + "/" + UUID.randomUUID());
        MCRPersistentIdentifier doi = parse.get();
        return (MCRDigitalObjectIdentifier) doi;
    }
}
