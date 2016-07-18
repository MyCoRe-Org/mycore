package org.mycore.pi;

import org.junit.Assert;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;


public class MCRMockInscriber extends MCRPersistentIdentifierInscriber<MCRMockIdentifier> {

    public static final String TEST_PROPERTY = "mockProperty";
    public static final String TEST_PROPERTY_VALUE = "mockPropertyValue";

    public MCRMockInscriber(String inscriberID) {
        super(inscriberID);
    }

    @Override
    public void insertIdentifier(MCRMockIdentifier identifier, MCRBase obj, String additional) throws MCRPersistentIdentifierException {
        Assert.assertEquals("Test propterties should be set!", getProperties().get(TEST_PROPERTY), TEST_PROPERTY_VALUE);
    }

    @Override
    public void removeIdentifier(MCRMockIdentifier identifier, MCRBase obj, String additional) {
        Assert.assertEquals("Test propterties should be set!", getProperties().get(TEST_PROPERTY), TEST_PROPERTY_VALUE);
    }

    @Override
    public boolean hasIdentifier(MCRBase obj, String additional) throws MCRPersistentIdentifierException {
        Assert.assertEquals("Test propterties should be set!", getProperties().get(TEST_PROPERTY), TEST_PROPERTY_VALUE);
        return false;
    }
}
