package org.mycore.util.concurrent;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

/**
 * Decorates a {@link Runnable} with a mycore session and a database transaction.
 * 
 * @author Matthias Eichner
 */
public class MCRTransactionableRunnable implements Runnable {

    protected final static Logger LOGGER = LogManager.getLogger();

    private Runnable decorator;

    private MCRSession session;

    /**
     * Creates a new {@link MCRTransactionableRunnable} using the given {@link MCRSession}.
     * Decorator and session must not be null.
     * 
     * @param decorator the runnable to run
     * @param session the session to use
     */
    public MCRTransactionableRunnable(Runnable decorator, MCRSession session) {
        this.decorator = Objects.requireNonNull(decorator, "decorator must not be null");
        this.session = Objects.requireNonNull(session, "session must not be null");
    }

    @Override
    public void run() {
        MCRSessionMgr.setCurrentSession(this.session);
        session.beginTransaction();
        try {
            this.decorator.run();
        } finally {
            try {
                session.commitTransaction();
            } catch (Exception commitExc) {
                LOGGER.error("Error while commiting transaction.", commitExc);
                try {
                    session.rollbackTransaction();
                } catch (Exception rollbackExc) {
                    LOGGER.error("Error while rollbacking transaction.", commitExc);
                }
            } finally {
                MCRSessionMgr.releaseCurrentSession();
            }
        }
    }

}
