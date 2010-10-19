package org.mycore.iview2.services;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.Query;

import java.util.Iterator;
import java.util.Date;
import org.hibernate.HibernateException;
import org.mycore.backend.hibernate.MCRHIBConnection;

/**
 * Resets jobs that took to long to tile.
 * Set property <code>MCR.Module-iview2.TimeTillReset</code> to alter grace period.
 * @author Thomas Scheffler (yagee)
 */
public class MCRStalledJobResetter implements Runnable {
    private static SessionFactory sessionFactory = MCRHIBConnection.instance().getSessionFactory();

    private static MCRStalledJobResetter instance = new MCRStalledJobResetter();

    private static int maxTimeDiff = Integer.parseInt(MCRIView2Tools.getIView2Property("TimeTillReset"));

    private static Logger LOGGER = Logger.getLogger(MCRStalledJobResetter.class);

    private MCRStalledJobResetter() {
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

        Query query = session.createQuery("FROM MCRTileJob WHERE status='" + MCRJobState.PROCESSING.toChar() + "' ORDER BY id ASC");
        long start = 0;
        long current = new Date(System.currentTimeMillis()).getTime() / 60000;

        @SuppressWarnings("unchecked")
        Iterator<MCRTileJob> result = query.iterate();
        while (result.hasNext()) {
            MCRTileJob job = result.next();
            start = job.getStart().getTime() / 60000;
            LOGGER.debug("checking " + job.getDerivate() + " " + job.getPath() + " ...");
            if (current - start >= maxTimeDiff) {
                LOGGER.debug("->Resetting too long in queue");

                job.setStatus(MCRJobState.NEW);
                job.setStart(null);
                session.update(job);
                reset = true;
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
}
