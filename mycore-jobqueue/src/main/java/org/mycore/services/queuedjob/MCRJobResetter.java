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

package org.mycore.services.queuedjob;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.mcr.cronjob.MCRCronjob;

import org.mycore.util.concurrent.MCRTransactionableRunnable;

/**
 * Resets jobs that are in {@link MCRJobStatus#ERROR} for minimum the time defined in the job config.
 *
 * @author René Adler
 */
public class MCRJobResetter extends MCRCronjob {
    private static Logger LOGGER = LogManager.getLogger(MCRJobResetter.class);

    private final MCRJobDAO dao;

    private final MCRJobConfig config;
    private Function<Class, Queue<MCRJob>> queueResolver;

    /**
     * Creates a new instance of {@link MCRJobResetter}. Uses {@link MCRJobQueueManager} to resolve dependencies. 
     * Used by the Cronjob system.
     */
    public MCRJobResetter() {
        this(MCRJobQueueManager.getInstance().getJobDAO(), MCRJobQueueManager.getInstance()::getJobQueue,
            MCRJobQueueManager.getInstance().getJobConfig());
    }

    /**
     * Creates a new instance of {@link MCRJobResetter}.
     * @param dao the job dao to receive jobs from
     * @param queueResolver the queue resolver to resolve the queue for a job action, which will be used add the
     *                      resetted jobs to.
     * @param config the job config to determine the time till reset
     */
    MCRJobResetter(MCRJobDAO dao, Function<Class, Queue<MCRJob>> queueResolver, MCRJobConfig config) {
        this.dao = dao;
        this.queueResolver = queueResolver;
        this.config = config;
    }

    /**
     * Resets jobs to {@link MCRJobStatus#NEW} that are in {@link MCRJobStatus#ERROR} for minimum the time defined
     * @param action the action to reset jobs for
     * @param config the job config
     * @param dao the job dao to use
     * @param queue the queue to add the resetted jobs to
     * @param status the status of the jobs to reset
     * @return the number of resetted jobs
     */
    protected static long resetJobsWithAction(Class<? extends MCRJobAction> action, MCRJobConfig config, MCRJobDAO dao,
        Queue<MCRJob> queue, MCRJobStatus status) {
        int maxTimeDiff = config.timeTillReset(action).orElseGet(config::timeTillReset);
        List<MCRJob> jobs = dao.getJobs(action, Collections.emptyMap(), List.of(status), null, null);
        long current = new Date(System.currentTimeMillis()).getTime() / 60000;

        return jobs.stream().filter(job -> {
            long start = job.getStart().getTime() / 60000;
            boolean resetJob = current - start >= maxTimeDiff;
            return resetJob;
        }).peek(queue::offer)
            .count();
    }

    /**
     * Resets jobs to {@link MCRJobStatus#NEW} that are in {@link MCRJobStatus#ERROR} for minimum the time defined
     * @param action the action to reset jobs for
     */
    protected void resetJobsWithAction(Class<? extends MCRJobAction> action) {
        resetJobsWithAction(action, config, dao, queueResolver.apply(action), MCRJobStatus.ERROR);
    }

    /**
     * Resets jobs to {@link MCRJobStatus#NEW} that are in {@link MCRJobStatus#ERROR} for minimum the time defined
     * in {@link MCRJobConfig#timeTillReset()} or {@link MCRJobConfig#timeTillReset(Class)}.
     */
    @Override
    public void runJob() {
        try {
            new MCRTransactionableRunnable(() -> {
                dao.getActions().forEach(this::resetJobsWithAction);
                LOGGER.info("MCRJob checking is done");
            }).run();
        } catch (Exception e) {
            throw new MCRException("Error while resetting jobs!", e);
        }
    }

    @Override
    public String getDescription() {
        return "Resets jobs that took to long to perform action or produced errors.";
    }
}
