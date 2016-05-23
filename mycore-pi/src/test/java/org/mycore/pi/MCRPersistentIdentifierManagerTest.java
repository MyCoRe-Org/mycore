package org.mycore.pi;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRJPATestCase;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRPersistentIdentifierManagerTest extends MCRJPATestCase {

    private static final String MOCK_SERVICE = "MockService";
    private MCRPersistentIdentifierManager managerInstance;
    private static final String MOCK_INSCRIBER = "MockInscriber";
    private static final String MOCK_PID_GENERATOR = "MockIDGenerator";

    @Test
    public void testGet() {
        String mockString = MCRMockIdentifier.MOCK_SCHEME + "http://google.de/";
        Optional<MCRPersistentIdentifier> mockIdentifierOptional = managerInstance.get(mockString).findAny();

        Assert.assertTrue(mockIdentifierOptional.isPresent());
        Assert.assertEquals(mockIdentifierOptional.get().asString(), mockString);
    }

    @Test
    public void testParseIdentifier() {
        String mockString = MCRMockIdentifier.MOCK_SCHEME + "http://google.de/";
        Optional<MCRPersistentIdentifier> mcrMockIdentifier = managerInstance.get(mockString).findFirst();
        Assert.assertEquals(mcrMockIdentifier.get().asString(), mockString);
    }

    @Test
    public void testRegistrationService() throws MCRAccessException, MCRActiveLinkException, MCRPersistentIdentifierException {
        MCRPIRegistrationService<MCRMockIdentifier> registrationService = MCRPIRegistrationServiceManager.getInstance().getRegistrationService(MOCK_SERVICE);


        MCRObject mcrObject = new MCRObject();
        MCRObjectID id = MCRObjectID.getNextFreeId("test", "mock");
        mcrObject.setId(id);
        mcrObject.setSchema("http://www.w3.org/2001/XMLSchema");
        MCRMetadataManager.create(mcrObject);

        MCRMockIdentifierRegistrationService casted = (MCRMockIdentifierRegistrationService)registrationService;

        Assert.assertFalse("Delete should not have been called!", casted.isDeleteCalled());
        Assert.assertFalse("Register should not have been called!", casted.isRegisterCalled());
        Assert.assertFalse("Update should not have been called!", casted.isUpdatedCalled());

        MCRMockIdentifier identifier = (MCRMockIdentifier) registrationService.fullRegister(mcrObject, "");

        Assert.assertFalse("Delete should not have been called!", casted.isDeleteCalled());
        Assert.assertTrue("The identifier " + identifier.asString() + " should be registered now!", registrationService.isRegistered(id, ""));

        registrationService.onUpdate(identifier, mcrObject, "");
        Assert.assertFalse("Delete should not have been called!", casted.isDeleteCalled());
        Assert.assertTrue("The identifier " + identifier.asString() + " should have been updated!", casted.isUpdatedCalled());

        registrationService.onDelete(identifier, mcrObject, "");
        Assert.assertFalse("The identifier " + identifier.asString() + " should not be registered now!", registrationService.isRegistered(id, ""));


    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        managerInstance = MCRPersistentIdentifierManager.getInstance();

    }

    @Override
    protected Map<String, String> getTestProperties() {
        HashMap<String, String> configuration = new HashMap<>();

        configuration.put("MCR.Access.Class", MCRAccessBaseImpl.class.getName());
        configuration.put("MCR.Metadata.Type.mock", "true");
        configuration.put("MCR.PI.Registration." + MOCK_SERVICE, MCRMockIdentifierRegistrationService.class.getName());
        configuration.put("MCR.PI.Registration." + MOCK_SERVICE + ".Generator", MOCK_PID_GENERATOR);
        configuration.put("MCR.PI.Registration." + MOCK_SERVICE + ".Inscriber", MOCK_INSCRIBER);
        configuration.put("MCR.PI.Inscriber." + MOCK_INSCRIBER, MCRMockInscriber.class.getName());
        configuration.put("MCR.PI.Generator." + MOCK_PID_GENERATOR, MCRMockIdentifierGenerator.class.getName());
        configuration.put("MCR.PI.Parsers." + MCRMockIdentifierRegistrationService.TYPE, MCRMockIdentifierParser.class.getName());

        return configuration;
    }
}