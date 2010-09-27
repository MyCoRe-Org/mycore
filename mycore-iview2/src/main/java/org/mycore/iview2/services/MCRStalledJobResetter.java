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

public class MCRStalledJobResetter implements Runnable {
    private static SessionFactory sessionFactory = MCRHIBConnection.instance().getSessionFactory();

    private static MCRStalledJobResetter instance = new MCRStalledJobResetter();

    private static int maxTimeDiff = Integer.parseInt(MCRIview2Props.getProperty("TimeTillReset"));

    private static Logger LOGGER = Logger.getLogger(MCRStalledJobResetter.class);

    private MCRStalledJobResetter() {
    }

    public static MCRStalledJobResetter getInstance() {
        return instance;
    }

    public void run() {
        boolean reset = false;
        Session session = sessionFactory.getCurrentSession();
        Transaction executorTransaction = session.beginTransaction();
        LOGGER.info("MCRTileJob is Checked for dead Entries");

        Query query = session.createQuery("FROM MCRTileJob WHERE status='" + MCRJobState.PROCESS.toChar() + "' ORDER BY id ASC");
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
