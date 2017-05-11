package org.mycore.pi.urn.rest;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.MCRPIUtils;
import org.mycore.pi.MCRPersistentIdentifierManager;
import org.mycore.pi.backend.MCRPI;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.mycore.pi.MCRPIUtils.generateMCRPI;
import static org.mycore.pi.MCRPIUtils.randomFilename;

/**
 * Created by chi on 23.02.17.
 *
 * @author Huu Chi Vu
 */
public class MCRURNGranularRESTRegistrationTaskTest extends MCRStoreTestCase {
    private static final String countRegistered = "select count(u) from MCRPI u "
            + "where u.type = :type "
            + "and u.registered is not null";

    @Test
    public void run() throws Exception {
        MCRPI urn1 = generateMCRPI(randomFilename(), countRegistered);
        MCREntityManagerProvider.getCurrentEntityManager()
                                .persist(urn1);

        Assert.assertNull("Registered date should be null.", urn1.getRegistered());

        MCRPersistentIdentifierManager.getInstance()
                                      .getUnregisteredIdenifiers(urn1.getType())
                                      .stream()
                                      .map(MCRPIRegistrationInfo::getIdentifier)
                                      .map("URN: "::concat)
                                      .forEach(System.out::println);

        MCRURNGranularRESTRegistrationTask registrationTask = new MCRURNGranularRESTRegistrationTask(
                MCRPIUtils.getMCRURNClient());

        registrationTask.run();

        boolean registered = MCRPersistentIdentifierManager.getInstance().isRegistered(urn1);
        System.out.println("Registered: " + registered);

        MCRPI mcrpi = MCREntityManagerProvider.getCurrentEntityManager().find(MCRPI.class, urn1.getId());
        Optional.ofNullable(mcrpi)
                .filter(pi -> pi.getRegistered() != null)
                .map(MCRPI::getRegistered)
                .map(Date::toString)
                .map("URN registered: "::concat)
                .ifPresent(System.out::println);

        MCRPersistentIdentifierManager.getInstance().getUnregisteredIdenifiers(urn1.getType())
                                      .stream()
                                      .map(MCRPIRegistrationInfo::getIdentifier)
                                      .map("URN update: "::concat)
                                      .forEach(System.out::println);

        System.out.println("end.");
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());
        testProperties.put("MCR.PI.Generator.testGenerator.Namespace", "frontend-");
        return testProperties;
    }
}