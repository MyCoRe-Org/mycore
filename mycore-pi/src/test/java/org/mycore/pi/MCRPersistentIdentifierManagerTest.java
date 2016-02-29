package org.mycore.pi;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MCRPersistentIdentifierManagerTest {

    private MCRPersistentIdentifierManager managerInstance;

    @Test
    public void testGet() {
        String mockString = MCRMockIdentifierParser.MOCK_SCHEME + "http://google.de/";
        Optional<MCRPersistentIdentifier> mockIdentifierOptional = managerInstance.get(mockString).findAny();

        Assert.assertTrue(mockIdentifierOptional.isPresent());
        Assert.assertEquals(mockIdentifierOptional.get().asString(), mockString);
    }

    @Test
    public void testParseIdentifier() {
        String mockString = MCRMockIdentifierParser.MOCK_SCHEME + "http://google.de/";
        Optional<MCRPersistentIdentifier> mcrMockIdentifier = managerInstance.get(mockString).findFirst();
        Assert.assertEquals(mcrMockIdentifier.get().asString(), mockString);
    }


    @Before
    public void getMcrPersistentIdentifierManager() {
        managerInstance = MCRPersistentIdentifierManager.getInstance();
        managerInstance.registerParser("mock", MCRMockIdentifierParser.class);
    }

}