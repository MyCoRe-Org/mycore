package org.mycore.util.concurrent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

/**
 * Decorates a {@link Runnable} with a mycore session and a database transaction.
 * 
 * @author Matthias Eichner
 */
public class MCRTransactionableRunnable implements Runnable {

    protected final static Logger LOGGER = LogManager.getLogger(MCRTransactionableRunnable.class);

    private Runnable decorator;

    private MCRSession session;

    public MCRTransactionableRunnable(Runnable decorator) {
        this(decorator, null);
    }

    /**
     * Creates a new {@link MCRTransactionableRunnable} using the given {@link MCRSession}.
     * 
     * @param decorator the runnable to run
     * @param session the session to use
     */
    public MCRTransactionableRunnable(Runnable decorator, MCRSession session) {
        this.decorator = decorator;
        this.session = session;
    }

    @Override
    public void run() {
        if(this.session == null) {
            this.session = MCRSessionMgr.getCurrentSession();
        }
        MCRSessionMgr.setCurrentSession(this.session);
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
