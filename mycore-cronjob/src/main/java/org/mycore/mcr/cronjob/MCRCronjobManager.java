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

package org.mycore.mcr.cronjob;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.processing.MCRProcessableDefaultCollection;
import org.mycore.common.processing.MCRProcessableRegistry;

/**
 * Schedules all Cronjobs defined with the property prefix {@link #JOBS_CONFIG_PREFIX}. Couples the execution with the
 * {@link MCRProcessableRegistry}.
 */
public class MCRCronjobManager implements MCRShutdownHandler.Closeable {

    public static final String JOBS_CONFIG_PREFIX = "MCR.Cronjob.Jobs.";

    public static final Logger LOGGER = LogManager.getLogger();

    private final MCRProcessableDefaultCollection processableCollection;

    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
        MCRConfiguration2.getOrThrow("MCR.Cronjob.CorePoolSize", Integer::valueOf));

    private MCRCronjobManager() {
        processableCollection = new MCRProcessableDefaultCollection(getClass().getSimpleName());
        MCRProcessableRegistry.getSingleInstance().register(processableCollection);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    }

    public static MCRCronjobManager getInstance() {
        return MCRCronjobManagerInstanceHelper.INSTANCE;
    }

    @Override
    public void prepareClose() {
        LOGGER.info("Shutdown {}", this.getClass().getSimpleName());
        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.warn("Got interrupted while awaiting termination for MCRCronjobThreadPool");
        }
    }

    @Override
    public void close() {
        if(!executor.isTerminated()) {
            LOGGER.info("Force shutdown {}", this.getClass().getSimpleName());
            executor.shutdownNow();
            try {
                executor.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOGGER.warn("Got interrupted while awaiting force termination for MCRCronjobThreadPool");
            }
        }
    }

    void startUp() {
        MCRShutdownHandler.getInstance().addCloseable(this);
        LOGGER.info("Cron schedule:");
        List<String> properties = MCRConfiguration2.getInstantiatablePropertyKeys(JOBS_CONFIG_PREFIX)
                .collect(Collectors.toList());
        for (String id : properties) {
            MCRCronjob job = (MCRCronjob) MCRConfiguration2.getInstanceOf(id).orElseThrow();
            LOGGER.info(job.getDescription() + " " + job.getCronDescription());
            scheduleNext(job.getID());
        }
    }

    private void scheduleNext(String jobID) {
        MCRCronjob job = getJob(jobID);
        this.processableCollection.add(job.getProcessable());
        job.getNextExecution().ifPresent(next -> {
            if (next > 0) {
                executor.schedule(() -> {
                    try {
                        LOGGER.info("Execute job " + job.getID() + " - " + job.getDescription());
                        job.run();
                        this.processableCollection.remove(job.getProcessable());
                        scheduleNext(job.getID());
                    } catch (Exception ex) {
                        LOGGER.error("Error while executing job " + job.getID() + " " + job.getDescription(), ex);
                        this.processableCollection.remove(job.getProcessable());
                    }
                }, next, TimeUnit.MILLISECONDS);
            }
        });
    }

    public <T extends MCRCronjob> T getJob(String id) {
        return (T) MCRConfiguration2.getInstanceOf(JOBS_CONFIG_PREFIX + id).orElseThrow();
    }

    private static class MCRCronjobManagerInstanceHelper {
        public static MCRCronjobManager INSTANCE = new MCRCronjobManager();
    }
}
