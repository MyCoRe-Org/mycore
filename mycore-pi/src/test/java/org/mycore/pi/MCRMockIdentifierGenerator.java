package org.mycore.pi;


import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRMockIdentifierGenerator implements MCRPersistentIdentifierGenerator<MCRMockIdentifier> {
    @Override
    public MCRMockIdentifier generate(MCRObjectID mcrID, String additional) throws MCRPersistentIdentifierException {
        return (MCRMockIdentifier) new MCRMockIdentifierParser().parse(MCRMockIdentifier.MOCK_SCHEME + mcrID.toString() + ":" + additional).get();
    }
}
