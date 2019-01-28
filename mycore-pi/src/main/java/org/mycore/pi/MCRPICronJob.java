package org.mycore.pi;

import java.text.ParseException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRStartupHandler;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;
import org.mycore.pi.urn.MCRDNBURN;
import org.mycore.pi.urn.MCRDNBURNParser;
import org.mycore.pi.urn.MCRURNUtils;
import org.mycore.util.concurrent.MCRFixedUserCallable;

/**
 * Handles various tasks that need to be executed time based:
 * <ul>
 *     <li>Check if created URNs are registered at the DNB</li>
 * </ul>
 */
public class MCRPICronJob implements Runnable, MCRStartupHandler.AutoExecutable {

    private static final int CHECK_URN_THREAD_COUNT = (int) Math.max(1, Runtime.getRuntime().availableProcessors() / 4);

    private static final int CRON_INITIAL_DELAY_MINUTES = 1;

    private static final int CRON_PERIOD_MINUTES = 15;

    private static final int CRON_THREAD_COUNT = 1;

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ExecutorService CHECK_URN_EXECUTOR_SERVICE = getCheckUrnExecutorService();

    private static final List<Future<Void>> tasks = new LinkedList<>();

    private ScheduledExecutorService cronExcutorService;

    public MCRPICronJob() {
        cronExcutorService = Executors.newScheduledThreadPool(CRON_THREAD_COUNT);
    }

    private static ExecutorService getCheckUrnExecutorService() {
        final ExecutorService executorService = Executors.newFixedThreadPool(CHECK_URN_THREAD_COUNT);
        addShutdownHandler(executorService);
        return executorService;
    }

    private static void addShutdownHandler(ExecutorService executorService) {
        MCRShutdownHandler.getInstance().addCloseable(() -> {
            executorService.shutdown();
            try {
                executorService.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOGGER.error("Interupted wait for termination.", e);
            }
        });
    }

    public void run() {
        LOGGER.info("Running " + getName() + "..");

        if (tasks.stream().allMatch(Future::isDone)) {
            tasks.clear();
            final List<MCRPI> urns = MCRPIManager.getInstance().getUnregisteredIdentifiers(MCRDNBURN.TYPE, -1);
            urns.stream().map(mcrpi ->
                new MCRFixedUserCallable<Void>(() -> {
                    LOGGER.info("check {} is registered.", mcrpi.getIdentifier());
                    MCRDNBURN dnburn = new MCRDNBURNParser()
                        .parse(mcrpi.getIdentifier())
                        .orElseThrow(
                            () -> new MCRException("Cannot parse Identifier from table: " + mcrpi.getIdentifier()));

                    try {
                        // Find register date in dnb rest
                        Date dnbRegisteredDate = MCRURNUtils.getDNBRegisterDate(dnburn);

                        if (dnbRegisteredDate == null) {
                            return null;
                        }

                        mcrpi.setRegistered(dnbRegisteredDate);
                        MCRPIServiceManager.getInstance().getRegistrationService(mcrpi.getService()).updateFlag(
                            MCRObjectID.getInstance(mcrpi.getMycoreID()), mcrpi.getAdditional(), mcrpi);
                    } catch (ParseException e) {
                        LOGGER.error(
                            "Could not parse Date from PIDEF ! URN wont be marked as registered because of this! ",
                            e);
                    } catch (MCRIdentifierUnresolvableException e) {
                        LOGGER
                            .error(
                                "Could not update Date from PIDEF ! URN wont be marked as registered because of this! ",
                                e);
                    }
                    return null;
                }, MCRSystemUserInformation.getJanitorInstance()))
                .map(CHECK_URN_EXECUTOR_SERVICE::submit)
                .forEach(tasks::add);
        }
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void startUp(ServletContext servletContext) {
        addShutdownHandler(cronExcutorService);
        cronExcutorService.scheduleWithFixedDelay(this, CRON_INITIAL_DELAY_MINUTES, CRON_PERIOD_MINUTES, TimeUnit.MINUTES);
    }
}
