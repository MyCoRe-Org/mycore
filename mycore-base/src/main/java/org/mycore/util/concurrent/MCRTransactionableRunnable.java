package org.mycore.util.concurrent;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

/**
 * Encapsulates a {@link Runnable} with a mycore session and a database transaction.
 * 
 * @author Matthias Eichner
 */
public class MCRTransactionableRunnable implements Runnable, MCRDecorator<Runnable> {

    private final static Logger LOGGER = LogManager.getLogger();

    protected Runnable runnable;

    private MCRSession session;

    /**
     * Creates a new {@link Runnable} encapsulating the {@link #run()} method with a new
     * {@link MCRSession} and a database transaction. Afterwards the transaction will
     * be committed and the session will be released and closed.
     * 
     * <p>If you want to execute your runnable in the context of an already existing
     * session use the {@link MCRTransactionableRunnable#MCRTransactionableRunnable(Runnable, MCRSession)}
     * constructor instead.
     * 
     * @param runnable the runnable to execute within a session and transaction
     */
    public MCRTransactionableRunnable(Runnable runnable) {
        this.runnable = Objects.requireNonNull(runnable, "runnable must not be null");
    }

    /**
     * Creates a new {@link Runnable} encapsulating the {@link #run()} method with a new
     * a database transaction. The transaction will be created in the context of the
     * given session. Afterwards the transaction will be committed and the session
     * will be released (but not closed!).
     * 
     * @param runnable the runnable to execute within a session and transaction
     * @param session the session to use
     */
    public MCRTransactionableRunnable(Runnable runnable, MCRSession session) {
        this.runnable = Objects.requireNonNull(runnable, "runnable must not be null");
        this.session = Objects.requireNonNull(session, "session must not be null");
    }

    @Override
    public void run() {
        boolean newSession = this.session == null;
        boolean closeSession = newSession && !MCRSessionMgr.hasCurrentSession();
        if (newSession) {
            this.session = MCRSessionMgr.getCurrentSession();
        }
        MCRSessionMgr.setCurrentSession(this.session);
        session.beginTransaction();
        try {
            this.runnable.run();
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
                if (closeSession && session != null) {
                    session.close();
                }
            }
        }
    }

    @Override
    public Runnable get() {
        return this.runnable;
    }

}
