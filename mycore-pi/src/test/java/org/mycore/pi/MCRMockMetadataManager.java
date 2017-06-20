package org.mycore.pi;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRMockMetadataManager extends MCRPersistentIdentifierMetadataManager<MCRMockIdentifier> {

    public static final String TEST_PROPERTY = "mockProperty";

    public static final String TEST_PROPERTY_VALUE = "mockPropertyValue";

    private Map<String, MCRMockIdentifier> map = new HashMap<>();

    public MCRMockMetadataManager(String inscriberID) {
        super(inscriberID);
    }

    @Override
    public void insertIdentifier(MCRMockIdentifier identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        Assert.assertEquals("Test propterties should be set!", getProperties().get(TEST_PROPERTY), TEST_PROPERTY_VALUE);
        map.put(obj.toString() + additional, identifier);
    }

    @Override
    public void removeIdentifier(MCRMockIdentifier identifier, MCRBase obj, String additional) {
        Assert.assertEquals("Test properties should be set!", getProperties().get(TEST_PROPERTY), TEST_PROPERTY_VALUE);
        map.remove(obj.toString() + additional);
    }

    @Override
    public Optional<MCRPersistentIdentifier> getIdentifier(MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        return Optional.ofNullable(map.get(obj.toString() + additional));
    }

}
