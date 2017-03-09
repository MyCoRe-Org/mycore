package org.mycore.pi.urn.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.MCRPIUtils;
import org.mycore.pi.MCRPersistentIdentifierManager;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.urn.MCRDNBURN;
import org.mycore.pi.urn.MCRURNUtils;
import org.mycore.pi.urn.MCRUUIDURNGenerator;

import javax.persistence.EntityManager;
import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mycore.pi.MCRPIUtils.generateMCRPI;
import static org.mycore.pi.MCRPIUtils.getMCRURNClient;
import static org.mycore.pi.MCRPIUtils.randomFilename;

/**
 * Created by chi on 23.02.17.
 * @author Huu Chi Vu
 */
public class MCRURNGranularRESTRegistrationTaskTest extends MCRStoreTestCase {
    @Test
    public void run() throws Exception {
        MCRPI urn1 = generateMCRPI(randomFilename());
        MCREntityManagerProvider.getCurrentEntityManager()
                                .persist(urn1);

        Assert.assertNull("Registered date should be null.", urn1.getRegistered());

        MCRPersistentIdentifierManager.getInstance()
                                      .getUnregisteredIdenifiers(urn1.getType())
                                      .stream()
                                      .map(MCRPIRegistrationInfo::getIdentifier)
                                      .map("URN: "::concat)
                                      .forEach(System.out::println);
        //        MCRHIBConnection.instance().getSession().save(generateMCRPI(randomFilename()));
        //        MCRHIBConnection.instance().getSession().save(generateMCRPI(randomFilename()));
        //        MCRHIBConnection.instance().getSession().save(generateMCRPI(randomFilename()));

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

    private static final String countRegistered = "select count(u) from MCRPI u "
            + "where u.type = :type "
            + "and u.registered is not null";

    @Test
    public void timerTask() throws Exception {
        System.out.println("Start: " + new Date());
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        MCRPI urn1 = generateMCRPI(randomFilename());
        MCRPI urn2 = generateMCRPI(randomFilename());
        em.persist(urn1);
        em.persist(urn2);
        em.getTransaction().commit();

        getMCRURNClient().put(urn1, (res, elp) -> Optional.of(res.getStatusLine().getStatusCode()))
                         .map(String::valueOf)
                         .map("Register: "::concat)
                         .ifPresent(System.out::println);


        Number singleResult = MCREntityManagerProvider
                .getCurrentEntityManager()
                .createQuery(countRegistered, Number.class)
                .setParameter("type", urn1.getType())
                .getSingleResult();

        System.out.println("Num: " + singleResult);

        MCRURNGranularRESTRegistrationTask registrationTask = new MCRURNGranularRESTRegistrationTask(
                MCRPIUtils.getMCRURNClient());
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(registrationTask, 0, 2, TimeUnit.SECONDS);
        Thread.sleep(6000);
        //        FooTask fooTask = new FooTask();
        //        scheduler.scheduleAtFixedRate(fooTask, 0, 2, TimeUnit.SECONDS);
        //        Thread.sleep(6000);

        singleResult = MCREntityManagerProvider
                .getCurrentEntityManager()
                .createQuery(countRegistered, Number.class)
                .setParameter("type", urn1.getType())
                .getSingleResult();

        System.out.println("Num: " + singleResult);
        System.out.println("End: " + new Date());

    }

    @Test
    public void dfgViewerURN() throws Exception {
        MCRUUIDURNGenerator testGen = new MCRUUIDURNGenerator("TestGen");
        MCRDNBURN mcrdnburn = testGen.generate("frontend-", null);

        System.out.println("URN: " + mcrdnburn.asString());
        System.out.println("URN: " + mcrdnburn.getSubNamespace());
        System.out.println("URN: " + mcrdnburn.getNamespaceSpecificString());

    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());
        testProperties.put("MCR.PI.Generator.testGenerator.Namespace", "frontend-");
        return testProperties;
    }

    private static class FooTask extends TimerTask implements Closeable {
        private static Logger LOGGER = LogManager.getLogger();

        @Override
        public void run() {
            UUID uuid = UUID.randomUUID();
            LOGGER.info("Start task " + uuid);
            LOGGER.info("Session: " + MCRSessionMgr.getCurrentSession().toString());
            LOGGER.info("end run " + uuid);
        }

        @Override
        public void close() throws IOException {
            LOGGER.info("End task");
        }
    }
}