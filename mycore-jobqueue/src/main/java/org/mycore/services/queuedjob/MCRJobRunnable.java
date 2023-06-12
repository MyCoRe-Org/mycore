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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.processing.MCRAbstractProcessable;
import org.mycore.common.processing.MCRProcessableStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceException;

/**
 * This class execute the specified action for {@link MCRJob} and performs {@link MCRJobAction#rollback()}
 * if an error occurs. 
 *
 * @author René Adler
 *
 */
public class MCRJobRunnable extends MCRAbstractProcessable implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(MCRJobRunnable.class);

    /**
     * The job to execute.
     */
    protected final MCRJob job;
    private final MCRJobConfig config;

    private final List<MCRJobStatusListener> listeners;

    private final MCRJobAction actionInstance;

    /**
     * Creates a new instance of {@link MCRJobRunnable}.
     * @param job the job to execute
     * @param config the job config to read the listeners from and retrieve the max retries
     * @param additionalListeners additional listeners to add to the list of listeners
     * @param actionInstance the action instance to execute
     */
    public MCRJobRunnable(MCRJob job,
        MCRJobConfig config,
        List<MCRJobStatusListener> additionalListeners,
        MCRJobAction actionInstance) {
        this.job = job;
        this.config = config;
        this.actionInstance = actionInstance;
        this.listeners
            = Stream.of(additionalListeners, config.jobStatusListeners(job.getAction())).flatMap(List::stream).toList();
        setName(this.job.getId() + " - " + this.job.getAction().getSimpleName());
        setStatus(MCRProcessableStatus.created);
        job.getParameters().forEach((k, v) -> this.getProperties().put(k, v));

    }

    public void run() {
        //prepare environment
        MCRSessionMgr.unlock();
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        mcrSession.setUserInformation(MCRSystemUserInformation.getSystemUserInstance());
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            Exception executionException = null;
            listeners.forEach(l -> l.onProcessing(job));
            //prepare job
            transaction.begin();
            MCRJob localJob;
            setStatus(MCRProcessableStatus.processing);
            job.setStart(new Date());
            localJob = em.merge(job);
            try {
                transaction.commit();
            } catch (PersistenceException e) {
                executionException = e;
                LOGGER.error("Could not start job {}", job.getId(), e);
                transaction.rollback();
            }
            if (executionException == null) {
                //execute job
                transaction.begin();
                localJob = em.merge(localJob);
                try {
                    actionInstance.execute();
                    transaction.commit();
                } catch (Exception ex) {
                    executionException = ex;
                    transaction.rollback();
                    try {
                        actionInstance.rollback();
                    } catch (RuntimeException e) {
                        executionException.addSuppressed(e);
                        LOGGER.error("Could not rollback job {}", job.getId(), e);
                    }
                }
                //handle result
                transaction.begin();
                localJob = em.merge(localJob);
                localJob.setFinished(new Date());
                if (executionException == null) {
                    handleSuccess(localJob);
                } else {
                    handleException(localJob, executionException);
                }
                try {
                    transaction.commit();
                } catch (PersistenceException e) {
                    LOGGER.error("Could not save result to job {}", job.getId(), e);
                    transaction.rollback();
                    if (executionException == null) {
                        executionException = e;
                    } else {
                        executionException.addSuppressed(e);
                    }
                }
            }
            //inform listeners
            final Exception finalException = executionException;
            listeners.forEach(l -> {
                if (finalException == null) {
                    l.onSuccess(job);
                } else {
                    l.onError(job, finalException);
                }
            });
        } finally {
            //clean up
            MCRSessionMgr.releaseCurrentSession();
            mcrSession.close();
        }
    }

    private void handleSuccess(MCRJob localJob) {
        localJob.setStatus(MCRJobStatus.FINISHED);
        setStatus(MCRProcessableStatus.successful);
    }

    private void handleException(MCRJob localJob, Exception executionException) {
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw);) {
            executionException.printStackTrace(pw);
            String exception = sw.toString();
            if (exception.length() > MCRJob.EXCEPTION_MAX_LENGTH) {
                exception = exception.substring(0, MCRJob.EXCEPTION_MAX_LENGTH);
            }
            //TODO: check if changes to entities are reflected in database at next transaction or lost
            localJob.setException(exception);
        } catch (IOException e) {
            LOGGER.error("Could not set exception for job {}", job.getId(), e);
        }

        Integer tries = localJob.getTries();
        if (tries == null) {
            tries = 0; // tries can be null, otherwise old database entries will fail or can not be migrated
        }

        localJob.setTries(++tries);

        if (tries >= config.maxTryCount(job.getAction()).orElseGet(config::maxTryCount)) {
            localJob.setStatus(MCRJobStatus.MAX_TRIES);
        } else {
            localJob.setStatus(MCRJobStatus.ERROR);
        }
        setError(executionException);
    }
}
