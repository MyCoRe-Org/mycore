package org.mycore.pi.doi;

import java.util.Date;
import java.util.Locale;
import java.util.Optional;

import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.MCRPersistentIdentifierGenerator;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRCreateDateDOIGenerator implements MCRPersistentIdentifierGenerator<MCRDigitalObjectIdentifier> {

    private static final String DATE_PATTERN = "yyyyMMdd-HHmmss";
    private final MCRDOIParser mcrdoiParser;
    private String prefix = MCRConfiguration.instance().getString("MCR.DOI.Prefix");

    public MCRCreateDateDOIGenerator() {
        mcrdoiParser = new MCRDOIParser();
    }

    @Override
    public MCRDigitalObjectIdentifier generate(MCRObjectID mcrID, String additional) throws MCRPersistentIdentifierException {
        Date createdate = MCRMetadataManager.retrieveMCRObject(mcrID).getService().getDate("createdate");
        if (createdate != null) {
            MCRISO8601Date mcrdate = new MCRISO8601Date();
            mcrdate.setDate(createdate);
            String format = mcrdate.format(DATE_PATTERN, Locale.ENGLISH);
            Optional<MCRPersistentIdentifier> parse = mcrdoiParser.parse(prefix + "/" + format);
            MCRPersistentIdentifier doi = parse.get();
            return (MCRDigitalObjectIdentifier) doi;
        } else {
            throw new MCRPersistenceException("The object " + mcrID.toString() + " doesn't have a createdate!");
        }
    }
}
