package org.mycore.pi.urn.rest;

import static org.mycore.pi.MCRPIUtils.generateMCRPI;
import static org.mycore.pi.MCRPIUtils.randomFilename;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.MCRPIUtils;
import org.mycore.pi.MCRPersistentIdentifierManager;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.urn.MCRDNBURN;

/**
 * Created by chi on 23.02.17.
 *
 * @author Huu Chi Vu
 */
public class MCRURNGranularRESTRegistrationTaskTest extends MCRStoreTestCase {
    private static final String countRegistered = "select count(u) from MCRPI u "
        + "where u.type = :type "
        + "and u.registered is not null";

    public static final int BATCH_SIZE = 20;

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Ignore
    @Test
    public void run() throws Exception {
        MCRPI urn1 = generateMCRPI(randomFilename(), countRegistered);
        MCREntityManagerProvider.getCurrentEntityManager()
            .persist(urn1);

        Assert.assertNull("Registered date should be null.", urn1.getRegistered());

        MCRPersistentIdentifierManager.getInstance()
            .getUnregisteredIdentifiers(urn1.getType())
            .stream()
            .map(MCRPIRegistrationInfo::getIdentifier)
            .map("URN: "::concat)
            .forEach(LOGGER::info);

        Integer progressedIdentifiersFromDatabase;
        Function<MCRPIRegistrationInfo, Optional<Date>> registerFn = MCRPIUtils.getMCRURNClient()::register;
        do {
            progressedIdentifiersFromDatabase = MCRPersistentIdentifierManager.getInstance()
                .setRegisteredDateForUnregisteredIdenifiers(MCRDNBURN.TYPE, registerFn, BATCH_SIZE);
        } while (progressedIdentifiersFromDatabase > 0);

        boolean registered = MCRPersistentIdentifierManager.getInstance().isRegistered(urn1);
        LOGGER.info("Registered: " + registered);

        MCRPI mcrpi = MCREntityManagerProvider.getCurrentEntityManager().find(MCRPI.class, urn1.getId());
        Optional.ofNullable(mcrpi)
            .filter(pi -> pi.getRegistered() != null)
            .map(MCRPI::getRegistered)
            .map(Date::toString)
            .map("URN registered: "::concat)
            .ifPresent(LOGGER::info);

        MCRPersistentIdentifierManager.getInstance().getUnregisteredIdentifiers(urn1.getType())
            .stream()
            .map(MCRPIRegistrationInfo::getIdentifier)
            .map("URN update: "::concat)
            .forEach(LOGGER::info);

        LOGGER.info("end.");
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());
        testProperties.put("MCR.PI.Generator.testGenerator.Namespace", "frontend-");
        return testProperties;
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
}
