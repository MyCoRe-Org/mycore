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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.access.MCRAccessException;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.processing.impl.MCRCentralProcessableRegistry;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRPIManagerTest extends MCRStoreTestCase {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String MOCK_SERVICE = "MockService";

    private static final String MOCK_METADATA_SERVICE = "MockInscriber";

    private static final String MOCK_PID_GENERATOR = "MockIDGenerator";

    @Rule
    public TemporaryFolder baseDir = new TemporaryFolder();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        resetManagerInstance();
    }

    @Test
    public void testGet() throws Exception {
        String mockString = MCRMockIdentifier.MOCK_SCHEME + "http://google.de/";

        Optional<? extends MCRPersistentIdentifier> mockIdentifierOptional = MCRPIManager
            .getInstance()
            .get(mockString)
            .findFirst();

        Assert.assertTrue(mockIdentifierOptional.isPresent());
        Assert.assertEquals(mockIdentifierOptional.get().asString(), mockString);

        // test get(service, id, additional)
        MCRPIService<MCRMockIdentifier> registrationService = MCRPIServiceManager
            .getInstance()
            .getRegistrationService(MOCK_SERVICE);

        ((MCRMockIdentifierService) registrationService).reset();

        MCRObject mcrObject = buildMockObject();
        registrationService.register(mcrObject, null);

        MCRPI mcrpi = MCRPIManager.getInstance().get(MOCK_SERVICE, mcrObject.getId().toString(), null);
        Assert.assertNotNull(mcrpi);
    }

    @Test
    public void testParseIdentifier() {
        String mockString = MCRMockIdentifier.MOCK_SCHEME + "http://google.de/";
        Stream<MCRPersistentIdentifier> mcrPersistentIdentifierStream = MCRPIManager
            .getInstance()
            .get(mockString);

        Optional<? extends MCRPersistentIdentifier> mcrMockIdentifier = mcrPersistentIdentifierStream
            .findFirst();
        Assert.assertEquals(mcrMockIdentifier.get().asString(), mockString);
    }

    @Test
    public void testRegistrationService()
        throws MCRAccessException, MCRPersistentIdentifierException, ExecutionException,
        InterruptedException {

        MCRPIService<MCRMockIdentifier> registrationService = MCRPIServiceManager
            .getInstance()
            .getRegistrationService(MOCK_SERVICE);

        MCRMockIdentifierService casted = (MCRMockIdentifierService) registrationService;
        casted.reset();

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

        Assert.assertTrue("There should be one resolver", MCRPIManager.getInstance()
            .getResolvers().stream().anyMatch(r -> r.getName().equals(MCRMockResolver.NAME)));
    }

    @Test
    public void testGetUnregisteredIdenifiers() throws Exception {
        MCREntityManagerProvider.getCurrentEntityManager().persist(generateMCRPI());
        MCREntityManagerProvider.getCurrentEntityManager().persist(generateMCRPI());

        long numOfUnregisteredPI = MCRPIManager.getInstance()
            .getUnregisteredIdentifiers("Unregistered")
            .size();

        Assert.assertEquals("Wrong number of unregistered PI: ", 2, numOfUnregisteredPI);
    }

    private MCRPI generateMCRPI() throws MCRPersistentIdentifierException {
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

    @AfterClass
    public static void resetManagerInstance() {
        MCRPIManager.getInstance().applyConfiguration();
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> configuration = super.getTestProperties();

        configuration.put("MCR.Access.Class", MCRAccessBaseImpl.class.getName());
        configuration.put("MCR.Metadata.Type.mock", "true");
        configuration.put("MCR.Metadata.Type.unregisterd", "true");

        configuration.put("MCR.PI.Resolvers", MCRMockResolver.class.getName());

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

        configuration.put("MCR.PI.Parsers." + MCRMockIdentifierService.TYPE,
            MCRMockIdentifierParser.class.getName());

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
