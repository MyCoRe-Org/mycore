package org.mycore.util.concurrent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

/**
 * Decorates a {@link Runnable} with the mycore session and database transaction handling.
 * 
 * @author Matthias Eichner
 */
public class MCRTransactionableRunnable implements Runnable {

    protected final static Logger LOGGER = LogManager.getLogger(MCRTransactionableRunnable.class);

    private Runnable decorator;

    public MCRTransactionableRunnable(Runnable decorator) {
        this.decorator = decorator;
    }

    @Override
    public void run() {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        MCRSessionMgr.setCurrentSession(session);
        Transaction transaction = MCRHIBConnection.instance().getSession().beginTransaction();
        try {
            this.decorator.run();
        } finally {
            try {
                transaction.commit();
            } catch (Exception commitExc) {
                LOGGER.error("Error while commiting transaction.", commitExc);
                try {
                    transaction.rollback();
                } catch (Exception rollbackExc) {
                    LOGGER.error("Error while rollbacking transaction.", commitExc);
                }
            }
            try {
                MCRSessionMgr.releaseCurrentSession();
                session.close();
            } catch (Exception exc) {
                LOGGER.error("Unable to release session: " + session.getID());
            }
        }
    }

}
