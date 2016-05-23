package org.mycore.pi;

import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;


public class MCRMockInscriber implements MCRPersistentIdentifierInscriber<MCRMockIdentifier> {

    @Override
    public void insertIdentifier(MCRMockIdentifier identifier, MCRBase obj, String additional) throws MCRPersistentIdentifierException {

    }

    @Override
    public void removeIdentifier(MCRMockIdentifier identifier, MCRBase obj, String additional) {

    }

    @Override
    public boolean hasIdentifier(MCRBase obj, String additional) throws MCRPersistentIdentifierException {
        return false;
    }
}
