package org.mycore.pi;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.access.MCRAccessException;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRJPATestCase;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRPersistentIdentifierManagerTest extends MCRJPATestCase {

    private static final String MOCK_SERVICE = "MockService";

    private static final String MOCK_INSCRIBER = "MockInscriber";

    private static final String MOCK_PID_GENERATOR = "MockIDGenerator";

    @Rule
    public TemporaryFolder baseDir = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testGet() {
        String mockString = MCRMockIdentifier.MOCK_SCHEME + "http://google.de/";

        Optional<? extends MCRPersistentIdentifier> mockIdentifierOptional = MCRPersistentIdentifierManager
            .getInstance()
            .get(mockString)
            .findFirst();

        Assert.assertTrue(mockIdentifierOptional.isPresent());
        Assert.assertEquals(mockIdentifierOptional.get().asString(), mockString);
    }

    @Test
    public void testParseIdentifier() {
        String mockString = MCRMockIdentifier.MOCK_SCHEME + "http://google.de/";
        Stream<MCRPersistentIdentifier> mcrPersistentIdentifierStream = MCRPersistentIdentifierManager
            .getInstance()
            .get(mockString);

        Optional<? extends MCRPersistentIdentifier> mcrMockIdentifier = mcrPersistentIdentifierStream
            .findFirst();
        Assert.assertEquals(mcrMockIdentifier.get().asString(), mockString);
    }

    @Test
    public void testRegistrationService()
        throws MCRAccessException, MCRActiveLinkException, MCRPersistentIdentifierException {
        MockMetadataManager mockMetadataManager = new MockMetadataManager();

        MCRPIRegistrationService<MCRMockIdentifier> registrationService = MCRPIRegistrationServiceManager
            .getInstance()
            .getRegistrationService(MOCK_SERVICE);

        MCRObject mcrObject = new MCRObject();
        MCRObjectID id = MCRObjectID.getNextFreeId("test", "mock");
        mcrObject.setId(id);
        mcrObject.setSchema("http://www.w3.org/2001/XMLSchema");
        mockMetadataManager.put(id, mcrObject);

        MCRMockIdentifierRegistrationService casted = (MCRMockIdentifierRegistrationService) registrationService;

        Assert.assertFalse("Delete should not have been called!", casted.isDeleteCalled());
        Assert.assertFalse("Register should not have been called!", casted.isRegisterCalled());
        Assert.assertFalse("Update should not have been called!", casted.isUpdatedCalled());

        MCRMockIdentifier identifier = registrationService.register(mcrObject, "", true);

        Assert.assertFalse("Delete should not have been called!", casted.isDeleteCalled());
        Assert.assertTrue("The identifier " + identifier.asString() + " should be registered now!",
            registrationService.isCreated(id, ""));

        registrationService.onUpdate(identifier, mcrObject, "");
        Assert.assertFalse("Delete should not have been called!", casted.isDeleteCalled());
        Assert.assertTrue("The identifier " + identifier.asString() + " should have been updated!",
            casted.isUpdatedCalled());

        registrationService.onDelete(identifier, mcrObject, "");
        Assert.assertFalse("The identifier " + identifier.asString() + " should not be registered now!",
            registrationService.isCreated(id, ""));

        Assert.assertTrue("There should be one resolver", MCRPersistentIdentifierManager.getInstance()
            .getResolvers().stream()
            .filter(r -> {
                return r.getName()
                    .equals(MCRMockResolver.NAME);
            }).count() > 0);

    }

    @Test
    public void testGetUnregisteredIdenifiers() throws Exception {
        MCRHIBConnection.instance().getSession().save(generateMCRPI());
        MCRHIBConnection.instance().getSession().save(generateMCRPI());

        long numOfUnregisteredPI = MCRPersistentIdentifierManager
            .getInstance()
            .getUnregisteredIdentifiers("Unregistered")
            .stream()
            .count();

        Assert.assertEquals("Wrong number of unregistered PI: ", 2, numOfUnregisteredPI);
    }

    private MCRPI generateMCRPI() throws MCRPersistentIdentifierException {
        MCRObjectID mycoreID = MCRObjectID.getNextFreeId("test_unregisterd");
        return new MCRPI(generatePIFor(mycoreID).asString(), "Unregistered",
            mycoreID.toString(), null);
    }

    private MCRMockIdentifier generatePIFor(MCRObjectID mycoreID) throws MCRPersistentIdentifierException {
        MCRMockIdentifierGenerator mcruuidurnGenerator = new MCRMockIdentifierGenerator(MOCK_PID_GENERATOR);
        return mcruuidurnGenerator.generate(mycoreID, "");
    }

    @Before
    public void resetManagerInstance() {
        try {
            Field instance = MCRPersistentIdentifierManager.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> configuration = super.getTestProperties();

        configuration.put("MCR.Metadata.Store.BaseDir", baseDir.getRoot().getAbsolutePath());
        try {
            configuration.put("MCR.Metadata.Store.SVNBase", baseDir.newFolder("versions").toURI().toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        configuration.put("MCR.Access.Class", MCRAccessBaseImpl.class.getName());
        configuration.put("MCR.Metadata.Type.mock", "true");
        configuration.put("MCR.Metadata.Type.unregisterd", "true");

        configuration.put("MCR.PI.Resolvers", MCRMockResolver.class.getName());

        configuration.put("MCR.PI.Registration." + MOCK_SERVICE, MCRMockIdentifierRegistrationService.class.getName());
        configuration.put("MCR.PI.Registration." + MOCK_SERVICE + ".Generator", MOCK_PID_GENERATOR);
        configuration.put("MCR.PI.Registration." + MOCK_SERVICE + ".Inscriber", MOCK_INSCRIBER);

        configuration.put("MCR.PI.Inscriber." + MOCK_INSCRIBER, MCRMockMetadataManager.class.getName());
        configuration.put("MCR.PI.Inscriber." + MOCK_INSCRIBER + "." + MCRMockMetadataManager.TEST_PROPERTY,
            MCRMockMetadataManager.TEST_PROPERTY_VALUE);

        configuration.put("MCR.PI.Generator." + MOCK_PID_GENERATOR, MCRMockIdentifierGenerator.class.getName());
        configuration.put("MCR.PI.Generator." + MOCK_PID_GENERATOR + "." + MCRMockIdentifierGenerator.TEST_PROPERTY,
            MCRMockIdentifierGenerator.TEST_PROPERTY_VALUE);
        configuration.put("MCR.PI.Generator." + MOCK_PID_GENERATOR + ".Namespace", "frontend-");

        configuration.put("MCR.PI.Parsers." + MCRMockIdentifierRegistrationService.TYPE,
            MCRMockIdentifierParser.class.getName());

        return configuration;
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
}
