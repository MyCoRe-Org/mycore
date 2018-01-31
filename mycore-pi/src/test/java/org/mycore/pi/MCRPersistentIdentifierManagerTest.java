/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.pi;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.access.MCRAccessException;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRPersistentIdentifierManagerTest extends MCRStoreTestCase {

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
    public void testGet() throws Exception {
        String mockString = MCRMockIdentifier.MOCK_SCHEME + "http://google.de/";

        Optional<? extends MCRPersistentIdentifier> mockIdentifierOptional = MCRPersistentIdentifierManager
            .getInstance()
            .get(mockString)
            .findFirst();

        Assert.assertTrue(mockIdentifierOptional.isPresent());
        Assert.assertEquals(mockIdentifierOptional.get().asString(), mockString);

        // test get(service, id, additional)
        MockMetadataManager mockMetadataManager = new MockMetadataManager();
        MCRPIRegistrationService<MCRMockIdentifier> registrationService = MCRPIRegistrationServiceManager
                .getInstance()
                .getRegistrationService(MOCK_SERVICE);

        MCRObject mcrObject = buildMockObject();
        mockMetadataManager.put(mcrObject.getId(), mcrObject);
        registrationService.register(mcrObject, null);

        MCRPI mcrpi = MCRPersistentIdentifierManager.getInstance().get(MOCK_SERVICE, mcrObject.getId().toString(), null);
        Assert.assertNotNull(mcrpi);
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

        MCRMockIdentifierRegistrationService casted = (MCRMockIdentifierRegistrationService) registrationService;

        Assert.assertFalse("Delete should not have been called!", casted.isDeleteCalled());
        Assert.assertFalse("Register should not have been called!", casted.isRegisterCalled());
        Assert.assertFalse("Update should not have been called!", casted.isUpdatedCalled());

        MCRObject mcrObject = buildMockObject();
        mockMetadataManager.put(mcrObject.getId(), mcrObject);
        MCRMockIdentifier identifier = registrationService.register(mcrObject, "", true);

        Assert.assertFalse("Delete should not have been called!", casted.isDeleteCalled());
        Assert.assertTrue("The identifier " + identifier.asString() + " should be registered now!",
            registrationService.isCreated(mcrObject.getId(), ""));

        registrationService.onUpdate(identifier, mcrObject, "");
        Assert.assertFalse("Delete should not have been called!", casted.isDeleteCalled());
        Assert.assertTrue("The identifier " + identifier.asString() + " should have been updated!",
            casted.isUpdatedCalled());

        registrationService.onDelete(identifier, mcrObject, "");
        Assert.assertFalse("The identifier " + identifier.asString() + " should not be registered now!",
            registrationService.isCreated(mcrObject.getId(), ""));

        Assert.assertTrue("There should be one resolver", MCRPersistentIdentifierManager.getInstance()
            .getResolvers().stream()
            .filter(r -> r.getName()
                .equals(MCRMockResolver.NAME))
            .count() > 0);
    }

    @Test
    public void testGetUnregisteredIdenifiers() throws Exception {
        MCRHIBConnection.instance().getSession().save(generateMCRPI());
        MCRHIBConnection.instance().getSession().save(generateMCRPI());

        long numOfUnregisteredPI = (long) MCRPersistentIdentifierManager.getInstance()
            .getUnregisteredIdentifiers("Unregistered")
            .size();

        Assert.assertEquals("Wrong number of unregistered PI: ", 2, numOfUnregisteredPI);
    }

    private MCRPI generateMCRPI() throws MCRPersistentIdentifierException {
        MCRObjectID mycoreID = MCRObjectID.getNextFreeId("test_unregisterd");
        return new MCRPI(generatePIFor(mycoreID).asString(), "Unregistered",
            mycoreID.toString(), null, MOCK_SERVICE, null);
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
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> configuration = super.getTestProperties();

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

    private MCRObject buildMockObject() {
        MCRObject mcrObject = new MCRObject();
        MCRObjectID id = MCRObjectID.getNextFreeId("test", "mock");
        mcrObject.setId(id);
        mcrObject.setSchema("http://www.w3.org/2001/XMLSchema");
        return mcrObject;
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
}
