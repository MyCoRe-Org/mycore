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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.access.MCRAccessException;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.processing.impl.MCRCentralProcessableRegistry;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@ExtendWith(MCRMetadataExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Access.Class", classNameOf = MCRAccessBaseImpl.class),
    @MCRTestProperty(key = "MCR.Metadata.Type.mock", string = "true"),
    @MCRTestProperty(key = "MCR.Metadata.Type.unregisterd", string = "true"),
    @MCRTestProperty(key = "MCR.PI.Resolvers", classNameOf = MCRMockResolver.class),
    @MCRTestProperty(key = "MCR.PI.Service." + MCRPIManagerTest.MOCK_SERVICE,
        classNameOf = MCRMockIdentifierService.class),
    @MCRTestProperty(key = "MCR.PI.Service." + MCRPIManagerTest.MOCK_SERVICE + ".Generator",
        string = MCRPIManagerTest.MOCK_PID_GENERATOR),
    @MCRTestProperty(key = "MCR.PI.Service." + MCRPIManagerTest.MOCK_SERVICE + ".MetadataService",
        string = MCRPIManagerTest.MOCK_METADATA_SERVICE),
    @MCRTestProperty(key = "MCR.PI.MetadataService." + MCRPIManagerTest.MOCK_METADATA_SERVICE,
        classNameOf = MCRMockMetadataService.class),
    @MCRTestProperty(key = "MCR.PI.MetadataService." + MCRPIManagerTest.MOCK_METADATA_SERVICE + "."
        + MCRMockMetadataService.TEST_PROPERTY, string = MCRMockMetadataService.TEST_PROPERTY_VALUE),
    @MCRTestProperty(key = "MCR.PI.Generator." + MCRPIManagerTest.MOCK_PID_GENERATOR,
        classNameOf = MCRMockIdentifierGenerator.class),
    @MCRTestProperty(key = "MCR.PI.Generator." + MCRPIManagerTest.MOCK_PID_GENERATOR + "."
        + MCRMockIdentifierGenerator.TEST_PROPERTY, string = MCRMockIdentifierGenerator.TEST_PROPERTY_VALUE),
    @MCRTestProperty(key = "MCR.PI.Generator." + MCRPIManagerTest.MOCK_PID_GENERATOR + ".Namespace",
        string = "frontend-"),
    @MCRTestProperty(key = "MCR.PI.Parsers." + MCRMockIdentifierService.TYPE,
        classNameOf = MCRMockIdentifierParser.class),
    @MCRTestProperty(key = "MCR.QueuedJob.activated", string = "true"),
    @MCRTestProperty(key = "MCR.QueuedJob.JobThreads", string = "2"),
    @MCRTestProperty(key = "MCR.QueuedJob.TimeTillReset", string = "10"),
    @MCRTestProperty(key = "MCR.Processable.Registry.Class", classNameOf = MCRCentralProcessableRegistry.class),
    @MCRTestProperty(key = "MCR.Access.Cache.Size", string = "1000")
})
public class MCRPIManagerTest {

    public static final String MOCK_SERVICE = "MockService";

    public static final String MOCK_METADATA_SERVICE = "MockInscriber";

    public static final String MOCK_PID_GENERATOR = "MockIDGenerator";

    @BeforeEach
    public void setUp() {
        resetManagerInstance();
    }

    @Test
    public void testGet() throws Exception {
        String mockString = MCRMockIdentifier.MOCK_SCHEME + "http://google.de/";

        Optional<? extends MCRPersistentIdentifier> mockIdentifierOptional = MCRPIManager
            .getInstance()
            .get(mockString)
            .findFirst();

        assertTrue(mockIdentifierOptional.isPresent());
        assertEquals(mockString, mockIdentifierOptional.get().asString());

        // test get(service, id, additional)
        MCRPIService<MCRMockIdentifier> registrationService = MCRPIServiceManager
            .getInstance()
            .getRegistrationService(MOCK_SERVICE);

        ((MCRMockIdentifierService) registrationService).reset();

        MCRObject mcrObject = buildMockObject();
        registrationService.register(mcrObject, null);

        MCRPI mcrpi = MCRPIManager.getInstance().get(MOCK_SERVICE, mcrObject.getId().toString(), null);
        assertNotNull(mcrpi);
    }

    @Test
    public void testParseIdentifier() {
        String mockString = MCRMockIdentifier.MOCK_SCHEME + "http://google.de/";
        Stream<MCRPersistentIdentifier> mcrPersistentIdentifierStream = MCRPIManager
            .getInstance()
            .get(mockString);

        Optional<? extends MCRPersistentIdentifier> mcrMockIdentifier = mcrPersistentIdentifierStream
            .findFirst();
        assertEquals(mockString, mcrMockIdentifier.get().asString());
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

        assertFalse(casted.isDeleteCalled(), "Delete should not have been called!");
        assertFalse(casted.isRegisterCalled(), "Register should not have been called!");
        assertFalse(casted.isUpdatedCalled(), "Update should not have been called!");

        MCRObject mcrObject = buildMockObject();
        MCRMockIdentifier identifier = registrationService.register(mcrObject, "", true);

        assertFalse(casted.isDeleteCalled(), "Delete should not have been called!");
        assertTrue(registrationService.isCreated(mcrObject.getId(), ""), "The identifier " + identifier.asString() + " should be registered now!");

        registrationService.onUpdate(identifier, mcrObject, "");
        assertFalse(casted.isDeleteCalled(), "Delete should not have been called!");
        assertTrue(casted.isUpdatedCalled(), "The identifier " + identifier.asString() + " should have been updated!");

        registrationService.onDelete(identifier, mcrObject, "");
        assertFalse(registrationService.isCreated(mcrObject.getId(), ""), "The identifier " + identifier.asString() + " should not be registered now!");

        assertTrue(MCRPIManager.getInstance()
            .getResolvers().stream().anyMatch(r -> r.getName().equals(MCRMockResolver.NAME)), "There should be one resolver");
    }

    @Test
    public void testGetUnregisteredIdenifiers() throws Exception {
        MCREntityManagerProvider.getCurrentEntityManager().persist(generateMCRPI());
        MCREntityManagerProvider.getCurrentEntityManager().persist(generateMCRPI());

        long numOfUnregisteredPI = MCRPIManager.getInstance()
            .getUnregisteredIdentifiers("Unregistered")
            .size();

        assertEquals(2, numOfUnregisteredPI, "Wrong number of unregistered PI: ");
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

    @AfterAll
    public static void resetManagerInstance() {
        MCRPIManager.getInstance().applyConfiguration();
    }

    

    private MCRObject buildMockObject() {
        MCRObject mcrObject = new MCRObject();
        MCRObjectID id = MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId("test", "mock");
        mcrObject.setId(id);
        mcrObject.setSchema("http://www.w3.org/2001/XMLSchema");
        return mcrObject;
    }

}
