package org.mycore.util.concurrent;

import java.util.concurrent.Callable;

import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUserInformation;

/**
 * Encapsulates a {@link Callable} with a mycore <b>SYSTEM</b> session and a database transaction.
 * 
 * @author Matthias Eichner
 */
public class MCRSystemCallable<V> extends MCRTransactionableCallable<V> {

    /**
     * Creates a new {@link Callable} encapsulating the {@link #call()} method with a new
     * <b>SYSTEM</b> {@link MCRSession} and a database transaction. Afterwards the transaction will
     * be committed and the session will be released and closed.
     * 
     * @param callable the callable to execute within a <b>SYSTEM</b> session and transaction
     */
    public MCRSystemCallable(Callable<V> callable) {
        super(callable);
    }

    @Override
    public V call() throws Exception {
        boolean hasSession = MCRSessionMgr.hasCurrentSession();
        this.session = MCRSessionMgr.getCurrentSession();
        MCRUserInformation currentUser = this.session.getUserInformation();
        MCRSystemUserInformation systemUser = MCRSystemUserInformation.getSystemUserInstance();
        if (hasSession) {
            if (!currentUser.equals(systemUser)) {
                throw new MCRException(
                    "MCRSystemCallable is bound to " + currentUser.getUserID() + " and not to SYSTEM.");
            }
        } else {
            this.session.setUserInformation(systemUser);
        }
        try {
            return super.call();
        } finally {
            if (!hasSession && this.session != null) {
                this.session.close();
            }
        }
    }

}
