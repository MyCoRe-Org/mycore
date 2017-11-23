package org.mycore.services.queuedjob;

import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.processing.MCRAbstractProcessable;
import org.mycore.common.processing.MCRProcessableStatus;

/**
 * A slave thread of {@link MCRJobMaster}.
 * 
 * This class execute the specified action for {@link MCRJob} and performs {@link MCRJobAction#rollback()} 
 * if an error occurs. 
 *
 * @author Ren\u00E9 Adler
 *
 */
public class MCRJobThread extends MCRAbstractProcessable implements Runnable {

    private static Logger LOGGER = LogManager.getLogger(MCRJobThread.class);

    protected final MCRJobQueue queue;

    protected MCRJob job = null;

    public MCRJobThread(MCRJob job) {
        this.job = job;
        setName(this.job.getId() + " - " + this.job.getAction().getSimpleName());
        setStatus(MCRProcessableStatus.created);
        this.queue = MCRJobQueue.getInstance(job.getAction());
    }

    public void run() {
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        mcrSession.setUserInformation(MCRSystemUserInformation.getSystemUserInstance());
        EntityManager em = MCREntityManagerProvider.getEntityManagerFactory().createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            Class<? extends MCRJobAction> actionClass = job.getAction();
            Constructor<? extends MCRJobAction> actionConstructor = actionClass.getConstructor(MCRJob.class);
            MCRJobAction action = actionConstructor.newInstance(job);

            transaction.begin();

            try {
                setStatus(MCRProcessableStatus.processing);
                job.setStart(new Date());

                action.execute();

                job.setFinished(new Date());
                job.setStatus(MCRJobStatus.FINISHED);
                setStatus(MCRProcessableStatus.successful);
            } catch (ExecutionException ex) {
                LOGGER.error("Exception occured while try to start job. Perform rollback.", ex);
                setError(ex);
                action.rollback();
            } catch (Exception ex) {
                LOGGER.error("Exception occured while try to start job.", ex);
                setError(ex);
            }
            em.merge(job);
            transaction.commit();

            // notify the queue we have processed the job
            synchronized (queue) {
                queue.notifyAll();
            }
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
