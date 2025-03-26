/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.processing.impl.MCRCentralProcessableRegistry;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.backend.MCRPI;

public class MCRPIManagerTest extends MCRStoreTestCase {

    private static final String MOCK_SERVICE = "MockService";

    private static final String MOCK_METADATA_SERVICE = "MockInscriber";

    private static final String MOCK_PID_GENERATOR = "MockIDGenerator";

    private MCRPIManager manager;

    @Rule
    public TemporaryFolder baseDir = new TemporaryFolder();

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        Map<String, MCRPIParser<? extends MCRPersistentIdentifier>> parsers = new HashMap<>();
        parsers.put(MCRMockIdentifierService.TYPE, new MCRMockIdentifierParser());

        List<MCRPIResolver<? extends MCRPersistentIdentifier>> resolvers = new ArrayList<>();
        resolvers.add(new MCRMockResolver());

        manager = new MCRPIManager(parsers, resolvers);

        MCRPIService<MCRMockIdentifier> registrationService = MCRPIServiceManager
            .getInstance().getRegistrationService(MOCK_SERVICE);
        ((MCRMockIdentifierService) registrationService).reset();

    }

    @Test
    public void testGetRegisteredPI() throws Exception {

        MCRPIService<MCRMockIdentifier> registrationService = MCRPIServiceManager
            .getInstance().getRegistrationService(MOCK_SERVICE);

        MCRObject mcrObject = buildMockObject();
        registrationService.register(mcrObject, null);

        MCRPI mcrpi = manager.get(MOCK_SERVICE, mcrObject.getId().toString(), null);
        Assert.assertNotNull(mcrpi);

    }

    @Test
    public void testParseIdentifier() {

        String mockString = MCRMockIdentifier.MOCK_SCHEME + "foobar";

        Stream<MCRPersistentIdentifier> mcrPersistentIdentifierStream = manager.get(mockString);
        Optional<MCRPersistentIdentifier> mockIdentifierOptional = mcrPersistentIdentifierStream.findFirst();

        Assert.assertTrue(mockIdentifierOptional.isPresent());
        Assert.assertEquals(mockString, mockIdentifierOptional.get().asString());

    }

    @Test
    public void testRegistrationService() throws Exception {

        MCRPIService<MCRMockIdentifier> registrationService = MCRPIServiceManager
            .getInstance().getRegistrationService(MOCK_SERVICE);

        MCRMockIdentifierService casted = (MCRMockIdentifierService) registrationService;

        Assert.assertFalse("Delete should not have been called!", casted.isDeleteCalled());
        Assert.assertFalse("Register should not have been called!", casted.isRegisterCalled());
        Assert.assertFalse("Update should not have been called!", casted.isUpdatedCalled());

        MCRObject mcrObject = buildMockObject();
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

        Assert.assertTrue("There should be one resolver", manager
            .getResolvers().stream().anyMatch(r -> r.getName().equals(MCRMockResolver.NAME)));

    }

    @Test
    public void testGetUnregisteredIdentifiers() throws Exception {

        MCREntityManagerProvider.getCurrentEntityManager().persist(generateMCRPI());
        MCREntityManagerProvider.getCurrentEntityManager().persist(generateMCRPI());

        long numOfUnregisteredPI = manager.getUnregisteredIdentifiers("Unregistered").size();

        Assert.assertEquals("Wrong number of unregistered PI: ", 2, numOfUnregisteredPI);

    }

    private MCRPI generateMCRPI() {
        MCRObjectID mycoreID = MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId("test_unregisterd");
        return new MCRPI(generatePIFor(mycoreID).asString(), "Unregistered",
            mycoreID.toString(), null, MOCK_SERVICE, new MCRPIServiceDates(null, null));
    }

    private MCRMockIdentifier generatePIFor(MCRObjectID mycoreID) {
        MCRMockIdentifierGenerator mcrUuidUrnGenerator = MCRConfiguration2.getInstanceOfOrThrow(
            MCRMockIdentifierGenerator.class, "MCR.PI.Generator." + MOCK_PID_GENERATOR);
        MCRObject mcrObject = new MCRObject();
        mcrObject.setId(mycoreID);
        return mcrUuidUrnGenerator.generate(mcrObject, "");
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> configuration = super.getTestProperties();

        configuration.put("MCR.Access.Class", MCRAccessBaseImpl.class.getName());
        configuration.put("MCR.Metadata.Type.mock", "true");
        configuration.put("MCR.Metadata.Type.unregisterd", "true");

        configuration.put("MCR.PI.Service." + MOCK_SERVICE, MCRMockIdentifierService.class.getName());
        configuration.put("MCR.PI.Service." + MOCK_SERVICE + ".Generator", MOCK_PID_GENERATOR);
        configuration.put("MCR.PI.Service." + MOCK_SERVICE + ".MetadataService", MOCK_METADATA_SERVICE);

        configuration.put("MCR.PI.MetadataService." + MOCK_METADATA_SERVICE, MCRMockMetadataService.class.getName());
        configuration.put(
            "MCR.PI.MetadataService." + MOCK_METADATA_SERVICE + "." + MCRMockMetadataService.TEST_PROPERTY,
            MCRMockMetadataService.TEST_PROPERTY_VALUE);

        configuration.put("MCR.PI.Generator." + MOCK_PID_GENERATOR, MCRMockIdentifierGenerator.class.getName());
        configuration.put("MCR.PI.Generator." + MOCK_PID_GENERATOR + "." + MCRMockIdentifierGenerator.TEST_PROPERTY,
            MCRMockIdentifierGenerator.TEST_PROPERTY_VALUE);
        configuration.put("MCR.PI.Generator." + MOCK_PID_GENERATOR + ".Namespace", "frontend-");

        configuration.put("MCR.QueuedJob.activated", "true");
        configuration.put("MCR.QueuedJob.JobThreads", "2");
        configuration.put("MCR.QueuedJob.TimeTillReset", "10");
        configuration.put("MCR.Processable.Registry.Class", MCRCentralProcessableRegistry.class.getName());
        configuration.put("MCR.Access.Cache.Size", "1000");

        return configuration;
    }

    private MCRObject buildMockObject() {
        MCRObject mcrObject = new MCRObject();
        MCRObjectID id = MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId("test", "mock");
        mcrObject.setId(id);
        mcrObject.setSchema("http://www.w3.org/2001/XMLSchema");
        return mcrObject;
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
}
