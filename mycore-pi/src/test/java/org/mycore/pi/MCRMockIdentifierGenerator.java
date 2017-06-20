package org.mycore.pi;

import org.junit.Assert;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRMockIdentifierGenerator extends MCRPersistentIdentifierGenerator<MCRMockIdentifier> {

    public static final String TEST_PROPERTY = "mockProperty";

    public static final String TEST_PROPERTY_VALUE = "mockPropertyValue";

    public MCRMockIdentifierGenerator(String generatorID) {
        super(generatorID);
    }

    @Override
    public MCRMockIdentifier generate(MCRObjectID mcrID, String additional) throws MCRPersistentIdentifierException {
        Assert.assertEquals("Test propterties should be set!", getProperties().get(TEST_PROPERTY), TEST_PROPERTY_VALUE);

        return (MCRMockIdentifier) new MCRMockIdentifierParser()
            .parse(MCRMockIdentifier.MOCK_SCHEME + mcrID.toString() + ":" + additional).get();
    }
}
