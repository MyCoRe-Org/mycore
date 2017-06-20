package org.mycore.pi;

import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRMockIdentifierRegistrationService extends MCRPIRegistrationService<MCRMockIdentifier> {
    protected static final String TYPE = "mock";

    public MCRMockIdentifierRegistrationService(String registrationServiceID) {
        super(registrationServiceID, TYPE);
    }

    private boolean registerCalled = false;

    private boolean deleteCalled = false;

    private boolean updatedCalled = false;

    @Override
    protected MCRMockIdentifier registerIdentifier(MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        registerCalled = true;
        return getNewIdentifier(obj.getId(), "");
    }

    @Override
    public void delete(MCRMockIdentifier identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        deleteCalled = true;
    }

    @Override
    public void update(MCRMockIdentifier identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        updatedCalled = true;
    }

    public boolean isRegisterCalled() {
        return registerCalled;
    }

    public boolean isDeleteCalled() {
        return deleteCalled;
    }

    public boolean isUpdatedCalled() {
        return updatedCalled;
    }
}
