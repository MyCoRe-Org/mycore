package org.mycore.services.iview2;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.Query;

import java.util.Iterator;
import java.util.Date;
import org.hibernate.HibernateException;
import org.mycore.backend.hibernate.MCRHIBConnection;

public class MCRQueueExecutor implements Runnable {
	private static SessionFactory sessionFactory=MCRHIBConnection.instance().getSessionFactory();
	private static MCRQueueExecutor instance = new MCRQueueExecutor();
	private static int maxTimeDiff = Integer.parseInt(MCRIview2Props.getProperty("TimeTillReset"));
	private static Logger LOGGER = Logger.getLogger(MCRQueueExecutor.class);

	private MCRQueueExecutor() {}
	
	public static MCRQueueExecutor getInstance() {
		return instance;
	}

    public void run() {
		boolean reset = false;
		Session executorSession = sessionFactory.getCurrentSession();
		LOGGER.info("MCRTileJob is Checked for dead Entries");
		
		Query query = executorSession.createQuery("FROM MCRTileJob WHERE status='"+MCRJobState.PROCESS.toChar()+"' ORDER BY id ASC");
		long start = 0;
		long current = new Date(System.currentTimeMillis()).getTime() / 60000;

		@SuppressWarnings("unchecked")
		Iterator<MCRTileJob> result = query.iterate();
		Transaction executorTransaction = executorSession.beginTransaction();
		while (result.hasNext()) {
		    MCRTileJob job = result.next();
			start = job.getStart().getTime() / 60000;
			LOGGER.debug("checking " + job.getDerivate() + " " + job.getPath() + " ...");
			if (current - start >= maxTimeDiff) {
				LOGGER.debug("->Resetting too long in queue");
				
				job.setStatus(MCRJobState.NEW);
				job.setStart(null);
				reset = true;
//				try {
//					executorTransaction.commit();
//				} catch (HibernateException e) {
//					e.printStackTrace();
//					if (executorTransaction != null) {
//						executorTransaction.rollback();
//					}
//				}
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
		executorSession.close();
		LOGGER.info("MCRTileJob checking is done");
	}
}
