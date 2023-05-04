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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.processing.MCRAbstractProcessable;
import org.mycore.common.processing.MCRProcessableStatus;

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
        MCRSessionMgr.unlock();
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        mcrSession.setUserInformation(MCRSystemUserInformation.getSystemUserInstance());
        EntityManager em = MCREntityManagerProvider.getEntityManagerFactory().createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();

            try {
                setStatus(MCRProcessableStatus.processing);
                job.setStart(new Date());
                listeners.forEach(l -> l.onProcessing(job));

                actionInstance.execute();

                job.setFinished(new Date());
                job.setStatus(MCRJobStatus.FINISHED);
                setStatus(MCRProcessableStatus.successful);
                listeners.forEach(l -> l.onSuccess(job));
            } catch (Exception ex) {
                LOGGER.error("Exception occured while try to start job. Perform rollback.", ex);
                setError(ex);
                actionInstance.rollback();
                Integer tries = job.getTries();
                if (tries == null) {
                    tries = 0; // tries can be null, otherwise old database entries will fail or can not be migrated
                }

                job.setTries(++tries);

                try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw);) {
                    ex.printStackTrace(pw);
                    String exception = sw.toString();
                    if (exception.length() > MCRJob.EXCEPTION_MAX_LENGTH) {
                        exception = exception.substring(0, MCRJob.EXCEPTION_MAX_LENGTH);
                    }
                    job.setException(exception);
                } catch (Exception e) {
                    LOGGER.error("Could not set exception for job {}", job.getId(), e);
                }

                if (tries >= config.maxTryCount(job.getAction()).orElseGet(config::maxTryCount)) {
                    job.setStatus(MCRJobStatus.MAX_TRIES);
                } else {
                    job.setStatus(MCRJobStatus.ERROR);
                }
                listeners.forEach(l -> l.onError(job));
            }
            em.merge(job);
            transaction.commit();
        } catch (Exception e) {
            LOGGER.error("Error while getting next job.", e);
            if (transaction != null) {
                transaction.rollback();
            }
        } finally {
            em.close();
            MCRSessionMgr.releaseCurrentSession();
            mcrSession.close();
        }
    }

}
