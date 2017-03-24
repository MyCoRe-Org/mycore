package org.mycore.pi.urn.rest;

import java.util.Optional;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.servlet.ServletContext;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.BiConsumer;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRStartupHandler;
import org.mycore.pi.MCRPIRegistrationInfo;

/**
 * @author shermann
 *
 */
public class MCRURNGranularRESTRegistrationStarter
        implements MCRStartupHandler.AutoExecutable, MCRShutdownHandler.Closeable {

    private static final Logger LOGGER = LogManager.getLogger();

    private final long period;

    private final TimeUnit timeUnit;

    private ScheduledExecutorService scheduler;

    public MCRURNGranularRESTRegistrationStarter() {
        this(1, TimeUnit.MINUTES);
    }

    public MCRURNGranularRESTRegistrationStarter(long period, TimeUnit timeUnit) {
        this.period = period;
        this.timeUnit = timeUnit;
    }

    @Override
    public String getName() {
        return "URN Registration Service";
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void startUp(ServletContext servletContext) {
        MCRShutdownHandler.getInstance().addCloseable(this);

        UsernamePasswordCredentials usernamePassword = getUsernamePassword()
                .orElseThrow(() -> new RuntimeException("could not get credentials"));
        Function<MCRPIRegistrationInfo, MCREpicurLite> epicureProvider = getEpicureProvider(usernamePassword);

        MCRDNBURNClient mcrdnburnClient = new MCRDNBURNClient(epicureProvider);
        MCRURNGranularRESTRegistrationTask registrationTask = new MCRURNGranularRESTRegistrationTask(mcrdnburnClient);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(registrationTask, 0, 2, TimeUnit.SECONDS);

        //        getUsernamePassword()
        //                .map(this::getEpicureProvider)
        //                .map(MCRDNBURNClient::new)
        //                .map(MCRURNGranularRESTRegistrationTask::new)
        //                .map(this::startTimerTask)
        //                .orElseGet(this::couldNotStartTask)
        //                .accept(LOGGER);

        //        URNRegistrationService urnRegistrationService = null;
        //        DFGURNRegistrationService dfgURNRegistrationService = null;
        //        boolean supportDfgViewerURN = MCRConfiguration.instance().getBoolean("MCR.URN.Display.DFG.Viewer.URN",
        //                                                                             false);
        //        try {
        //            urnRegistrationService = new URNRegistrationService();
        //            if (supportDfgViewerURN) {
        //                dfgURNRegistrationService = new DFGURNRegistrationService();
        //            }
        //
        //        } catch (Exception e) {
        //            LOGGER.error("Could not instantiate " + URNRegistrationService.class.getName(), e);
        //            return;
        //
        //        }
        //        LOGGER.info("Starting executor service...");
        //        scheduler = Executors.newSingleThreadScheduledExecutor();
        //
        //        LOGGER.info("Starting " + URNRegistrationService.class.getSimpleName());
        //        // refresh every 60 seconds
        //        scheduler.scheduleAtFixedRate(urnRegistrationService, 0, 1, TimeUnit.MINUTES);
        //
        //        if (dfgURNRegistrationService != null) {
        //            LOGGER.info("Starting " + DFGURNRegistrationService.class.getSimpleName());
        //            // refresh every 60 seconds
        //            scheduler.scheduleAtFixedRate(dfgURNRegistrationService, 0, 1, TimeUnit.MINUTES);
        //        }
    }

    private Consumer<Logger> couldNotStartTask() {
        return logger -> logger
                .warn("Could not start Task " + MCRURNGranularRESTRegistrationTask.class.getSimpleName());
    }

    private ScheduledExecutorService getScheduler() {
        if (scheduler == null) {
            LOGGER.info("Starting executor service...");
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }

        return scheduler;
    }

    private Consumer<Logger> startTimerTask(TimerTask task) {
        getScheduler().scheduleAtFixedRate(task, 0, period, timeUnit);
        return logger -> logger
                .info("Started task " + task.getClass().getSimpleName() + ", refresh every " + period + timeUnit
                        .toString());
    }

    public Function<MCRPIRegistrationInfo, MCREpicurLite> getEpicureProvider(UsernamePasswordCredentials credentials) {
        return urn -> MCREpicurLite.instance(urn, MCRDerivateURNUtils.getURL(urn))
                                   .setCredentials(credentials);
    }

    public Optional<UsernamePasswordCredentials> getUsernamePassword() {
        String username = MCRConfiguration.instance().getString("MCR.URN.DNB.Credentials.Login", null);
        String password = MCRConfiguration.instance().getString("MCR.URN.DNB.Credentials.Password", null);

        if (username == null || password == null || username.length() == 0 || password.length() == 0) {
            LOGGER.warn("Could not instantiate " + this.getClass().getName()
                                + " as required credentials are unset");
            LOGGER.warn("Please set MCR.URN.DNB.Credentials.Login and MCR.URN.DNB.Credentials.Password");
            return Optional.empty();
        }

        return Optional.of(new UsernamePasswordCredentials(username, password));
    }

    @Override
    public void prepareClose() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    @Override
    public void close() {
        if (scheduler != null && !scheduler.isShutdown()) {
            try {
                scheduler.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            scheduler.shutdownNow();
        }
    }
}
