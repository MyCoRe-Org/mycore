package org.mycore.iview2.services;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.mycore.backend.hibernate.MCRHIBConnection;

/**
 * Resets jobs that took to long to tile.
 * Set property <code>MCR.Module-iview2.TimeTillReset</code> to alter grace period.
 * Set property <code>MCR.Module-iview2.MaxResetCount</code> to alter maximum tries per job.
 * @author Thomas Scheffler (yagee)
 */
public class MCRStalledJobResetter implements Runnable {
    private static SessionFactory sessionFactory = MCRHIBConnection.instance().getSessionFactory();

    private static MCRStalledJobResetter instance = new MCRStalledJobResetter();

    private static int maxTimeDiff = Integer.parseInt(MCRIView2Tools.getIView2Property("TimeTillReset"));

    private static Logger LOGGER = Logger.getLogger(MCRStalledJobResetter.class);

    private static int maxResetCount = Integer.parseInt(MCRIView2Tools.getIView2Property("MaxResetCount"));

    private HashMap<Long, Integer> jobCounter;

    private MCRStalledJobResetter() {
        jobCounter = new HashMap<>();
    }

    public static MCRStalledJobResetter getInstance() {
        return instance;
    }

    /**
     * Resets jobs to {@link MCRJobState#NEW} that where in status {@link MCRJobState#PROCESSING} for to long time.
     */
    public void run() {
        boolean reset = false;
        Session session = sessionFactory.getCurrentSession();
        Transaction executorTransaction = session.beginTransaction();
        LOGGER.info("MCRTileJob is Checked for dead Entries");

        Query query = session.createQuery("FROM MCRTileJob WHERE status='" + MCRJobState.PROCESSING.toChar()
            + "' ORDER BY id ASC");
        long start = 0;
        long current = new Date(System.currentTimeMillis()).getTime() / 60000;

        @SuppressWarnings("unchecked")
        Iterator<MCRTileJob> result = query.iterate();
        while (result.hasNext()) {
            MCRTileJob job = result.next();
            start = job.getStart().getTime() / 60000;
            LOGGER.debug("checking " + job.getDerivate() + " " + job.getPath() + " ...");
            if (current - start >= maxTimeDiff) {
                if (hasPermanentError(job)) {
                    LOGGER.warn("Job has permanent errors: " + job);
                    job.setStatus(MCRJobState.ERROR);
                    jobCounter.remove(job.getId());
                } else {
                    LOGGER.debug("->Resetting too long in queue");
                    job.setStatus(MCRJobState.NEW);
                    job.setStart(null);
                    reset = true;
                }
                session.update(job);
            } else {
                LOGGER.debug("->ok");
            }
        }
        try {
            executorTransaction.commit();
        } catch (HibernateException e) {
            e.printStackTrace();
            if (executorTransaction != null) {
                executorTransaction.rollback();
                reset = false;//No changes are applied, so no notification is needed as well
            }
        }
        //Only notify Listeners on TilingQueue if really something is set back
        if (reset) {
            synchronized (MCRTilingQueue.getInstance()) {
                MCRTilingQueue.getInstance().notifyListener();
            }
        }
        session.close();
        LOGGER.info("MCRTileJob checking is done");
    }

    private boolean hasPermanentError(MCRTileJob job) {
        int runs = 0;
        if (jobCounter.containsKey(job.getId())) {
            runs = jobCounter.get(job.getId());
        }
        if (++runs >= maxResetCount) {
            return true;
        }
        jobCounter.put(job.getId(), runs);
        return false;
    }
}
